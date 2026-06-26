package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;

public final class NoFallModule extends Module {

    private final NumberSetting minFallDistance = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_fall_distance")
            .name("Fall Dist")
            .defaultValue(3.0)
            .min(2.0)
            .max(10.0)
            .step(0.5)
            .build()));
    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Packet")
            .addAllOptions(List.of("Packet", "Spoof Ground", "No Ground", "Cancel Motion", "MLG Assist"))
            .build()));

    public NoFallModule() {
        super("no_fall", "No Fall", ModuleCategory.MOVEMENT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            return;
        }
        if (!shouldHandle(player, this.minFallDistance.value())) {
            return;
        }
        switch (this.mode.value()) {
            case "Cancel Motion" -> {
                if (player.getDeltaMovement().y < 0.0) {
                    player.setDeltaMovement(player.getDeltaMovement().x, 0.0, player.getDeltaMovement().z);
                }
                player.resetFallDistance();
            }
            case "Spoof Ground" -> {
                player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                player.resetFallDistance();
            }
            case "MLG Assist" -> {
                if (player.getDeltaMovement().y < -0.6) {
                    player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
                }
            }
            default -> player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, player.horizontalCollision));
        }
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ServerboundMovePlayerPacket move)
                || client.player == null
                || !shouldHandle(client.player, this.minFallDistance.value())) {
            return packet;
        }
        if ("Spoof Ground".equals(this.mode.value())) {
            return withGround(move, client.player, true);
        }
        if ("No Ground".equals(this.mode.value())) {
            return withGround(move, client.player, false);
        }
        return packet;
    }

    static boolean shouldHandle(final LocalPlayer player, final double minFallDistance) {
        return player != null
                && player.connection != null
                && player.fallDistance >= minFallDistance
                && !player.onGround()
                && !player.isFallFlying()
                && !player.isPassenger()
                && !player.isInWater();
    }

    private static Packet<?> withGround(final ServerboundMovePlayerPacket packet, final LocalPlayer player,
                                        final boolean onGround) {
        boolean horizontalCollision = packet.horizontalCollision();
        if (packet instanceof ServerboundMovePlayerPacket.PosRot) {
            return new ServerboundMovePlayerPacket.PosRot(
                    packet.getX(player.getX()),
                    packet.getY(player.getY()),
                    packet.getZ(player.getZ()),
                    packet.getYRot(player.getYRot()),
                    packet.getXRot(player.getXRot()),
                    onGround,
                    horizontalCollision
            );
        }
        if (packet instanceof ServerboundMovePlayerPacket.Pos) {
            return new ServerboundMovePlayerPacket.Pos(
                    packet.getX(player.getX()),
                    packet.getY(player.getY()),
                    packet.getZ(player.getZ()),
                    onGround,
                    horizontalCollision
            );
        }
        if (packet instanceof ServerboundMovePlayerPacket.Rot) {
            return new ServerboundMovePlayerPacket.Rot(
                    packet.getYRot(player.getYRot()),
                    packet.getXRot(player.getXRot()),
                    onGround,
                    horizontalCollision
            );
        }
        return new ServerboundMovePlayerPacket.StatusOnly(onGround, horizontalCollision);
    }
}
