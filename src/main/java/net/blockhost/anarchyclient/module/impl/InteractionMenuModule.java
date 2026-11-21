package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import java.util.Locale;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

public final class InteractionMenuModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Copy Info")
            .addAllOptions(List.of("Copy Info", "Command", "Friend"))
            .build()));
    private final StringSetting command = this.setting(StringSetting.from(StringSetting.builder()
            .id("command")
            .name("Command")
            .defaultValue("/msg {target} hi")
            .build()));
    private boolean wasPressed;

    public InteractionMenuModule() {
        super(
                "interaction_menu",
                "Interaction Menu",
                ModuleCategory.MISC,
                "Copies the target's info, sends a command, or adds a friend when you middle click a target.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            this.wasPressed = false;
            return;
        }
        boolean pressed = InputStates.mousePressed(client, InputConstants.MOUSE_BUTTON_MIDDLE);
        if (pressed && !this.wasPressed) {
            this.execute(client);
        }
        this.wasPressed = pressed;
    }

    private void execute(final Minecraft client) {
        String info = targetInfo(client);
        switch (this.mode.value()) {
            case "Command" ->
                ChatActions.run(
                        client,
                        this.command
                                .value()
                                .replace("{target}", targetName(client))
                                .replace("{info}", info));
            case "Friend" -> {
                if (client.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof Player player) {
                    AnarchyClient.FRIENDS.add(player.getScoreboardName());
                    client.player.sendSystemMessage(Translations.component(
                            "anarchyclient.message.interaction_menu.added_friend",
                            "Added friend %s.",
                            player.getScoreboardName()));
                }
            }
            default -> {
                if (!info.isBlank()) {
                    client.keyboardHandler.setClipboard(info);
                    client.player.sendSystemMessage(
                            Translations.component("anarchyclient.message.interaction_menu.copied", "Copied %s", info));
                }
            }
        }
    }

    static String targetInfo(final Minecraft client) {
        if (client.hitResult instanceof EntityHitResult hit) {
            Entity entity = hit.getEntity();
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()) + " "
                    + entity.getScoreboardName() + " "
                    + String.format(Locale.ROOT, "%.2f %.2f %.2f", entity.getX(), entity.getY(), entity.getZ());
        }
        if (client.hitResult instanceof BlockHitResult hit) {
            return hit.getBlockPos().getX() + " " + hit.getBlockPos().getY() + " "
                    + hit.getBlockPos().getZ();
        }
        return "";
    }

    private static String targetName(final Minecraft client) {
        if (client.hitResult instanceof EntityHitResult hit) {
            return hit.getEntity().getScoreboardName();
        }
        return "";
    }
}
