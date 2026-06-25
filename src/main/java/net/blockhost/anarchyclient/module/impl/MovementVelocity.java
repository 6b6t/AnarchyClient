package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

final class MovementVelocity {

    private MovementVelocity() {
    }

    static Vec3 fromKeys(final Minecraft client, final float yaw, final double speed) {
        double forward = impulse(client.options.keyUp.isDown(), client.options.keyDown.isDown());
        double strafe = impulse(client.options.keyLeft.isDown(), client.options.keyRight.isDown());
        return fromInput(yaw, forward, strafe, speed);
    }

    static Vec3 fromInput(final float yawDegrees, final double forward, final double strafe, final double speed) {
        if (forward == 0.0 && strafe == 0.0) {
            return Vec3.ZERO;
        }
        double yaw = yawDegrees * Mth.DEG_TO_RAD;
        double sin = Math.sin(yaw);
        double cos = Math.cos(yaw);
        double x = strafe * cos - forward * sin;
        double z = forward * cos + strafe * sin;
        Vec3 vector = new Vec3(x, 0.0, z);
        if (vector.horizontalDistanceSqr() <= 1.0E-6) {
            return Vec3.ZERO;
        }
        return vector.normalize().scale(speed);
    }

    static boolean moving(final Minecraft client) {
        return client.options.keyUp.isDown()
                || client.options.keyDown.isDown()
                || client.options.keyLeft.isDown()
                || client.options.keyRight.isDown();
    }

    private static int impulse(final boolean positive, final boolean negative) {
        if (positive == negative) {
            return 0;
        }
        return positive ? 1 : -1;
    }
}
