package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class AntiPacketKickModule extends Module {

    private final BooleanSetting invalidMovement = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invalid_movement")
            .name("Invalid Movement")
            .defaultValue(true)
            .build()));
    private final NumberSetting maxCoordinate = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_coordinate")
            .name("Max Coordinate")
            .defaultValue(3.0E7)
            .min(1024.0)
            .max(3.0E7)
            .step(1024.0)
            .build()));
    private final NumberSetting maxPacketsPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_packets_per_tick")
            .name("Packet Limit")
            .defaultValue(80.0)
            .min(5.0)
            .max(500.0)
            .step(5.0)
            .build()));
    private int movementPacketsThisTick;

    public AntiPacketKickModule() {
        super("anti_packet_kick", "Anti Packet Kick", ModuleCategory.MISC);
    }

    @Override
    public void tick(final Minecraft client) {
        this.movementPacketsThisTick = 0;
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket movement)) {
            return false;
        }
        this.movementPacketsThisTick++;
        if (this.movementPacketsThisTick > this.maxPacketsPerTick.value().intValue()) {
            return true;
        }
        return this.invalidMovement.value() && isInvalidMovement(movement, this.maxCoordinate.value());
    }

    static boolean isInvalidMovement(final ServerboundMovePlayerPacket packet, final double maxCoordinate) {
        if (packet.hasPosition()) {
            if (!finiteAndInside(packet.getX(0.0), maxCoordinate)
                    || !finiteAndInside(packet.getY(0.0), maxCoordinate)
                    || !finiteAndInside(packet.getZ(0.0), maxCoordinate)) {
                return true;
            }
        }
        if (packet.hasRotation()) {
            float yaw = packet.getYRot(0.0F);
            float pitch = packet.getXRot(0.0F);
            return !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(pitch) > 90.0F;
        }
        return false;
    }

    private static boolean finiteAndInside(final double value, final double maxCoordinate) {
        return Double.isFinite(value) && Math.abs(value) <= maxCoordinate;
    }
}
