package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.player.LocalPlayer;
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
}
