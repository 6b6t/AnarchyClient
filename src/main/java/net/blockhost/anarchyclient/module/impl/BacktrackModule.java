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
import net.minecraft.world.entity.player.Player;

public final class BacktrackModule extends Module {

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
    private final BooleanSetting esp = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("esp")
            .name("ESP")
            .defaultValue(true)
            .build()));

    public BacktrackModule() {
        super("backtrack", "Backtrack", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        int flushed = PacketQueueManager.flushIncomingOlderThan(this.delay.value().longValue());
        this.debugValue("queued", PacketQueueManager.incomingSize());
        if (flushed > 0) {
            this.debugValue("flushed", flushed);
        }
        if (this.esp.value() && client.player != null) {
            Player target = CombatTargets.nearestEnemy(client, this.range.value());
            if (target != null) {
                MarkerManager.put(new CuboidMarker("backtrack:" + target.getUUID(), target.getBoundingBox(),
                        MarkerStyle.CYAN, 4));
            }
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.hasSingleplayerServer()
                || client.player == null
                || CombatTargets.nearestEnemy(client, this.range.value()) == null
                || !isEntityMovementPacket(packet)) {
            return false;
        }
        return PacketQueueManager.queueIncoming(connection, packet, this.maxPackets.value().intValue());
    }

    @Override
    public void gameLeft(final Minecraft client, final ClientPacketListener listener) {
        PacketQueueManager.dropIncoming();
    }

    @Override
    protected void onDisable() {
        PacketQueueManager.flushIncoming();
        this.clearDebugValues();
    }

    static boolean isEntityMovementPacket(final Packet<?> packet) {
        if (packet == null) {
            return false;
        }
        String name = packet.getClass().getSimpleName();
        return name.contains("MoveEntity")
                || name.contains("TeleportEntity")
                || name.contains("EntityPosition")
                || name.contains("SetEntityMotion");
    }
}
