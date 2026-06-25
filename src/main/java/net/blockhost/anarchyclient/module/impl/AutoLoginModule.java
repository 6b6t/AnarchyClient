package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ServerData;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class AutoLoginModule extends Module {

    private final StringSetting commands = this.setting(StringSetting.from(StringSetting.builder()
            .id("commands")
            .name("Commands")
            .defaultValue("")
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(40.0)
            .min(0.0)
            .max(400.0)
            .step(5.0)
            .build()));
    private String pendingCommand;
    private int pendingTicks;

    public AutoLoginModule() {
        super("auto_login", "Auto Login", ModuleCategory.MISC);
    }

    @Override
    public void gameJoined(final Minecraft client, final ClientPacketListener listener) {
        this.pendingCommand = this.commandFor(client);
        this.pendingTicks = this.pendingCommand == null ? 0 : this.delay.value().intValue();
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.clearPending();
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.pendingCommand == null) {
            return;
        }
        if (this.pendingTicks > 0) {
            this.pendingTicks--;
            return;
        }
        ChatActions.send(client, this.pendingCommand);
        this.clearPending();
    }

    @Override
    protected void onDisable() {
        this.clearPending();
    }

    private String commandFor(final Minecraft client) {
        Map<String, String> entries = parseCommands(this.commands.value());
        if (entries.isEmpty()) {
            return null;
        }
        ServerIdentity server = serverIdentity(client);
        String command = entries.get(server.address());
        if (command == null) {
            command = entries.get(server.name());
        }
        if (command == null && server.local()) {
            command = entries.get("local");
        }
        if (command == null) {
            command = entries.get("*");
        }
        return command == null || command.isBlank() ? null : command;
    }

    private void clearPending() {
        this.pendingCommand = null;
        this.pendingTicks = 0;
    }

    static Map<String, String> parseCommands(final String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        Map<String, String> entries = new LinkedHashMap<>();
        for (String entry : value.split("[;\\n]+")) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                continue;
            }
            String key = entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String command = entry.substring(separator + 1).trim();
            if (!key.isEmpty() && !command.isEmpty()) {
                entries.put(key, command);
            }
        }
        return Map.copyOf(entries);
    }

    private static ServerIdentity serverIdentity(final Minecraft client) {
        ServerData server = client.getCurrentServer();
        if (server != null) {
            return new ServerIdentity(
                    normalize(server.ip),
                    normalize(server.name),
                    false
            );
        }
        return new ServerIdentity("local", "local", client.isLocalServer() || client.hasSingleplayerServer());
    }

    private static String normalize(final String value) {
        return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ServerIdentity(String address, String name, boolean local) {
    }
}
