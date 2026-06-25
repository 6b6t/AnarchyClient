package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;

import java.util.Locale;

public final class PacketLoggerModule extends Module {

    private final BooleanSetting incoming = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("incoming")
            .name("Incoming")
            .defaultValue(true)
            .build()));
    private final BooleanSetting outgoing = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("outgoing")
            .name("Outgoing")
            .defaultValue(false)
            .build()));
    private final BooleanSetting chat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chat")
            .name("Chat")
            .defaultValue(false)
            .build()));
    private final StringSetting filter = this.setting(StringSetting.from(StringSetting.builder()
            .id("filter")
            .name("Filter")
            .defaultValue("")
            .build()));

    public PacketLoggerModule() {
        super("packet_logger", "Packet Logger", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.incoming.value()) {
            this.log(client, "IN", packet);
        }
        return false;
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.outgoing.value()) {
            this.log(client, "OUT", packet);
        }
        return false;
    }

    private void log(final Minecraft client, final String direction, final Packet<?> packet) {
        String name = packetName(packet);
        if (!matchesFilter(name, this.filter.value())) {
            return;
        }
        String line = direction + " " + name;
        AnarchyClient.LOGGER.info("[packet] {}", line);
        if (this.chat.value() && client.player != null) {
            client.player.sendSystemMessage(Component.literal(line));
        }
    }

    static String packetName(final Packet<?> packet) {
        if (packet == null) {
            return "unknown";
        }
        return packet.type().id() + " (" + packet.getClass().getSimpleName() + ")";
    }

    static boolean matchesFilter(final String packetName, final String filter) {
        return filter == null
                || filter.isBlank()
                || packetName.toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT).trim());
    }
}
