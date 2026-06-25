package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.friends.FriendManager;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

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
    private final BooleanSetting friends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("friends")
            .name("Friends")
            .defaultValue(false)
            .build()));
    private final StringSetting names = this.setting(StringSetting.from(StringSetting.builder()
            .id("names")
            .name("Names")
            .defaultValue("")
            .description("Extra names to protect, separated by commas or spaces.")
            .build()));

    public NameProtectModule() {
        super("name_protect", "Name Protect", ModuleCategory.MISC);
    }

    @Override
    public Component chatMessage(final Minecraft client, final Component message) {
        if (!this.incoming.value()) {
            return message;
        }
        return replaceProtectedNames(client, message, this.replacement.value(), this.friends.value(), this.names.value());
    }

    @Override
    public String sendChatMessage(final Minecraft client, final String message, final boolean command) {
        if (!this.outgoing.value() || client == null || client.getUser() == null || message == null) {
            return message;
        }
        return replaceAll(message, protectedNames(client, this.friends.value(), this.names.value()), this.replacement.value());
    }

    @Override
    public Component tabPlayerName(final Minecraft client, final PlayerInfo playerInfo, final Component name) {
        if (!this.tab.value()) {
            return name;
        }
        return replaceProtectedNames(client, name, this.replacement.value(), this.friends.value(), this.names.value());
    }

    static Component replaceLocalName(final Minecraft client, final Component message, final String replacement) {
        return replaceProtectedNames(client, message, replacement, false, "");
    }

    static Component replaceProtectedNames(final Minecraft client, final Component message, final String replacement,
                                           final boolean includeFriends, final String extraNames) {
        if (client == null || client.getUser() == null || message == null) {
            return message;
        }
        String replaced = replaceAll(message.getString(), protectedNames(client, includeFriends, extraNames), replacement);
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

    static String replaceAll(final String value, final List<String> names, final String replacement) {
        if (value == null || names.isEmpty()) {
            return value;
        }
        String result = value;
        for (String name : names) {
            result = replace(result, name, replacement);
        }
        return result;
    }

    private static List<String> protectedNames(final Minecraft client, final boolean includeFriends,
                                               final String extraNames) {
        List<String> names = new ArrayList<>();
        if (client != null && client.getUser() != null && !client.getUser().getName().isBlank()) {
            names.add(client.getUser().getName());
        }
        if (includeFriends) {
            names.addAll(AnarchyClient.FRIENDS.friends());
        }
        names.addAll(FriendManager.parseNames(extraNames));
        return List.copyOf(names);
    }
}
