package net.blockhost.anarchyclient.module.impl;

import java.util.Locale;
import net.blockhost.anarchyclient.AnarchyClient;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlePacket;
import net.minecraft.network.protocol.Packet;

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
    private final BooleanSetting chat = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("chat").name("Chat").defaultValue(false).build()));
    private final StringSetting filter = this.setting(StringSetting.from(
            StringSetting.builder().id("filter").name("Filter").defaultValue("").build()));

    public PacketLoggerModule() {
        super(
                "packet_logger",
                "Packet Logger",
                ModuleCategory.MISC,
                "Records packet traffic for debugging protocol behavior and module interactions.");
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
        if (packet instanceof BundlePacket<?> bundle) {
            // Entity spawns and similar arrive inside bundles; log what they carry, not just the wrapper.
            for (Packet<?> subPacket : bundle.subPackets()) {
                this.log(client, direction, subPacket);
            }
            return;
        }
        String name = packetName(packet);
        if (!matchesFilter(name, this.filter.value())) {
            return;
        }
        String line = direction + " " + name;
        AnarchyClient.LOGGER.info("[packet] {}", line);
        if (this.chat.value()) {
            MainThread.run(() -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(Component.literal(line));
                }
            });
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
                || packetName
                        .toLowerCase(Locale.ROOT)
                        .contains(filter.toLowerCase(Locale.ROOT).trim());
    }
}
