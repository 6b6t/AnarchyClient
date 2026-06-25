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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public final class BlockPlacer {

    private BlockPlacer() {
    }

    public static PlacementResult place(final Minecraft client, final String owner, final BlockPos target,
                                        final boolean rotate, final float maxTurnDegrees) {
        return place(client, owner, target, rotate, maxTurnDegrees, stack -> true);
    }

    public static PlacementResult place(final Minecraft client, final String owner, final BlockPos target,
                                        final boolean rotate, final float maxTurnDegrees,
                                        final Predicate<ItemStack> itemPredicate) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return PlacementResult.WAITING;
        }
        if (!needsPlacement(client.level, target)) {
            return PlacementResult.FILLED;
        }
        Optional<PlacementTarget> placementTarget = findPlacementTarget(client.level, target);
        if (placementTarget.isEmpty()) {
            return PlacementResult.WAITING;
        }
        OptionalInt hotbarSlot = findPlaceableHotbarSlot(player.getInventory(), client.level, player, target, itemPredicate);
        if (hotbarSlot.isEmpty()) {
            return PlacementResult.MISSING_ITEM;
        }
        if (!SilentHotbar.select(player, owner, hotbarSlot.orElseThrow(), SilentHotbar.PRIORITY_COMBAT, 3, true)) {
            return PlacementResult.WAITING;
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
        }
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, placement.hitResult());
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            return PlacementResult.PLACED;
        }
        return PlacementResult.WAITING;
    }

    public static boolean needsPlacement(final ClientLevel level, final BlockPos target) {
        BlockState state = level.getBlockState(target);
        return state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    public static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                                      final LocalPlayer player, final BlockPos target) {
        return findPlaceableHotbarSlot(inventory, level, player, target, stack -> true);
    }

    public static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                                      final LocalPlayer player, final BlockPos target,
                                                      final Predicate<ItemStack> itemPredicate) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (itemPredicate.test(stack) && isPlaceableBlock(stack, level, player, target)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    public static boolean isPlaceableBlock(final ItemStack stack, final ClientLevel level, final LocalPlayer player,
                                           final BlockPos target) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        if (!needsPlacement(level, target)
                || !Block.isShapeFullBlock(state.getCollisionShape(level, target))
                || block instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(target.below()))) {
            return false;
        }
        return level.isUnobstructed(state, target, CollisionContext.placementContext(player));
    }

    public static Optional<PlacementTarget> findPlacementTarget(final ClientLevel level, final BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.relative(direction);
            Direction face = direction.getOpposite();
            if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, face)) {
                Vec3 hitLocation = Vec3.atCenterOf(neighbor).relative(face, 0.5);
                return Optional.of(new PlacementTarget(new BlockHitResult(hitLocation, face, neighbor, false), hitLocation));
            }
        }
        return Optional.empty();
    }

    public enum PlacementResult {
        FILLED,
        PLACED,
        WAITING,
        MISSING_ITEM
    }

    public record PlacementTarget(BlockHitResult hitResult, Vec3 hitLocation) {
    }
}
