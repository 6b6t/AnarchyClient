package net.blockhost.anarchyclient.module.impl;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class DamageEstimator {

    private DamageEstimator() {
    }

    public static double explosionDamage(final LivingEntity entity, final Vec3 explosion, final double radius) {
        if (entity == null || radius <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(entity.distanceToSqr(explosion));
        if (distance > radius) {
            return 0.0;
        }
        double exposure = 1.0 - distance / radius;
        return Math.max(0.0, (exposure * exposure + exposure) * 0.5 * 12.0 + 1.0);
    }
}
