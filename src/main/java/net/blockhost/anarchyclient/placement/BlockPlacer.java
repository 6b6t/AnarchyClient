package net.blockhost.anarchyclient.placement;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

public final class BlockPlacer {

    private static final Set<String> USE_METHODS = Set.of("useWithoutItem", "useItemOn");
    // Reflection here inspects which methods a block class declares; it does not reach private
    // members, so an access widener or a mixin cannot replace it.
    private static final ClassValue<Boolean> OVERRIDES_USE = new ClassValue<>() {
        @Override
        protected Boolean computeValue(final Class<?> type) {
            for (Class<?> current = type; current != null && current != BlockBehaviour.class; current = current.getSuperclass()) {
                for (Method method : current.getDeclaredMethods()) {
                    if (USE_METHODS.contains(method.getName())) {
                        return true;
                    }
                }
            }
            return false;
        }
    };

    private BlockPlacer() {
    }

    public static PlacementResult place(final Minecraft client, final String owner, final BlockPos target,
                                        final boolean rotate, final float maxTurnDegrees) {
        return place(client, owner, target, rotate, maxTurnDegrees, stack -> true);
    }

    public static PlacementResult place(final Minecraft client, final String owner, final BlockPos target,
                                        final boolean rotate, final float maxTurnDegrees,
                                        final Predicate<ItemStack> itemPredicate) {
        return place(client, owner, target, rotate, maxTurnDegrees, itemPredicate, PlacementOptions.DEFAULT);
    }

    /**
     * Places one block at {@code target}.
     *
     * <p>With {@code rotate}, the placement waits (returns {@link PlacementResult#WAITING}) until the
     * rotation toward the clicked face has been sent to the server, so the click never arrives
     * before the rotation. Without {@code rotate}, it places immediately.
     */
    public static PlacementResult place(final Minecraft client, final String owner, final BlockPos target,
                                        final boolean rotate, final float maxTurnDegrees,
                                        final Predicate<ItemStack> itemPredicate, final PlacementOptions options) {
        return switch (attempt(client, owner, target, rotate, maxTurnDegrees, itemPredicate, options)) {
            case FILLED -> PlacementResult.FILLED;
            case PLACED -> PlacementResult.PLACED;
            case WAITING, ROTATING -> PlacementResult.WAITING;
            case MISSING_ITEM -> PlacementResult.MISSING_ITEM;
        };
    }

    /**
     * Places up to {@code maxPlacements} requests in order. With {@code rotate}, the batch stops at
     * the first request that still needs to turn, so later requests cannot steal its rotation.
     */
    public static int placeBatch(final Minecraft client, final String owner, final Iterable<PlacementRequest> requests,
                                 final int maxPlacements, final boolean rotate, final float maxTurnDegrees) {
        int limit = placementsPerTick(maxPlacements);
        int placed = 0;
        for (PlacementRequest request : requests) {
            Attempt result = attempt(
                    client,
                    owner,
                    request.target(),
                    rotate,
                    maxTurnDegrees,
                    request.itemPredicate(),
                    request.options()
            );
            if (result == Attempt.ROTATING) {
                return placed;
            }
            if (result == Attempt.PLACED && ++placed >= limit) {
                return placed;
            }
        }
        return placed;
    }

