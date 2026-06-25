package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class HighJumpModule extends Module {

    private final NumberSetting height = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("height")
            .name("Height")
            .defaultValue(0.75)
            .min(0.42)
            .max(2.5)
            .step(0.05)
            .build()));

    public HighJumpModule() {
        super("high_jump", "High Jump", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player != null && player.onGround() && client.options.keyJump.isDown()) {
            player.setDeltaMovement(player.getDeltaMovement().x, this.height.value(), player.getDeltaMovement().z);
        }
    }
}
