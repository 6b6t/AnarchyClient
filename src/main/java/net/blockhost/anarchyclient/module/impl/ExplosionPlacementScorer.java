package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ExplosionPlacementScorer {

    private final SelfDamagePolicy policy;
    private final double radius;

    public ExplosionPlacementScorer(final SelfDamagePolicy policy, final double radius) {
        this.policy = policy;
        this.radius = radius;
    }

    public PlacementScore score(final LocalPlayer self, final LivingEntity target, final BlockPos pos) {
        Vec3 explosion = Vec3.atCenterOf(pos);
        double selfDamage = DamageEstimator.explosionDamage(self, explosion, this.radius);
        double targetDamage = DamageEstimator.explosionDamage(target, explosion, this.radius);
        return new PlacementScore(pos, targetDamage, selfDamage, this.policy.allows(selfDamage, targetDamage));
    }

    public record PlacementScore(BlockPos pos, double targetDamage, double selfDamage, boolean allowed) {

        double value() {
            return this.targetDamage - this.selfDamage * 0.65;
        }
    }
}
