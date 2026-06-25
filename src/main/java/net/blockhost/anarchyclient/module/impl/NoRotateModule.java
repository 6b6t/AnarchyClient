package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

import java.util.List;

public final class NoRotateModule extends Module {

    private final BooleanSetting positionCorrections = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("position_corrections")
            .name("Position Packets")
            .defaultValue(false)
            .build()));

    public NoRotateModule() {
        super("no_rotate", "No Rotate", ModuleCategory.PLAYER, List.of("no_rotate_set"));
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.player != null && NoRotateSetModule.shouldCancel(packet, this.positionCorrections.value());
    }
}
