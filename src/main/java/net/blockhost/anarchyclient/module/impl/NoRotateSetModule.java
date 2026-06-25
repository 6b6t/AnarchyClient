package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
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
            .description("Also blocks rotation embedded in server position corrections.")
            .build()));

    public NoRotateSetModule() {
        super("no_rotate_set", "No Rotate Set", ModuleCategory.PLAYER);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.player != null && shouldCancel(packet, this.positionCorrections.value());
    }

    static boolean shouldCancel(final Packet<?> packet, final boolean includePositionCorrections) {
        if (packet instanceof ClientboundPlayerRotationPacket || packet instanceof ClientboundPlayerLookAtPacket) {
            return true;
        }
        return includePositionCorrections
                && packet instanceof ClientboundPlayerPositionPacket positionPacket
                && hasRotation(positionPacket);
    }

    private static boolean hasRotation(final ClientboundPlayerPositionPacket packet) {
        return !packet.relatives().contains(Relative.X_ROT)
                || !packet.relatives().contains(Relative.Y_ROT)
                || packet.change().xRot() != 0.0F
                || packet.change().yRot() != 0.0F;
    }
}
