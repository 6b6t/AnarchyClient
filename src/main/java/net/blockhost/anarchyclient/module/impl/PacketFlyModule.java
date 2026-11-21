package net.blockhost.anarchyclient.module.impl;

import java.util.List;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

public final class PacketFlyModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Normal")
            .addAllOptions(List.of("Normal", "Clip", "Setback"))
            .build()));
    private final NumberSetting speed = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("speed")
            .name("Speed")
            .defaultValue(0.25)
            .min(0.02)
            .max(1.5)
            .step(0.01)
            .build()));
    private final NumberSetting vertical = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical")
            .name("Vertical")
            .defaultValue(0.18)
            .min(0.02)
            .max(1.5)
            .step(0.01)
            .build()));
    private final BooleanSetting noClip = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("noclip")
            .name("No Clip")
            .defaultValue(true)
            .build()));
    private final NumberSetting antiKickTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("anti_kick_ticks")
            .name("Anti Kick")
            .defaultValue(12.0)
            .min(0.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private final NumberSetting bounds = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("bounds")
            .name("Bounds")
            .defaultValue(1337.0)
            .min(10.0)
            .max(4096.0)
            .step(1.0)
            .build()));
    private final BooleanSetting sprint = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sprint")
            .name("Sprint")
            .defaultValue(false)
            .build()));
    private int ticks;
    private boolean sendingOwn;

    public PacketFlyModule() {
        super(
                "packet_fly",
                "Packet Fly",
                ModuleCategory.MOVEMENT,
                "Uses packet movement to handle flight-like movement on servers where normal movement is restricted.");
    }

    @Override
    public void tick(final Minecraft client) {
        ClientPacketListener connection = client.getConnection();
        LocalPlayer player = client.player;
        if (player == null || connection == null) {
            return;
        }
        if (this.noClip.value()) {
            player.noPhysics = true;
        }
        if (this.sprint.value()) {
            player.setSprinting(true);
        }
        Vec3 horizontal = MovementVelocity.fromKeys(client, player.getYRot(), this.speed.value());
        double y = client.options.keyJump.isDown()
                ? this.vertical.value()
                : client.options.keyShift.isDown() ? -this.vertical.value() : 0.0;
        int antiKick = this.antiKickTicks.value().intValue();
        this.ticks++;
        if (y == 0.0 && antiKick > 0 && this.ticks % antiKick == 0) {
            y = -0.04;
        }
        Vec3 next = player.position().add(horizontal.x, y, horizontal.z);
        player.setDeltaMovement(horizontal.x, y, horizontal.z);
        if ("Clip".equals(this.mode.value())) {
            player.setPos(next.x, next.y, next.z);
        }
        // A 26.3 server kicks a client that sends two position packets in one tick, so this sends one
        // per tick and alternates between the move and the follow-up packet the old version sent with it.
        boolean followUp = this.ticks % 2 != 0;
        ServerboundMovePlayerPacket packet;
        if (!followUp) {
            packet = new ServerboundMovePlayerPacket.Pos(next, false, false);
        } else if ("Setback".equals(this.mode.value())) {
            packet = new ServerboundMovePlayerPacket.Pos(player.position(), true, player.horizontalCollision);
        } else {
            packet = new ServerboundMovePlayerPacket.Pos(
                    next.x, next.y - this.bounds.value(), next.z, true, player.horizontalCollision);
        }
        this.sendingOwn = true;
        try {
            connection.send(packet);
        } finally {
            this.sendingOwn = false;
        }
        this.debugValue("mode", this.mode.value());
    }

    /** Keeps only the rotation of vanilla's movement packet, so the tick still has one position packet. */
    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.sendingOwn
                || !(packet instanceof ServerboundMovePlayerPacket move)
                || !move.hasPosition()
                || !move.hasRotation()) {
            return packet;
        }
        return new ServerboundMovePlayerPacket.Rot(
                move.getYRot(0.0F), move.getXRot(0.0F), move.isOnGround(), move.horizontalCollision());
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return !this.sendingOwn && packet instanceof ServerboundMovePlayerPacket move && move.hasPosition();
    }

    @Override
    protected void onDisable() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.noPhysics = false;
        }
        this.ticks = 0;
    }
}
