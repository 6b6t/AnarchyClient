package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoSprintModule extends Module {

    private final BooleanSetting keepSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_sprint")
            .name("Keep Sprint")
            .defaultValue(true)
            .build()));

    public AutoSprintModule() {
        super("auto_sprint", "Auto Sprint", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.input == null) {
            return;
        }
        if (player.input.hasForwardImpulse() && !player.isCrouching() && !player.isUsingItem() || this.keepSprint.value() && player.isSprinting()) {
            player.setSprinting(true);
        }
    }
}
