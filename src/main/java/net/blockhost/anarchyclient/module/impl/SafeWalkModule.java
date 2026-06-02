package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

public final class SafeWalkModule extends Module {

    private final NumberSetting lookAhead = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("look_ahead")
            .name("Look Ahead")
            .defaultValue(0.45)
            .min(0.15)
            .max(1.0)
            .step(0.05)
            .build()));

    public SafeWalkModule() {
        super("safe_walk", "Safe Walk", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null || !player.onGround()) {
            return;
        }
        if (MovementChecks.movingTowardAir(player, this.lookAhead.value())) {
            Vec3 movement = player.getDeltaMovement();
            player.setDeltaMovement(movement.x * 0.25, movement.y, movement.z * 0.25);
        }
    }
}
