package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;

public final class NoSwingModule extends Module {

    private final BooleanSetting serverSide = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("server_side")
            .name("Server Side")
            .defaultValue(false)
            .description("Stops swing animation packets from being sent to the server.")
            .build()));

    public NoSwingModule() {
        super("no_swing", "No Swing", ModuleCategory.RENDER);
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return client.player != null && shouldCancel(packet, this.serverSide.value());
    }

    static boolean shouldCancel(final Packet<?> packet, final boolean serverSide) {
        return serverSide && packet instanceof ServerboundSwingPacket;
    }
}
