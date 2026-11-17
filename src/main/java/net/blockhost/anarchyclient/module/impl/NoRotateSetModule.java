package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.util.MainThread;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerRotationPacket;
import net.minecraft.world.entity.Relative;

public final class NoRotateSetModule extends Module {

    private final BooleanSetting positionCorrections = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("position_corrections")
            .name("Position Packets")
            .defaultValue(false)
            .description("Keeps your view after server position corrections. The correction itself still applies.")
            .build()));

    public NoRotateSetModule() {
        super("no_rotate_set", "No Rotate Set", ModuleCategory.PLAYER,
                "Ignores server packets that force the camera rotation, and optionally the rotation in position corrections.");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        LocalPlayer player = client.player;
        if (player == null) {
            return false;
        }
        if (shouldCancel(packet)) {
            return true;
        }
        if (this.positionCorrections.value()
                && packet instanceof ClientboundPlayerPositionPacket positionPacket
                && hasRotation(positionPacket)) {
            // A setback must reach vanilla so it confirms the teleport; cancelling it leaves the player stuck.
            // This hook runs on the Netty thread before vanilla queues the packet on its packet processor.
            // Minecraft.runTick handles queued packets before it runs queued tasks, so the rotation is read
            // here and the task below puts it back after vanilla applied the setback.
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            MainThread.run(() -> {
                if (client.player == player) {
                    player.setYRot(yaw);
                    player.setXRot(pitch);
                    player.yRotO = yaw;
                    player.xRotO = pitch;
                }
            });
        }
        return false;
    }

    static boolean shouldCancel(final Packet<?> packet) {
        return packet instanceof ClientboundPlayerRotationPacket || packet instanceof ClientboundPlayerLookAtPacket;
    }

    private static boolean hasRotation(final ClientboundPlayerPositionPacket packet) {
        return !packet.relatives().contains(Relative.X_ROT)
                || !packet.relatives().contains(Relative.Y_ROT)
                || packet.change().xRot() != 0.0F
                || packet.change().yRot() != 0.0F;
    }
}
