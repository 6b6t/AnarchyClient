package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;

public final class EagleModule extends Module {

    private final NumberSetting lookAhead = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("look_ahead")
            .name("Look Ahead")
            .defaultValue(0.35)
            .min(0.15)
            .max(0.9)
            .step(0.05)
            .build()));

    public EagleModule() {
        super("eagle", "Eagle", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || player.input != input || client.screen != null) {
            return;
        }
        boolean shouldSneak = player.onGround() && MovementChecks.movingTowardAir(player, this.lookAhead.value());
        input.keyPresses = InputStates.withShift(input.keyPresses, input.keyPresses.shift() || shouldSneak);
    }
}
