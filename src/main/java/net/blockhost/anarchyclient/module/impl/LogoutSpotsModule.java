package net.blockhost.anarchyclient.module.impl;

import com.mojang.blaze3d.vertex.PoseStack;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class LogoutSpotsModule extends Module {

    private final NumberSetting lifetimeSeconds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("lifetime_seconds")
            .name("Lifetime")
            .defaultValue(180.0)
            .min(10.0)
            .max(1800.0)
            .step(10.0)
            .build()));
    private final NumberSetting maxSpots = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_spots")
            .name("Max")
            .defaultValue(32.0)
            .min(4.0)
            .max(128.0)
            .step(4.0)
            .build()));
    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(150.0)
            .min(30.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final BooleanSetting ignoreFriends = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ignore_friends")
            .name("Friends")
            .defaultValue(true)
            .build()));
    private Map<UUID, LogoutSpot> lastSeen = Map.of();
    private final Map<UUID, LogoutSpot> spots = new LinkedHashMap<>();

    public LogoutSpotsModule() {
        super("logout_spots", "Logout Spots", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            this.clear();
            return;
        }
        this.ageSpots(this.lifetimeSeconds.value().intValue() * 20);
        this.lastSeen = snapshotPlayers(client, player, this.ignoreFriends.value());
        this.lastSeen.keySet().forEach(this.spots::remove);
        this.trimSpots(this.maxSpots.value().intValue());
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoRemovePacket remove) || client.player == null) {
            return false;
        }
        for (UUID id : remove.profileIds()) {
            LogoutSpot spot = this.lastSeen.get(id);
            if (spot == null || id.equals(client.player.getUUID())
                    || this.ignoreFriends.value() && AnarchyClient.FRIENDS.isFriend(spot.name())) {
                continue;
            }
            this.spots.put(id, spot.withAge(0));
        }
        this.trimSpots(this.maxSpots.value().intValue());
        return false;
    }

    @Override
    public void renderWorld(final LevelRenderContext context) {
        Minecraft client = Minecraft.getInstance();
        PoseStack matrices = context.poseStack();
        SubmitNodeCollector submits = context.submitNodeCollector();
        if (client.player == null || matrices == null || submits == null || this.spots.isEmpty()) {
            return;
        }
        Vec3 camera = client.gameRenderer.mainCamera().position();
        int alpha = this.opacity.value().intValue();
        WorldLineRenderer.Color fill = new WorldLineRenderer.Color(255, 70, 90, Math.max(20, alpha / 3));
        WorldLineRenderer.Color outline = new WorldLineRenderer.Color(255, 95, 120, alpha);
        for (LogoutSpot spot : this.spots.values()) {
            WorldLineRenderer.fillNoDepth(matrices, submits, spot.box().move(camera.scale(-1)), fill);
            WorldLineRenderer.boxNoDepth(matrices, submits, spot.box().move(camera.scale(-1)), outline);
        }
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.clear();
    }

    @Override
    protected void onDisable() {
        this.clear();
    }

    static String formatLabel(final String name, final float health) {
        return name + " " + String.format(Locale.ROOT, "%.1f", health) + " HP";
    }

    static boolean expired(final int ageTicks, final int lifetimeTicks) {
        return lifetimeTicks >= 0 && ageTicks > lifetimeTicks;
    }

    private static Map<UUID, LogoutSpot> snapshotPlayers(final Minecraft client, final Player self,
                                                         final boolean ignoreFriends) {
        Map<UUID, LogoutSpot> players = new LinkedHashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player player) || player == self || player.isSpectator() || !player.isAlive()
                    || ignoreFriends && AnarchyClient.FRIENDS.isFriend(player.getScoreboardName())) {
                continue;
            }
            players.put(player.getUUID(), capture(player));
        }
        return Map.copyOf(players);
    }

    private static LogoutSpot capture(final Player player) {
        return new LogoutSpot(
                player.getUUID(),
                player.getScoreboardName(),
                player.position(),
                player.getBoundingBox(),
                player.getHealth() + player.getAbsorptionAmount(),
                0
        );
    }

    private void ageSpots(final int lifetimeTicks) {
        this.spots.replaceAll((id, spot) -> spot.withAge(spot.ageTicks() + 1));
        this.spots.entrySet().removeIf(entry -> expired(entry.getValue().ageTicks(), lifetimeTicks));
    }

    private void trimSpots(final int max) {
        while (this.spots.size() > max) {
            UUID oldest = this.spots.entrySet().stream()
                    .max(Comparator.comparingInt(entry -> entry.getValue().ageTicks()))
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (oldest == null) {
                return;
            }
            this.spots.remove(oldest);
        }
    }

    private void clear() {
        this.lastSeen = Map.of();
        this.spots.clear();
    }

    record LogoutSpot(UUID id, String name, Vec3 position, AABB box, float health, int ageTicks) {

        LogoutSpot withAge(final int ageTicks) {
            return new LogoutSpot(this.id, this.name, this.position, this.box, this.health, ageTicks);
        }
    }
}
