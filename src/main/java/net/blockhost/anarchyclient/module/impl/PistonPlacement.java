package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.placement.BlockPlacer;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

/**
 * Places pistons that face a chosen direction and the redstone blocks that power them.
 *
 * <p>A piston faces away from the direction the player looks while placing it, so the placement
 * turns the player to a fixed horizontal rotation and waits until the server has that rotation,
 * and has turned the player's head to it, before clicking.
 */
final class PistonPlacement {

    static final Predicate<ItemStack> PISTONS = stack -> stack.is(Items.PISTON) || stack.is(Items.STICKY_PISTON);
    static final Predicate<ItemStack> REDSTONE_BLOCKS = stack -> stack.is(Items.REDSTONE_BLOCK);

    private static final float ROTATION_TOLERANCE = 1.0F;
    /**
     * Ticks to wait after the server has the rotation. Vanilla takes a placed block's facing from
     * the player's head rotation, and the server only turns the head when it ticks the player, so
     * a click in the same server tick as the rotation packet still uses the old facing.
     */
    static final int HEAD_TURN_TICKS = 2;
    private static final Map<String, Integer> SETTLED_TICKS = new HashMap<>();

    private PistonPlacement() {
    }

    /** Returns the rotation that makes a newly placed piston face {@code facing}. */
    static Rotation rotationFor(final Direction facing) {
        return new Rotation(Rotation.wrapDegrees(facing.getOpposite().toYRot()), 0.0F);
    }

    /** Same rule as {@link BlockPlacer#needsPlacement}, for any block getter. */
    static boolean isOpen(final BlockGetter level, final BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() || !state.getFluidState().isEmpty();
    }

    static boolean isPiston(final BlockState state) {
        return state.is(Blocks.PISTON) || state.is(Blocks.STICKY_PISTON);
    }

    static boolean isPistonFacing(final BlockState state, final Direction facing) {
        return isPiston(state) && state.getValue(PistonBaseBlock.FACING) == facing;
    }

    static boolean isExtended(final BlockState state) {
        return isPiston(state) && state.getValue(PistonBaseBlock.EXTENDED);
    }

    /**
     * Returns the blocks next to a piston where a redstone block powers it, in a stable order:
     * above first, then the sides. The piston's front and the {@code excluded} cells are skipped.
     */
    static List<BlockPos> powerPositions(final BlockPos piston, final Direction facing, final List<BlockPos> excluded) {
        List<BlockPos> positions = new ArrayList<>();
        for (Direction direction : List.of(Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST,
                Direction.EAST, Direction.DOWN)) {
            if (direction == facing) {
                continue;
            }
            BlockPos pos = piston.relative(direction);
            if (!excluded.contains(pos)) {
                positions.add(pos);
            }
        }
        return positions;
    }

    /** Returns the first power position that already holds a redstone block. */
    static Optional<BlockPos> existingPower(final BlockGetter level, final List<BlockPos> positions) {
        for (BlockPos pos : positions) {
            if (level.getBlockState(pos).is(Blocks.REDSTONE_BLOCK)) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }

    /**
     * Places a piston at {@code pos} that faces {@code facing}. Returns {@link Result#WAITING} until
     * the rotation reached the server, so call it every tick until it returns another result.
     */
    static Result placePiston(final Minecraft client, final Module owner, final BlockPos pos, final Direction facing,
                              final double range) {
        LocalPlayer player = client.player;
        ClientLevel level = client.level;
        if (player == null || level == null || client.gameMode == null) {
            return Result.WAITING;
        }
        BlockState state = level.getBlockState(pos);
        if (isPistonFacing(state, facing)) {
            return Result.PRESENT;
        }
        if (!BlockPlacer.needsPlacement(level, pos) || !CombatTargets.canReachBlock(player, pos, range)) {
            return Result.BLOCKED;
        }
        Optional<BlockPlacer.PlacementTarget> target = BlockPlacer.findPlacementTarget(level, pos);
        if (target.isEmpty()) {
            return Result.BLOCKED;
        }
        OptionalInt slot = BlockPlacer.findPlaceableHotbarSlot(player.getInventory(), level, player, pos, PISTONS);
        if (slot.isEmpty()) {
            // A piston in the hotbar that cannot go here means the cell is obstructed (a player or crystal stands
            // in it), not that pistons ran out.
            return WorldInteraction.hasHotbarItem(player.getInventory(), PISTONS) ? Result.BLOCKED : Result.MISSING_ITEM;
        }
        if (!SilentHotbar.select(player, owner.id(), slot.orElseThrow(), SilentHotbar.PRIORITY_COMBAT, 3, true)) {
            return Result.WAITING;
        }
        Rotation rotation = rotationFor(facing);
        RotationManager.request(new RotationRequest(owner.id(), rotation, 80, 180.0F, 3, ROTATION_TOLERANCE,
                RotationTurnMode.INSTANT, true));
        RotationManager.apply(player);
        if (RotationManager.serverRotation(player).angleTo(rotation) > ROTATION_TOLERANCE) {
            SETTLED_TICKS.remove(owner.id());
            return Result.WAITING;
        }
        if (!headTurned(SETTLED_TICKS.merge(owner.id(), 1, Integer::sum))) {
            return Result.WAITING;
        }
        SETTLED_TICKS.remove(owner.id());
        BlockPlacer.PlacementTarget placement = target.orElseThrow();
        if (BlockPlacer.useItemOn(client, player, placement.hitResult(), placement.sneak()).consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            return Result.PLACED;
        }
        return Result.WAITING;
    }

    /** Returns whether the rotation has been on the server long enough for the head to follow it. */
    static boolean headTurned(final int ticksSinceRotationSent) {
        return ticksSinceRotationSent > HEAD_TURN_TICKS;
    }

    /** Places a redstone block on the first open power position next to the piston. */
    static Result placePower(final Minecraft client, final Module owner, final List<BlockPos> positions,
                             final boolean rotate) {
        ClientLevel level = client.level;
        LocalPlayer player = client.player;
        if (level == null || player == null) {
            return Result.WAITING;
        }
        if (existingPower(level, positions).isPresent()) {
            return Result.PRESENT;
        }
        for (BlockPos pos : positions) {
            if (!BlockPlacer.needsPlacement(level, pos) || !CombatTargets.canReachBlock(player, pos)
                    || CombatPlacementPlanner.playerInside(player, pos)) {
                continue;
            }
            switch (BlockPlacement.place(client, owner, pos, rotate, 90.0F, REDSTONE_BLOCKS)) {
                case PLACED, FILLED -> {
                    return Result.PLACED;
                }
                case WAITING -> {
                    return Result.WAITING;
                }
                case UNPLACEABLE -> {
                    if (!WorldInteraction.hasHotbarItem(player.getInventory(), REDSTONE_BLOCKS)) {
                        return Result.MISSING_ITEM;
                    }
                }
            }
        }
        return Result.BLOCKED;
    }

    enum Result {
        PLACED,
        PRESENT,
        WAITING,
        BLOCKED,
        MISSING_ITEM
    }
}
