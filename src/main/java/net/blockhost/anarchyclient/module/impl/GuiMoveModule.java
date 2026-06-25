package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public final class GuiMoveModule extends Module {

    public GuiMoveModule() {
        super("gui_move", "GUI Move", ModuleCategory.MOVEMENT);
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (client.player == null || client.gui.screen() == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        input.keyPresses = new Input(
                client.options.keyUp.isDown(),
                client.options.keyDown.isDown(),
                client.options.keyLeft.isDown(),
                client.options.keyRight.isDown(),
                client.options.keyJump.isDown(),
                client.options.keyShift.isDown(),
                client.options.keySprint.isDown()
        );
        InputStates.refreshMoveVector(input);
    }
}
