package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public final class PacketCancellerModule extends Module {

    private final BooleanSetting incoming = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("incoming")
            .name("Incoming")
            .defaultValue(false)
            .build()));
    private final BooleanSetting outgoing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("outgoing")
            .name("Outgoing")
            .defaultValue(false)
            .build()));
    private final StringSetting incomingFilter = this.setting(StringSetting.from(StringSetting.builder()
            .id("incoming_filter")
            .name("Incoming Filter")
            .defaultValue("")
            .build()));
    private final StringSetting outgoingFilter = this.setting(StringSetting.from(StringSetting.builder()
            .id("outgoing_filter")
            .name("Outgoing Filter")
            .defaultValue("")
            .build()));

    public PacketCancellerModule() {
        super("packet_canceller", "Packet Canceller", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return shouldCancel(packet, this.incoming.value(), this.incomingFilter.value());
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return shouldCancel(packet, this.outgoing.value(), this.outgoingFilter.value());
    }

    static boolean shouldCancel(final Packet<?> packet, final boolean enabled, final String filter) {
        return enabled
                && filter != null
                && !filter.isBlank()
                && PacketLoggerModule.matchesFilter(PacketLoggerModule.packetName(packet), filter);
    }
}
