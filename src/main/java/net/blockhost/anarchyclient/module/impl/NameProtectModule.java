package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

public final class NameProtectModule extends Module {

    private final StringSetting replacement = this.setting(StringSetting.from(StringSetting.builder()
            .id("replacement")
            .name("Name")
            .defaultValue("You")
            .build()));
    private final BooleanSetting incoming = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("incoming")
            .name("Incoming")
            .defaultValue(true)
            .build()));
    private final BooleanSetting outgoing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("outgoing")
            .name("Outgoing")
            .defaultValue(false)
            .build()));
    private final BooleanSetting tab = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("tab")
            .name("Tab")
            .defaultValue(true)
            .build()));

    public NameProtectModule() {
        super("name_protect", "Name Protect", ModuleCategory.MISC);
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (!this.incoming.value()) {
            return message;
        }
        return replaceLocalName(client, message, this.replacement.value());
    }

    @Override
    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        if (!this.outgoing.value() || client == null || client.getUser() == null || message == null) {
            return message;
        }
        return replace(message, client.getUser().getName(), this.replacement.value());
    }

    @Override
    public Component tabPlayerName(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        if (!this.tab.value()) {
            return name;
        }
        return replaceLocalName(client, name, this.replacement.value());
    }

    static Component replaceLocalName(final Minecraft client, final Component message, final String replacement) {
        if (client == null || client.getUser() == null || message == null) {
            return message;
        }
        String replaced = replace(message.getString(), client.getUser().getName(), replacement);
        if (replaced.equals(message.getString())) {
            return message;
        }
        return Component.literal(replaced).withStyle(message.getStyle());
    }

    static String replace(final String value, final String name, final String replacement) {
        if (value == null || name == null || name.isBlank()) {
            return value;
        }
        return value.replace(name, replacement == null || replacement.isBlank() ? "You" : replacement);
    }
}
