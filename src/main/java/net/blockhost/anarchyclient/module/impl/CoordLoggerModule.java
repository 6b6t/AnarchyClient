package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class CoordLoggerModule extends Module {

    private static final int WITHER_SPAWN_EVENT = 1023;
    private static final int DRAGON_DEATH_EVENT = 1028;
    private static final int END_PORTAL_OPEN_EVENT = 1038;

    private final NumberSetting minDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_distance")
            .name("Distance")
            .defaultValue(10.0)
            .min(0.0)
            .max(200.0)
            .step(1.0)
            .build()));
    private final BooleanSetting players = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("players")
            .name("Players")
            .defaultValue(true)
            .build()));
    private final BooleanSetting wolves = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("wolves")
            .name("Wolves")
            .defaultValue(false)
            .build()));
    private final BooleanSetting withers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("withers")
            .name("Withers")
            .defaultValue(true)
            .build()));
    private final BooleanSetting endPortals = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("end_portals")
            .name("Portals")
            .defaultValue(true)
            .build()));
    private final BooleanSetting dragonDeaths = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("dragon_deaths")
            .name("Dragons")
            .defaultValue(false)
            .build()));
    private final BooleanSetting otherGlobalEvents = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("other_global_events")
            .name("Other")
            .defaultValue(false)
            .build()));

    /** Global level events are sent from at most this far away; farther events only reveal a direction. */
    private static final double GLOBAL_EVENT_EXACT_RANGE = 30.0;

    /** Positions of watched entities, published each tick so the Netty thread can read them safely. */
    private volatile Map<Integer, Tracked> tracked = Map.of();

    public CoordLoggerModule() {
        super("coord_logger", "Coord Logger", ModuleCategory.MISC,
                "Logs distant player and tamed wolf teleports to chat. Global events like wither spawns show their"
                        + " direction, because the server only sends the exact position within 32 blocks.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level == null || client.player == null) {
            this.tracked = Map.of();
            return;
        }
        Map<Integer, Tracked> snapshot = new HashMap<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player) {
                continue;
            }
            if (entity.getType() == EntityTypes.PLAYER && this.players.value()) {
                snapshot.put(entity.getId(), new Tracked("Player " + entity.getScoreboardName(), entity.position()));
            } else if (entity.getType() == EntityTypes.WOLF && entity instanceof TamableAnimal tamable
                    && tamable.isTame() && this.wolves.value()) {
                snapshot.put(entity.getId(), new Tracked("Wolf", entity.position()));
            }
        }
        this.tracked = Map.copyOf(snapshot);
    }

    @Override
    protected void onDisable() {
        this.tracked = Map.of();
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        switch (packet) {
            case ClientboundTeleportEntityPacket teleport -> this.logTeleport(client, teleport.id(),
                    teleportTarget(this.tracked.get(teleport.id()), teleport.change().position(), teleport.relatives()));
            case ClientboundEntityPositionSyncPacket sync -> this.logTeleport(client, sync.id(),
                    sync.position().endPosition());
            case ClientboundLevelEventPacket levelEvent -> MainThread.run(() -> this.logLevelEvent(client, levelEvent));
            default -> {
            }
        }
        return false;
    }

    private void logTeleport(final Minecraft client, final int entityId, final Vec3 target) {
        Tracked entity = this.tracked.get(entityId);
        if (entity == null || target == null || entity.position().distanceTo(target) < this.minDistance.value()) {
            return;
        }
        String message = entity.label() + " teleported to " + formatPosition(target) + ".";
        MainThread.run(() -> send(client, message));
    }

    private void logLevelEvent(final Minecraft client, final ClientboundLevelEventPacket packet) {
        if (!this.enabled() || client.player == null || !packet.isGlobalEvent()) {
            return;
        }
        boolean enabled = switch (packet.getType()) {
            case WITHER_SPAWN_EVENT -> this.withers.value();
            case END_PORTAL_OPEN_EVENT -> this.endPortals.value();
            case DRAGON_DEATH_EVENT -> this.dragonDeaths.value();
            default -> this.otherGlobalEvents.value();
        };
        Vec3 eventPos = Vec3.atCenterOf(packet.getPos());
        if (!enabled || client.player.position().distanceTo(eventPos) <= this.minDistance.value()) {
            return;
        }
        send(client, worldEventName(packet.getType()) + " " + describeGlobalEvent(client.player.position(), eventPos) + ".");
    }

    static Vec3 teleportTarget(final Tracked entity, final Vec3 change, final Set<Relative> relatives) {
        if (entity == null) {
            return null;
        }
        Vec3 base = entity.position();
        return new Vec3(
                relatives.contains(Relative.X) ? base.x + change.x : change.x,
                relatives.contains(Relative.Y) ? base.y + change.y : change.y,
                relatives.contains(Relative.Z) ? base.z + change.z : change.z
        );
    }

    /**
     * Describes where a global event happened. The server sends the real position only when the
     * event is within 32 blocks; otherwise it sends a point 32 blocks away in the event's direction.
     */
    static String describeGlobalEvent(final Vec3 playerPos, final Vec3 eventPos) {
        double dx = eventPos.x - playerPos.x;
        double dz = eventPos.z - playerPos.z;
        if (playerPos.distanceTo(eventPos) < GLOBAL_EVENT_EXACT_RANGE) {
            return "at " + formatPosition(eventPos);
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return String.format(Locale.ROOT, "more than 32 blocks away toward yaw %.1f (%s) from %s",
                yaw, cardinal(yaw), formatPosition(playerPos));
    }

    static String cardinal(final float yaw) {
        float wrapped = ((yaw % 360.0F) + 360.0F) % 360.0F;
        String[] names = {"south", "south-west", "west", "north-west", "north", "north-east", "east", "south-east"};
        return names[Math.round(wrapped / 45.0F) % names.length];
    }

    static String worldEventName(final int eventType) {
        return switch (eventType) {
            case WITHER_SPAWN_EVENT -> "Wither spawned";
            case END_PORTAL_OPEN_EVENT -> "End portal opened";
            case DRAGON_DEATH_EVENT -> "Ender dragon killed";
            default -> "Global event " + eventType;
        };
    }

    static String formatPosition(final BlockPos pos) {
        return formatPosition(new Vec3(pos.getX(), pos.getY(), pos.getZ()));
    }

    static String formatPosition(final Vec3 pos) {
        return String.format(Locale.ROOT, "%.1f, %.1f, %.1f", pos.x, pos.y, pos.z);
    }

    private static void send(final Minecraft client, final String message) {
        if (client.player != null) {
            client.player.sendSystemMessage(Component.literal(message));
        }
    }

    record Tracked(String label, Vec3 position) {
    }
}
