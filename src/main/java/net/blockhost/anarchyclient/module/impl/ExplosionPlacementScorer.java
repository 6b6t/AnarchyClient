package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Scores end crystal positions. {@code pos} is the block the crystal stands in, and the crystal
 * explodes at that block's bottom center.
 */
public final class ExplosionPlacementScorer {

    private final SelfDamagePolicy policy;
    private final double power;

    public ExplosionPlacementScorer(final SelfDamagePolicy policy, final double power) {
        this.policy = policy;
        this.power = power;
    }

    public PlacementScore score(final LocalPlayer self, final LivingEntity target, final BlockPos pos) {
        Vec3 explosion = DamageEstimator.crystalCenter(pos);
        double selfDamage = DamageEstimator.explosionDamage(self, explosion, this.power);
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, this.power);
        return new PlacementScore(pos, targetDamage, selfDamage, this.policy.allows(selfDamage, targetDamage));
    }

    public record PlacementScore(BlockPos pos, double targetDamage, double selfDamage, boolean allowed) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.65;
        }
    }
}
