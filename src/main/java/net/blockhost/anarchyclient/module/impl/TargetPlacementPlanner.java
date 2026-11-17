package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TargetPlacementPlanner {

    private TargetPlacementPlanner() {
    }

    /**
     * Scores crystal positions around the target that the player can reach with the default
     * block interaction range.
     */
    public static List<ExplosionPlacementScorer.PlacementScore> explosionPlacements(
            final Minecraft client,
            final LocalPlayer self,
            final LivingEntity target,
            final int radius,
            final ExplosionPlacementScorer scorer
    ) {
        return explosionPlacements(client, self, target, radius, scorer, self.blockInteractionRange());
    }

    /**
     * Scores crystal positions around the target. Only obsidian or bedrock within
     * {@code placeRange} of the player's eyes is considered, so every result can be clicked.
     */
    public static List<ExplosionPlacementScorer.PlacementScore> explosionPlacements(
            final Minecraft client,
            final LocalPlayer self,
            final LivingEntity target,
            final int radius,
            final ExplosionPlacementScorer scorer,
            final double placeRange
    ) {
        if (client.level == null) {
            return List.of();
        }
        BlockPos center = target.blockPosition();
        List<ExplosionPlacementScorer.PlacementScore> scores = new ArrayList<>();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - 1; y <= center.getY() + 2; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos base = new BlockPos(x, y, z);
                    if (!CombatTargets.canReachBlock(self, base, placeRange) || !canPlaceCrystal(client.level, base)) {
                        continue;
                    }
                    ExplosionPlacementScorer.PlacementScore score = scorer.score(self, target, base.above());
                    if (score.allowed()) {
                        scores.add(score);
                    }
                }
            }
        }
        scores.sort(Comparator.comparingDouble(ExplosionPlacementScorer.PlacementScore::value).reversed());
        return List.copyOf(scores);
    }

    public static boolean canPlaceCrystal(final Minecraft client, final BlockPos base) {
        return client.level != null && canPlaceCrystal(client.level, base);
    }

    /**
     * Mirrors {@code EndCrystalItem.useOn}: the base must be obsidian or bedrock, the block above
     * must be empty, and no entity may touch the 1x2x1 box above the base.
     */
    public static boolean canPlaceCrystal(final Level level, final BlockPos base) {
        if (!isCrystalBase(level.getBlockState(base))) {
            return false;
        }
        BlockPos above = base.above();
        if (!level.isEmptyBlock(above)) {
            return false;
        }
        return level.getEntities(null, crystalSpace(above)).isEmpty();
    }

    static boolean isCrystalBase(final BlockState state) {
        return state.is(Blocks.OBSIDIAN) || state.is(Blocks.BEDROCK);
    }

    /** The box {@code EndCrystalItem.useOn} checks for entities, starting at the crystal block. */
    static AABB crystalSpace(final BlockPos crystalPos) {
        double x = crystalPos.getX();
        double y = crystalPos.getY();
        double z = crystalPos.getZ();
        return new AABB(x, y, z, x + 1.0, y + 2.0, z + 1.0);
    }
}
