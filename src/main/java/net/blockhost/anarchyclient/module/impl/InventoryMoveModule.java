package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

public final class InventoryMoveModule extends Module {

    private final BooleanSetting allowJump = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("jump")
            .name("Jump")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowSneak = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sneak")
            .name("Sneak")
            .defaultValue(true)
            .build()));
    private final BooleanSetting allowChat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chat")
            .name("Chat")
            .defaultValue(false)
            .build()));

    public InventoryMoveModule() {
        super("inventory_move", "Inventory Move", ModuleCategory.MOVEMENT, java.util.List.of("inv_move"));
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (client.player == null
                || client.gui.screen() == null
                || client.gui.screen() instanceof AnarchyClientScreen
                || !this.allowChat.value() && client.gui.screen() instanceof ChatScreen) {
            return;
        }
        input.keyPresses = new Input(
                client.options.keyUp.isDown(),
                client.options.keyDown.isDown(),
                client.options.keyLeft.isDown(),
                client.options.keyRight.isDown(),
                this.allowJump.value() && client.options.keyJump.isDown(),
                this.allowSneak.value() && client.options.keyShift.isDown(),
                client.options.keySprint.isDown()
        );
        InputStates.refreshMoveVector(input);
    }
}
