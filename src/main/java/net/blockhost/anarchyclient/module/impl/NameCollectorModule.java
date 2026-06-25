package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class NameCollectorModule extends Module {

    private final Set<String> names = new LinkedHashSet<>();

    public NameCollectorModule() {
        super("name_collector", "Name Collector", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (listener == null) {
            return;
        }
        for (PlayerInfo info : listener.getOnlinePlayers()) {
            this.names.add(info.getProfile().name());
        }
    }

    @Override
    protected void onDisable() {
        this.save(Minecraft.getInstance());
        this.names.clear();
    }

    private void save(final Minecraft client) {
        if (this.names.isEmpty()) {
            return;
        }
        String domain = ServerObserver.snapshot().rootDomain();
        String fileName = (domain.isBlank() ? "local" : domain).replaceAll("[^a-zA-Z0-9._-]", "_") + ".txt";
        Path file = FabricLoader.getInstance().getConfigDir().resolve(AnarchyClient.MOD_ID + "-name-captures").resolve(fileName);
        try {
            Files.createDirectories(file.getParent());
            Files.write(file, this.names.stream().sorted(Comparator.naturalOrder()).toList(), StandardCharsets.UTF_8);
            if (client.player != null) {
                client.player.sendSystemMessage(Component.literal("Saved " + this.names.size() + " names to " + file + "."));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save collected names", exception);
        }
    }
}
