package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
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
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
        }
        LocalPlayer player = client.player;
        if (player == null || player.input != input || client.gui.screen() != null || this.cooldownTicks > 0) {
            return;
        }
        if (player.onGround() && input.hasForwardImpulse() && MovementChecks.movingTowardAir(player, this.lookAhead.value())) {
            input.keyPresses = InputStates.withJump(input.keyPresses, true);
            this.cooldownTicks = 8;
        }
    }
}
