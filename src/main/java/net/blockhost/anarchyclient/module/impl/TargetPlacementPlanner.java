package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class TargetPlacementPlanner {

    private TargetPlacementPlanner() {
    }

    public static List<ExplosionPlacementScorer.PlacementScore> explosionPlacements(
            final Minecraft client,
            final LocalPlayer self,
            final LivingEntity target,
            final int radius,
            final ExplosionPlacementScorer scorer
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
                    if (!canPlaceCrystal(client, base)) {
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
        if (client.level == null) {
            return false;
        }
        return (client.level.getBlockState(base).is(Blocks.OBSIDIAN) || client.level.getBlockState(base).is(Blocks.BEDROCK))
                && client.level.isEmptyBlock(base.above())
                && client.level.isEmptyBlock(base.above(2));
    }
}
