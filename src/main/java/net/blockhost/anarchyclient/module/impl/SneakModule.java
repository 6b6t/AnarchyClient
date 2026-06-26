package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;

public final class SneakModule extends Module {

    private final BooleanSetting pauseInGui = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_gui")
            .name("GUI Pause")
            .defaultValue(true)
            .build()));
    private final BooleanSetting onlyOnGround = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("only_on_ground")
            .name("Ground Only")
            .defaultValue(false)
            .build()));
    private final BooleanSetting pauseInLiquid = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_liquid")
            .name("Liquid Pause")
            .defaultValue(false)
            .build()));
    private final BooleanSetting pauseFallFlying = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_flying")
            .name("Flight Pause")
            .defaultValue(true)
            .build()));

    public SneakModule() {
        super("sneak", "Sneak", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        LocalPlayer player = client.player;
        if (player == null || player.input != input) {
            return;
        }
        if (this.pauseInGui.value() && client.gui.screen() != null) {
            return;
        }
        if (this.onlyOnGround.value() && !player.onGround()) {
            return;
        }
        if (this.pauseInLiquid.value() && (player.isInWater() || player.isInLava())) {
            return;
        }
        if (this.pauseFallFlying.value() && player.isFallFlying()) {
            return;
        }
        input.keyPresses = InputStates.withShift(input.keyPresses, true);
    }
}
