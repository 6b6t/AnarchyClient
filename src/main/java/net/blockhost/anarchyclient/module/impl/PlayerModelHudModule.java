package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;

import java.util.List;

public final class PlayerModelHudModule extends HudElementModule {

    public PlayerModelHudModule() {
        super("player_model_hud", "Player Model", "Bottom Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return List.of(
                "Pose " + client.player.getPose().name().toLowerCase(java.util.Locale.ROOT),
                "Yaw " + Math.round(client.player.getYRot()),
                "Pitch " + Math.round(client.player.getXRot()),
                "Body " + Math.round(client.player.yBodyRot)
        );
    }
}
