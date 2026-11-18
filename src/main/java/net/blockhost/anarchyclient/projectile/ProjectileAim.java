package net.blockhost.anarchyclient.projectile;

import net.blockhost.anarchyclient.rotation.Rotation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public final class ProjectileAim {

    private static final double MAX_TICK_MOVEMENT_SQR = 10.0 * 10.0;

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
        Vec3 lead = tickMovement(target).scale(travelTicks);
        double dropCompensation = 0.5 * Math.max(0.0, gravity) * travelTicks * travelTicks;
        return center.add(lead.x, lead.y + dropCompensation, lead.z);
    }

    /**
     * Movement over the last tick. Remote entities have no synced delta movement on the client,
     * so this uses the change in position instead. Teleport-sized jumps count as no movement.
     */
    public static Vec3 tickMovement(final Entity target) {
        Vec3 movement = new Vec3(target.getX() - target.xo, target.getY() - target.yo, target.getZ() - target.zo);
        return movement.lengthSqr() > MAX_TICK_MOVEMENT_SQR ? Vec3.ZERO : movement;
    }

    /** Projectile type name for a thrown or fired item, as used by the speed, gravity and pitch helpers. */
    public static String typeOf(final ItemStack stack) {
        if (stack.is(Items.TRIDENT)) {
            return "trident";
        }
        if (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)) {
            return "potion";
        }
        if (stack.is(Items.EXPERIENCE_BOTTLE)) {
            return "experience_bottle";
        }
        return "throwable";
    }

    /** Launch speed in blocks per tick, matching the vanilla item that throws the projectile. */
    public static double throwableVelocity(final String type) {
        return switch (type) {
            case "bow", "crossbow" -> 3.0;
            case "trident" -> 2.5;
            case "potion" -> 0.5;
            case "experience_bottle" -> 0.7;
            default -> 1.5;
        };
    }

    public static double gravity(final String type) {
        return switch (type) {
            case "bow", "crossbow", "trident", "potion" -> 0.05;
            case "experience_bottle" -> 0.07;
            default -> 0.03;
        };
    }

    /**
     * Pitch offset vanilla adds when throwing. Potions and experience bottles leave 20 degrees above the view, so the
     * view must aim 20 degrees lower than the launch direction.
     */
    public static float pitchOffset(final String type) {
        return switch (type) {
            case "potion", "experience_bottle" -> -20.0F;
            default -> 0.0F;
        };
    }

    /** View rotation that launches a projectile along {@code launch} once the item's pitch offset is applied. */
    public static Rotation viewForLaunch(final Rotation launch, final String type) {
        float pitch = Math.max(-90.0F, Math.min(90.0F, launch.pitch() - pitchOffset(type)));
        return new Rotation(launch.yaw(), pitch);
    }
}
