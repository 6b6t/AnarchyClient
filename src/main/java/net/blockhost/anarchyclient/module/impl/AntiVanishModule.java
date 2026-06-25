package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class AntiVanishModule extends Module {

    private final NumberSetting intervalTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("interval_ticks")
            .name("Interval")
            .defaultValue(100.0)
            .min(20.0)
            .max(600.0)
            .step(10.0)
            .build()));
    private final BooleanSetting requireLeaveMessage = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_leave_message")
            .name("Leave Msg")
            .defaultValue(true)
            .build()));
    private Map<UUID, String> playerCache = Map.of();
    private final List<String> messageCache = new ArrayList<>();
    private int timer;

    public AntiVanishModule() {
        super("anti_vanish", "Anti Vanish", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.reset();
    }

    @Override
    protected void onDisable() {
        this.reset();
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (client.player == null || listener == null) {
            this.reset();
            return;
        }
        if (this.playerCache.isEmpty()) {
            this.playerCache = onlinePlayers(listener);
            return;
        }
        this.timer++;
        if (this.timer < this.intervalTicks.value()) {
            return;
        }
        Map<UUID, String> current = onlinePlayers(listener);
        for (String name : vanishedPlayers(this.playerCache, current, this.messageCache, this.requireLeaveMessage.value())) {
            client.player.sendSystemMessage(Component.literal(name + " may have vanished."));
        }
        this.playerCache = current;
        this.messageCache.clear();
        this.timer = 0;
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientboundSystemChatPacket systemChat) {
            String message = systemChat.content().getString();
            if (!message.isBlank()) {
                this.messageCache.add(message);
            }
        }
        return false;
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
        this.playerCache = onlinePlayers(listener);
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.reset();
    }

    static List<String> vanishedPlayers(final Map<UUID, String> previous, final Map<UUID, String> current,
                                        final List<String> messages, final boolean requireMissingLeaveMessage) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<UUID, String> entry : previous.entrySet()) {
            if (current.containsKey(entry.getKey()) || !validPlayerName(entry.getValue())) {
                continue;
            }
            if (!requireMissingLeaveMessage || messages.stream().noneMatch(message -> mentionsPlayer(message, entry.getValue()))) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    private static Map<UUID, String> onlinePlayers(final ClientPacketListener listener) {
        Map<UUID, String> players = new LinkedHashMap<>();
        for (PlayerInfo info : listener.getOnlinePlayers()) {
            players.put(info.getProfile().id(), info.getProfile().name());
        }
        return players;
    }

    private static boolean mentionsPlayer(final String message, final String name) {
        return message.toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT));
    }

    private static boolean validPlayerName(final String name) {
        if (name == null || name.length() < 3 || name.length() > 16) {
            return false;
        }
        for (int index = 0; index < name.length(); index++) {
            char character = name.charAt(index);
            if (!Character.isLetterOrDigit(character) && character != '_') {
                return false;
            }
        }
        return true;
    }

    private void reset() {
        this.playerCache = Map.of();
        this.messageCache.clear();
        this.timer = 0;
    }
}
