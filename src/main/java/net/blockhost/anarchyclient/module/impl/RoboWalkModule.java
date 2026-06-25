package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.world.phys.Vec3;

public final class RoboWalkModule extends Module {

    private final NumberSetting grid = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("grid")
            .name("Grid")
            .defaultValue(0.125)
            .min(0.01)
            .max(1.0)
            .step(0.005)
            .build()));

    public RoboWalkModule() {
        super("robo_walk", "Robo Walk", ModuleCategory.MOVEMENT);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (client.player == null) {
            return packet;
        }
        double gridValue = this.grid.value();
        if (packet instanceof ServerboundMovePlayerPacket move && move.hasPosition()) {
            double x = snap(move.getX(client.player.getX()), gridValue);
            double y = move.getY(client.player.getY());
            double z = snap(move.getZ(client.player.getZ()), gridValue);
            if (move.hasRotation()) {
                return new ServerboundMovePlayerPacket.PosRot(x, y, z,
                        move.getYRot(client.player.getYRot()),
                        move.getXRot(client.player.getXRot()),
                        move.isOnGround(),
                        move.horizontalCollision());
            }
            return new ServerboundMovePlayerPacket.Pos(x, y, z, move.isOnGround(), move.horizontalCollision());
        }
        if (packet instanceof ServerboundMoveVehiclePacket vehicle) {
            Vec3 pos = vehicle.position();
            return new ServerboundMoveVehiclePacket(
                    new Vec3(snap(pos.x, gridValue), pos.y, snap(pos.z, gridValue)),
                    vehicle.yRot(),
                    vehicle.xRot(),
                    vehicle.onGround()
            );
        }
        return packet;
    }

    static double snap(final double value, final double grid) {
        if (grid <= 0.0) {
            return value;
        }
        return Math.round(value / grid) * grid;
    }
}
