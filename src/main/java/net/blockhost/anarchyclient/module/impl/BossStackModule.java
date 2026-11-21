package net.blockhost.anarchyclient.module.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBossEventPacket;
import net.minecraft.world.BossEvent;

public final class BossStackModule extends HudElementModule {

    private final Map<UUID, BossInfo> bosses = new LinkedHashMap<>();

    public BossStackModule() {
        super(
                "boss_stack",
                "Boss Stack",
                "Lists boss bars received while enabled with their names and progress percentages.",
                "Top Right");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientboundBossEventPacket bossPacket) {
            // Packets arrive on the network thread while the HUD reads the map on the render thread.
            synchronized (this.bosses) {
                bossPacket.dispatch(new Handler());
            }
        }
        return false;
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.clear();
    }

    @Override
    protected void onDisable() {
        this.clear();
    }

    private void clear() {
        synchronized (this.bosses) {
            this.bosses.clear();
        }
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        synchronized (this.bosses) {
            return this.bosses.values().stream()
                    .map(info -> info.name() + " " + Math.round(info.progress() * 100.0F) + "%")
                    .toList();
        }
    }

    private final class Handler implements ClientboundBossEventPacket.Handler {

        @Override
        public void add(
                final UUID id,
                final Component name,
                final float progress,
                final BossEvent.BossBarColor color,
                final BossEvent.BossBarOverlay overlay,
                final boolean darkenScreen,
                final boolean playMusic,
                final boolean createWorldFog) {
            BossStackModule.this.bosses.put(id, new BossInfo(name.getString(), progress));
        }

        @Override
        public void remove(final UUID id) {
            BossStackModule.this.bosses.remove(id);
        }

        @Override
        public void updateProgress(final UUID id, final float progress) {
            BossInfo info = BossStackModule.this.bosses.get(id);
            if (info != null) {
                BossStackModule.this.bosses.put(id, new BossInfo(info.name(), progress));
            }
        }

        @Override
        public void updateName(final UUID id, final Component name) {
            BossInfo info = BossStackModule.this.bosses.get(id);
            if (info != null) {
                BossStackModule.this.bosses.put(id, new BossInfo(name.getString(), info.progress()));
            }
        }
    }

    private record BossInfo(String name, float progress) {}
}
