package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class MovementChecks {

    private MovementChecks() {
    }

    static boolean movingTowardAir(final LocalPlayer player, final double lookAhead) {
        Vec3 movement = player.getDeltaMovement();
        Vec3 horizontal = new Vec3(movement.x, 0.0, movement.z);
        if (horizontal.horizontalDistanceSqr() < 1.0E-4) {
            horizontal = player.getViewVector(0.0F).horizontal();
        }
        if (horizontal.horizontalDistanceSqr() < 1.0E-4) {
            return false;
        }

        Vec3 direction = horizontal.normalize().scale(lookAhead);
        AABB belowAhead = player.getBoundingBox()
                .move(direction.x, -0.08, direction.z)
                .inflate(-0.02, 0.0, -0.02);
        return player.level().noBlockCollision(player, belowAhead);
    }

    static boolean movingTowardAir(final LocalPlayer player, final ClientInput input, final double lookAhead) {
        Vec3 horizontal = intendedMovement(player, input);
        if (horizontal.horizontalDistanceSqr() < 1.0E-4) {
            return false;
        }
        Vec3 direction = horizontal.normalize().scale(lookAhead);
        AABB belowAhead = player.getBoundingBox()
                .move(direction.x, -0.08, direction.z)
                .inflate(-0.02, 0.0, -0.02);
        return player.level().noBlockCollision(player, belowAhead);
    }

    private static Vec3 intendedMovement(final LocalPlayer player, final ClientInput input) {
        double forwardImpulse = 0.0;
        if (input.keyPresses.forward()) {
            forwardImpulse += 1.0;
        }
        if (input.keyPresses.backward()) {
            forwardImpulse -= 1.0;
        }
        double rightImpulse = 0.0;
        if (input.keyPresses.right()) {
            rightImpulse += 1.0;
        }
        if (input.keyPresses.left()) {
            rightImpulse -= 1.0;
        }
        Vec3 forward = player.getViewVector(0.0F).horizontal();
        if (forward.horizontalDistanceSqr() < 1.0E-4) {
            return Vec3.ZERO;
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return forward.scale(forwardImpulse).add(right.scale(rightImpulse));
    }
}
