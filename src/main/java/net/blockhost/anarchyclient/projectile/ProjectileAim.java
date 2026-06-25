package net.blockhost.anarchyclient.projectile;

import net.blockhost.anarchyclient.rotation.Rotation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class ProjectileAim {

    private ProjectileAim() {
    }

    public static Rotation rotationToHit(final Vec3 shooterEye, final LivingEntity target,
                                         final double velocity, final double gravity) {
        Vec3 point = predictedPoint(shooterEye, target, velocity, gravity);
        return Rotation.lookingAt(point, shooterEye);
    }

    public static Vec3 predictedPoint(final Vec3 shooterEye, final LivingEntity target,
                                      final double velocity, final double gravity) {
        Vec3 center = target.getBoundingBox().getCenter();
        double safeVelocity = Math.max(0.1, velocity);
        double travelTicks = shooterEye.distanceTo(center) / safeVelocity;
        Vec3 lead = target.getDeltaMovement().scale(travelTicks);
        double dropCompensation = 0.5 * Math.max(0.0, gravity) * travelTicks * travelTicks;
        return center.add(lead.x, lead.y + dropCompensation, lead.z);
    }

    public static double throwableVelocity(final String type) {
        return switch (type) {
            case "bow", "crossbow" -> 3.0;
            case "trident" -> 2.5;
            case "potion" -> 0.9;
            default -> 1.5;
        };
    }

    public static double gravity(final String type) {
        return switch (type) {
            case "bow", "crossbow", "trident" -> 0.05;
            default -> 0.03;
        };
    }
}
