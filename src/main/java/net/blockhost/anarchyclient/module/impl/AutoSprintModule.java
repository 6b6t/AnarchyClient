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
    private final BooleanSetting omniSprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("omni_sprint")
            .name("Omni")
            .defaultValue(false)
            .build()));
    private final BooleanSetting allowUsingItem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("allow_using_item")
            .name("Use Item")
            .defaultValue(false)
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
        boolean moving = this.omniSprint.value()
                ? player.input.keyPresses.forward() || player.input.keyPresses.backward() || player.input.keyPresses.left() || player.input.keyPresses.right()
                : player.input.hasForwardImpulse();
        boolean blocked = player.isCrouching()
                || player.isInWater()
                || !this.allowUsingItem.value() && player.isUsingItem();
        if ((moving || this.keepSprint.value() && player.isSprinting()) && !blocked) {
            player.setSprinting(true);
        }
    }
}
