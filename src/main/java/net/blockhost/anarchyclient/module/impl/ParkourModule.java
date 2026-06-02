package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class ParkourModule extends Module {

    private final NumberSetting lookAhead = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("look_ahead")
            .name("Look Ahead")
            .defaultValue(0.6)
            .min(0.2)
            .max(1.2)
            .step(0.05)
            .build()));
    private int cooldownTicks;

    public ParkourModule() {
        super("parkour", "Parkour", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
        LocalPlayer player = client.player;
        if (player == null || player.input == null || client.screen != null || this.cooldownTicks > 0) {
            return;
        }
        if (player.onGround() && player.input.hasForwardImpulse() && MovementChecks.movingTowardAir(player, this.lookAhead.value())) {
            player.input.keyPresses = InputStates.withJump(player.input.keyPresses, true);
            player.jumpFromGround();
            this.cooldownTicks = 8;
        }
    }
}