    private static Attempt attempt(final Minecraft client, final String owner, final BlockPos target,
                                   final boolean rotate, final float maxTurnDegrees,
                                   final Predicate<ItemStack> itemPredicate, final PlacementOptions options) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return Attempt.WAITING;
        }
        if (!needsPlacement(client.level, target)) {
            return Attempt.FILLED;
        }
        Optional<PlacementTarget> placementTarget = findPlacementTarget(client.level, target);
        if (placementTarget.isEmpty()) {
            return Attempt.WAITING;
        }
        OptionalInt hotbarSlot = findPlaceableHotbarSlot(player.getInventory(), client.level, player, target, itemPredicate, options);
        if (hotbarSlot.isEmpty()) {
            return Attempt.MISSING_ITEM;
        }
        if (!SilentHotbar.select(player, owner, hotbarSlot.orElseThrow(), SilentHotbar.PRIORITY_COMBAT, 3, true)) {
            return Attempt.WAITING;
        }
        PlacementTarget placement = placementTarget.orElseThrow();
        if (rotate) {
            Rotation rotation = Rotation.lookingAt(placement.hitLocation(), player.getEyePosition());
            RotationManager.request(new RotationRequest(
                    owner,
                    rotation,
                    70,
                    maxTurnDegrees,
                    2,
                    2.0F,
                    RotationTurnMode.STEPPED,
                    true
            ));
            RotationManager.apply(player);
            if (!RotationManager.serverLookHits(player, new AABB(placement.hitResult().getBlockPos()),
                    player.blockInteractionRange() + 1.0)) {
                return Attempt.ROTATING;
            }
        }
        InteractionResult result = useItemOn(client, player, placement.hitResult(), placement.sneak());
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            return Attempt.PLACED;
        }
        return Attempt.WAITING;
    }

    /**
     * Clicks the block with the main hand. With {@code sneak}, the server and the client prediction
     * both see the player sneaking during the click, so the clicked block's own interaction (a
     * door, a chest, a respawn anchor) is skipped like a vanilla shift-click.
     */
    public static InteractionResult useItemOn(final Minecraft client, final LocalPlayer player,
                                              final BlockHitResult hitResult, final boolean sneak) {
        Input previous = player.input.keyPresses;
        if (!sneak || previous.shift() || client.getConnection() == null) {
            return client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
        }
        Input sneaking = new Input(previous.forward(), previous.backward(), previous.left(), previous.right(),
                previous.jump(), true, previous.sprint());
        client.getConnection().send(new ServerboundPlayerInputPacket(sneaking));
        player.input.keyPresses = sneaking;
        try {
            return client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
        } finally {
            player.input.keyPresses = previous;
            client.getConnection().send(new ServerboundPlayerInputPacket(player.getLastSentInput()));
        }
    }

    public static int placementsPerTick(final int value) {
        return Math.max(1, value);
    }

    /**
     * Whether a block can go at {@code target}. Water and lava are already replaceable; a waterlogged slab, stair or
     * fence is not, and treating its fluid as free space made placement retry it forever.
     */
    public static boolean needsPlacement(final ClientLevel level, final BlockPos target) {
        return level.getBlockState(target).canBeReplaced();
    }

    public static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                                      final LocalPlayer player, final BlockPos target) {
        return findPlaceableHotbarSlot(inventory, level, player, target, stack -> true);
    }

    public static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                                      final LocalPlayer player, final BlockPos target,
                                                      final Predicate<ItemStack> itemPredicate) {
        return findPlaceableHotbarSlot(inventory, level, player, target, itemPredicate, PlacementOptions.DEFAULT);
    }

    public static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                                      final LocalPlayer player, final BlockPos target,
                                                      final Predicate<ItemStack> itemPredicate,
                                                      final PlacementOptions options) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (itemPredicate.test(stack) && isPlaceableBlock(stack, level, player, target, options)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    public static boolean isPlaceableBlock(final ItemStack stack, final ClientLevel level, final LocalPlayer player,
                                           final BlockPos target) {
        return isPlaceableBlock(stack, level, player, target, PlacementOptions.DEFAULT);
    }

    public static boolean isPlaceableBlock(final ItemStack stack, final ClientLevel level, final LocalPlayer player,
                                           final BlockPos target, final PlacementOptions options) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        if (!needsPlacement(level, target)
                || options.requireFullBlock() && !Block.isShapeFullBlock(state.getCollisionShape(level, target))
                || !options.allowFallingBlocks() && block instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(target.below()))) {
            return false;
        }
        return level.isUnobstructed(state, target, CollisionContext.placementContext(player));
    }

    /**
     * Finds a sturdy neighbour face to click. Neighbours without their own use interaction are
     * preferred; if only interactive neighbours remain, the target asks for a sneaking click.
     */
    public static Optional<PlacementTarget> findPlacementTarget(final ClientLevel level, final BlockPos target) {
        PlacementTarget interactive = null;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.relative(direction);
            Direction face = direction.getOpposite();
            BlockState state = level.getBlockState(neighbor);
            if (!state.isFaceSturdy(level, neighbor, face)) {
                continue;
            }
            Vec3 hitLocation = Vec3.atCenterOf(neighbor).relative(face, 0.5);
            BlockHitResult hitResult = new BlockHitResult(hitLocation, face, neighbor, false);
            if (!hasUseInteraction(state)) {
                return Optional.of(new PlacementTarget(hitResult, hitLocation, false));
            }
            if (interactive == null) {
                interactive = new PlacementTarget(hitResult, hitLocation, true);
            }
        }
        return Optional.ofNullable(interactive);
    }

    /**
     * Returns whether right-clicking this block can do something other than place against it:
     * blocks with block entities and blocks whose class overrides a use method.
     */
    public static boolean hasUseInteraction(final BlockState state) {
        return state.hasBlockEntity() || OVERRIDES_USE.get(state.getBlock().getClass());
    }

    public enum PlacementResult {
        FILLED,
        PLACED,
        WAITING,
        MISSING_ITEM
    }

    public record PlacementOptions(boolean requireFullBlock, boolean allowFallingBlocks) {

        public static final PlacementOptions DEFAULT = new PlacementOptions(true, false);
        public static final PlacementOptions NON_FULL = new PlacementOptions(false, false);
        public static final PlacementOptions ANY_BLOCK_ITEM = new PlacementOptions(false, true);
    }

    public record PlacementRequest(BlockPos target, Predicate<ItemStack> itemPredicate, PlacementOptions options) {

        public PlacementRequest(final BlockPos target, final Predicate<ItemStack> itemPredicate) {
            this(target, itemPredicate, PlacementOptions.DEFAULT);
        }
    }

    /**
     * A face to click. {@code sneak} is set when the clicked block has its own use interaction and
     * the click must be sent while sneaking.
     */
    public record PlacementTarget(BlockHitResult hitResult, Vec3 hitLocation, boolean sneak) {
    }

    private enum Attempt {
        FILLED,
        PLACED,
        WAITING,
        ROTATING,
        MISSING_ITEM
    }
}
