package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

import java.util.List;

public final class CompassHudModule extends HudElementModule {

    public CompassHudModule() {
        super("compass_hud", "Compass HUD", "Top Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        Direction direction = client.player.getDirection();
        return List.of(
                "Facing " + direction.getName(),
                "Yaw " + Math.round(wrapYaw(client.player.getYRot())),
                compassLine(direction)
        );
    }

    static String compassLine(final Direction direction) {
        return switch (direction) {
            case NORTH -> "[N] E S W";
            case EAST -> "N [E] S W";
            case SOUTH -> "N E [S] W";
            case WEST -> "N E S [W]";
            default -> "N E S W";
        };
    }

    static float wrapYaw(final float yaw) {
        float wrapped = yaw % 360.0F;
        return wrapped < 0 ? wrapped + 360.0F : wrapped;
    }
}
