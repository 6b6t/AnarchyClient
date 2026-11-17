package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;

import java.util.List;

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
        super("inventory_move", "Inventory Move", ModuleCategory.MOVEMENT,
                "Lets you walk, jump, and sneak while an inventory or other screen is open.", List.of("inv_move"));
    }

    @Override
    public void updateInput(final Minecraft client, final ClientInput input) {
        if (client.player == null
                || client.gui.screen() == null
                || client.gui.screen() instanceof AnarchyClientScreen
                || !this.allowChat.value() && client.gui.screen() instanceof ChatScreen) {
            return;
        }
        // Vanilla releases every key mapping while a screen is open, so read the physical key state instead.
        input.keyPresses = new Input(
                physicallyDown(client, client.options.keyUp),
                physicallyDown(client, client.options.keyDown),
                physicallyDown(client, client.options.keyLeft),
                physicallyDown(client, client.options.keyRight),
                this.allowJump.value() && physicallyDown(client, client.options.keyJump),
                this.allowSneak.value() && physicallyDown(client, client.options.keyShift),
                physicallyDown(client, client.options.keySprint)
        );
        InputStates.refreshMoveVector(input);
    }

    private static boolean physicallyDown(final Minecraft client, final KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        InputConstants.Key key = KeyMappingHelper.getBoundKeyOf(mapping);
        return key.getType() == InputConstants.Type.MOUSE
                ? InputStates.mousePressed(client, key.getValue())
                : InputStates.keyPressed(client, key.getValue());
    }
}
