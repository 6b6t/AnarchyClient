package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One piston crystal setup. {@code outward} points from the target to the crystal; the piston
 * sits one block further out and faces back toward the target.
 */
record PistonCrystalPlan(
        BlockPos feet, Direction outward, BlockPos head, BlockPos crystal, BlockPos piston, List<BlockPos> power) {

    static PistonCrystalPlan of(final BlockPos feet, final Direction outward) {
        BlockPos head = feet.above();
        BlockPos crystal = head.relative(outward);
        BlockPos piston = crystal.relative(outward);
        // An end crystal's hitbox is two blocks wide, so it blocks placing power on most sides of
        // the piston. Only spots outside it, such as behind or below the piston, stay usable.
        AABB crystalBox = crystalHitbox(crystal);
        List<BlockPos> power =
                PistonPlacement.powerPositions(piston, outward.getOpposite(), List.of(crystal, head, crystal.above()))
                        .stream()
                        .filter(pos -> !new AABB(pos).intersects(crystalBox))
                        .toList();
        return new PistonCrystalPlan(feet.immutable(), outward, head, crystal, piston, power);
    }

    /** The hitbox of an end crystal resting in {@code cell}: two blocks wide and two tall. */
    static AABB crystalHitbox(final BlockPos cell) {
        return new AABB(
                cell.getX() - 0.5,
                cell.getY(),
                cell.getZ() - 0.5,
                cell.getX() + 1.5,
                cell.getY() + 2.0,
                cell.getZ() + 1.5);
    }

    Direction pistonFacing() {
        return this.outward.getOpposite();
    }

    /**
     * Returns the directions, pointing away from the target, where a piston crystal fits: the
     * crystal cell beside the head and the cell above it are open, the block under the crystal is
     * obsidian or bedrock, and the piston cell behind the crystal is open or holds a piston that
     * faces the target. Setups nearest to {@code eye} come first.
     */
    static List<Direction> candidates(final BlockGetter level, final BlockPos feet, final Vec3 eye) {
        List<Direction> directions = new ArrayList<>();
        if (!PistonPlacement.isOpen(level, feet.above())) {
            return directions;
        }
        for (Direction outward : Direction.Plane.HORIZONTAL) {
            PistonCrystalPlan plan = PistonCrystalPlan.of(feet, outward);
            BlockState base = level.getBlockState(plan.crystal().below());
            BlockState piston = level.getBlockState(plan.piston());
            // Vanilla's End Crystal item needs an empty (air) cell; grass, snow or water there refuse it.
            if ((base.is(Blocks.OBSIDIAN) || base.is(Blocks.BEDROCK))
                    && level.getBlockState(plan.crystal()).isAir()
                    && (PistonPlacement.isOpen(level, plan.piston())
                            || PistonPlacement.isPistonFacing(piston, plan.pistonFacing()))
                    && plan.power().stream()
                            .anyMatch(power -> PistonPlacement.isOpen(level, power)
                                    || PistonPlacement.existingPower(level, List.of(power))
                                            .isPresent())) {
                directions.add(outward);
            }
        }
        directions.sort(Comparator.comparingDouble(direction ->
                Vec3.atCenterOf(PistonCrystalPlan.of(feet, direction).piston()).distanceToSqr(eye)));
        return directions;
    }

    /** Whether the piston has pushed {@code crystal}, or extended to push it. */
    boolean pushed(final BlockGetter level, final Entity crystal) {
        BlockState piston = level.getBlockState(this.piston());
        return crystal.blockPosition().equals(this.head())
                || (piston.getBlock() instanceof PistonBaseBlock && piston.getValue(PistonBaseBlock.EXTENDED));
    }
}
