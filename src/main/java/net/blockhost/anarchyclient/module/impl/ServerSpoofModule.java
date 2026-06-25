package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;

public final class ServerSpoofModule extends Module {

    private final StringSetting host = this.setting(StringSetting.from(StringSetting.builder()
            .id("host")
            .name("Host")
            .defaultValue("localhost")
            .build()));

    public ServerSpoofModule() {
        super("server_spoof", "Server Spoof", ModuleCategory.MISC);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (packet instanceof ClientIntentionPacket intention && intention.intention() == ClientIntent.LOGIN) {
            return new ClientIntentionPacket(intention.protocolVersion(), this.host.value(), intention.port(), intention.intention());
        }
        return packet;
    }
}
