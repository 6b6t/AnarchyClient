package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

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

    public CoordLoggerModule() {
        super("coord_logger", "Coord Logger", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.level == null || client.player == null) {
            return false;
        }
        if (packet instanceof ClientboundTeleportEntityPacket teleport) {
            this.logTeleport(client, teleport);
        } else if (packet instanceof ClientboundLevelEventPacket levelEvent) {
            this.logLevelEvent(client, levelEvent);
        }
        return false;
    }

    private void logTeleport(final Minecraft client, final ClientboundTeleportEntityPacket packet) {
        Entity entity = client.level.getEntity(packet.id());
        if (entity == null) {
            return;
        }
        Vec3 target = packet.change().position();
        if (entity.position().distanceTo(target) < this.minDistance.value()) {
            return;
        }
        if (entity.getType() == EntityTypes.PLAYER && this.players.value()) {
            send(client, "Player " + entity.getScoreboardName() + " teleported to " + formatPosition(target) + ".");
        } else if (entity.getType() == EntityTypes.WOLF && entity instanceof TamableAnimal tamable
                && tamable.isTame() && this.wolves.value()) {
            send(client, "Wolf teleported to " + formatPosition(target) + ".");
        }
    }

    private void logLevelEvent(final Minecraft client, final ClientboundLevelEventPacket packet) {
        if (!packet.isGlobalEvent() || client.player.blockPosition().distSqr(packet.getPos()) <= this.minDistance.value() * this.minDistance.value()) {
            return;
        }
        String name = worldEventName(packet.getType());
        boolean enabled = switch (packet.getType()) {
            case WITHER_SPAWN_EVENT -> this.withers.value();
            case END_PORTAL_OPEN_EVENT -> this.endPortals.value();
            case DRAGON_DEATH_EVENT -> this.dragonDeaths.value();
            default -> this.otherGlobalEvents.value();
        };
        if (enabled) {
            send(client, name + " at " + formatPosition(packet.getPos()) + ".");
        }
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
        client.player.sendSystemMessage(Component.literal(message));
    }
}
