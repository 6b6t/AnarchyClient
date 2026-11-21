package net.blockhost.anarchyclient.module.impl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.server.ServerObserver;
import net.blockhost.anarchyclient.util.MoreFiles;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class NameCollectorModule extends Module {

    private final Set<String> names = new LinkedHashSet<>();
    private String domain;

    public NameCollectorModule() {
        super(
                "name_collector",
                "Name Collector",
                ModuleCategory.MISC,
                "Collects the names of online players and adds them to a per-server file when you leave or turn it off.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener listener = client.getConnection();
        if (listener == null) {
            return;
        }
        String domain = currentDomain();
        if (!domain.equals(this.domain)) {
            // Keep each server's names in its own file instead of mixing them.
            this.save(client);
            this.domain = domain;
        }
        for (PlayerInfo info : listener.getOnlinePlayers()) {
            this.names.add(info.getProfile().name());
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.save(client);
    }

    @Override
    protected void onDisable() {
        this.save(Minecraft.getInstance());
    }

    private static String currentDomain() {
        String domain = ServerObserver.snapshot().rootDomain();
        return domain.isBlank() ? "local" : domain;
    }

    /** Merges the collected names into the server's file, so earlier sessions are kept. */
    private void save(final Minecraft client) {
        if (this.names.isEmpty() || this.domain == null) {
            this.names.clear();
            return;
        }
        String fileName = this.domain.replaceAll("[^a-zA-Z0-9._-]", "_") + ".txt";
        Path file = FabricLoader.getInstance()
                .getConfigDir()
                .resolve(AnarchyClient.MOD_ID + "-name-captures")
                .resolve(fileName);
        try {
            MoreFiles.createParentDirectories(file);
            Set<String> merged = new TreeSet<>(this.names);
            if (Files.exists(file)) {
                merged.addAll(Files.readAllLines(file, StandardCharsets.UTF_8).stream()
                        .map(String::strip)
                        .filter(line -> !line.isEmpty())
                        .toList());
            }
            Files.write(file, merged, StandardCharsets.UTF_8);
            if (client.player != null) {
                client.player.sendSystemMessage(Translations.component(
                        "anarchyclient.message.name_collector.saved_names_to",
                        "Saved %s names to %s.",
                        merged.size(),
                        file));
            }
        } catch (IOException exception) {
            AnarchyClient.LOGGER.warn("Failed to save collected names", exception);
        }
        this.names.clear();
    }
}
