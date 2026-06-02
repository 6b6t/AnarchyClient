package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class NoFallModule extends Module {

    private final NumberSetting minFallDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_fall_distance")
            .name("Fall Dist")
            .defaultValue(3.0)
            .min(2.0)
            .max(10.0)
            .step(0.5)
            .build()));

    public NoFallModule() {
        super("no_fall", "No Fall", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            return;
        }
        if (player.fallDistance >= this.minFallDistance.value()
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isPassenger()
                && !player.isInWater()) {
            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
        }
    }
}
