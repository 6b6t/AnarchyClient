package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;

public final class AutoWalkModule extends Module {

    private final BooleanSetting pauseInGui = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("pause_in_gui")
            .name("GUI Pause")
            .defaultValue(true)
            .build()));
    private final BooleanSetting stopOnCollision = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("stop_on_collision")
            .name("Wall Stop")
            .defaultValue(true)
            .build()));
    private final BooleanSetting sprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sprint")
            .name("Sprint")
            .defaultValue(false)
            .build()));

    public AutoWalkModule() {
        super("auto_walk", "Auto Walk", ModuleCategory.MOVEMENT);
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
        if (this.stopOnCollision.value() && player.horizontalCollision) {
            return;
        }
        if (input.keyPresses.backward()) {
            return;
        }

        input.keyPresses = InputStates.withForward(input.keyPresses, true);
        if (this.sprint.value()) {
            input.keyPresses = InputStates.withSprint(input.keyPresses, true);
            player.setSprinting(true);
        }
        InputStates.refreshMoveVector(input);
    }
}
