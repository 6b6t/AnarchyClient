package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

public final class DebugRecorderModule extends Module {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.ROOT);

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
    private final BooleanSetting autoSaveOnFlag = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("auto_save_flag")
            .name("Save Flag")
            .defaultValue(true)
            .build()));
    private final NumberSetting maxRecords = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_records")
            .name("Records")
            .defaultValue(400.0)
            .min(50.0)
            .max(5000.0)
            .step(50.0)
            .build()));
    private final Deque<String> records = new ArrayDeque<>();
    private int observedFlagSequence;

    public DebugRecorderModule() {
        super("debug_recorder", "Debug Recorder", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        this.records.clear();
        this.observedFlagSequence = ServerObserver.flagSequence();
    }

    @Override
    protected void onDisable() {
        this.save(null, "manual");
        this.records.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        int sequence = ServerObserver.flagSequence();
        if (this.autoSaveOnFlag.value() && sequence != this.observedFlagSequence) {
            this.observedFlagSequence = sequence;
            this.record("FLAG " + ServerObserver.lastFlag());
            this.save(client, "flag");
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.incoming.value()) {
            this.record("IN " + PacketLoggerModule.packetName(packet));
        }
        return false;
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.outgoing.value()) {
            this.record("OUT " + PacketLoggerModule.packetName(packet));
        }
        return false;
    }

    private void record(final String line) {
        this.records.addLast(System.currentTimeMillis() + " " + line);
        int max = this.maxRecords.value().intValue();
        while (this.records.size() > max) {
            this.records.removeFirst();
        }
    }

    private void save(final Minecraft client, final String reason) {
        if (this.records.isEmpty()) {
            return;
        }
        Path dir = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-debug");
        Path file = dir.resolve(FILE_TIME.format(LocalDateTime.now()) + "-" + reason + ".log");
        try {
            Files.createDirectories(dir);
            Files.write(file, new ArrayList<>(this.records), StandardCharsets.UTF_8);
            if (client != null && client.player != null) {
                client.player.sendSystemMessage(Component.literal("Debug log saved to " + file + "."));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save debug recorder log", exception);
        }
    }
}
