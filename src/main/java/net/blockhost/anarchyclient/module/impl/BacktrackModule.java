package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.network.PacketQueueManager;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.player.Player;

public final class BacktrackModule extends Module {

    private static final int NO_TARGET = Integer.MIN_VALUE;

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(120.0)
            .min(0.0)
            .max(750.0)
            .step(10.0)
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(0.25)
            .build()));
    private final NumberSetting maxPackets = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_packets")
            .name("Packets")
            .defaultValue(80.0)
            .min(1.0)
            .max(512.0)
            .step(1.0)
            .build()));
    private final BooleanSetting esp = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("esp").name("ESP").defaultValue(true).build()));

    /** Entity id of the local player while an enemy is in range, or {@code NO_TARGET}. Read on the Netty thread. */
    private volatile int activeSelfId = NO_TARGET;

    public BacktrackModule() {
        super(
                "backtrack",
                "Backtrack",
                ModuleCategory.COMBAT,
                "Delays entity movement updates while an enemy is in range, so you can still hit where they were a moment ago.");
    }

    @Override
    public void tick(final Minecraft client) {
        Player target = client.player == null || client.hasSingleplayerServer()
                ? null
                : CombatTargets.nearestEnemy(client, this.range.value());
        this.activeSelfId = target == null ? NO_TARGET : client.player.getId();
        int flushed = target == null
                ? PacketQueueManager.flushIncoming(this.id())
                : PacketQueueManager.flushIncomingOlderThan(
                        this.id(), this.delay.value().longValue());
        this.debugValue("queued", PacketQueueManager.incomingSize(this.id()));
        if (flushed > 0) {
            this.debugValue("flushed", flushed);
        }
        if (this.esp.value() && target != null) {
            MarkerManager.put(
                    new CuboidMarker("backtrack:" + target.getUUID(), target.getBoundingBox(), MarkerStyle.CYAN, 4));
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        int selfId = this.activeSelfId;
        if (selfId == NO_TARGET || !isEntityMovementPacket(packet) || isAbout(packet, selfId)) {
            return false;
        }
        return PacketQueueManager.queueIncoming(
                this.id(), connection, packet, this.maxPackets.value().intValue());
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        this.activeSelfId = NO_TARGET;
        PacketQueueManager.dropIncoming(this.id());
    }

    @Override
    protected void onDisable() {
        this.activeSelfId = NO_TARGET;
        PacketQueueManager.flushIncoming(this.id());
        this.clearDebugValues();
    }

    static boolean isEntityMovementPacket(final Packet<?> packet) {
        return packet instanceof ClientboundMoveEntityPacket
                || packet instanceof ClientboundTeleportEntityPacket
                || packet instanceof ClientboundEntityPositionSyncPacket
                || packet instanceof ClientboundSetEntityMotionPacket;
    }

    /** Returns whether the packet moves the given entity. Relative move packets never target the receiver. */
    private static boolean isAbout(final Packet<?> packet, final int entityId) {
        return switch (packet) {
            case ClientboundTeleportEntityPacket teleport -> teleport.id() == entityId;
            case ClientboundEntityPositionSyncPacket sync -> sync.id() == entityId;
            case ClientboundSetEntityMotionPacket motion -> motion.id() == entityId;
            default -> false;
        };
    }
}
