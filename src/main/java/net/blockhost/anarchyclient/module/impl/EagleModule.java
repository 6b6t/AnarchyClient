package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
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
    private boolean pressingSneak;

    public EagleModule() {
        super("eagle", "Eagle", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.input == null || client.screen != null) {
            return;
        }
        boolean shouldSneak = player.onGround() && MovementChecks.movingTowardAir(player, this.lookAhead.value());
        player.input.keyPresses = InputStates.withShift(player.input.keyPresses, shouldSneak);
        if (shouldSneak || this.pressingSneak) {
            client.options.keyShift.setDown(shouldSneak);
            this.pressingSneak = shouldSneak;
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (client.options != null) {
            client.options.keyShift.setDown(false);
        }
        this.pressingSneak = false;
    }
}
