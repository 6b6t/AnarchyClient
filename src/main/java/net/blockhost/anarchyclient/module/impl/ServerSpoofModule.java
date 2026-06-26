package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringListSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;

import java.util.List;
import java.util.Locale;

public final class ServerSpoofModule extends Module {

    private final BooleanSetting spoofHost = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("spoof_host")
            .name("Host Spoof")
            .defaultValue(true)
            .build()));
    private final StringSetting host = this.setting(StringSetting.from(StringSetting.builder()
            .id("host")
            .name("Host")
            .defaultValue("localhost")
            .build()));
    private final BooleanSetting spoofBrand = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("spoof_brand")
            .name("Brand Spoof")
            .defaultValue(true)
            .build()));
    private final StringSetting brand = this.setting(StringSetting.from(StringSetting.builder()
            .id("brand")
            .name("Brand")
            .defaultValue("vanilla")
            .build()));
    private final BooleanSetting blockChannels = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("block_channels")
            .name("Block Channels")
            .defaultValue(true)
            .build()));
    private final StringListSetting channels = this.setting(StringListSetting.from(StringListSetting.builder()
            .id("channels")
            .name("Channels")
            .addAllDefaultValue(List.of("fabric", "minecraft:register"))
            .build()));
    private final BooleanSetting resourcePack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("resource_pack")
            .name("Resource Pack")
            .defaultValue(false)
            .build()));

    public ServerSpoofModule() {
        super("server_spoof", "Server Spoof", ModuleCategory.MISC);
        this.host.visibleWhen(this.spoofHost::value);
        this.brand.visibleWhen(this.spoofBrand::value);
        this.channels.visibleWhen(this.blockChannels::value);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.spoofHost.value()
                && packet instanceof ClientIntentionPacket intention
                && intention.intention() == ClientIntent.LOGIN) {
            return new ClientIntentionPacket(
                    intention.protocolVersion(),
                    this.host.value(),
                    intention.port(),
                    intention.intention()
            );
        }
        if (this.spoofBrand.value()
                && packet instanceof ServerboundCustomPayloadPacket customPayload
                && customPayload.payload() instanceof BrandPayload) {
            return new ServerboundCustomPayloadPacket(new BrandPayload(this.brand.value()));
        }
        return packet;
    }

    @Override
    public boolean sendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (this.resourcePack.value() && packet instanceof ServerboundResourcePackPacket) {
            return true;
        }
        return this.blockChannels.value()
                && packet instanceof ServerboundCustomPayloadPacket customPayload
                && matchesChannel(customPayload.payload().type().id().toString(), this.channels.value());
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!this.resourcePack.value() || !(packet instanceof ClientboundResourcePackPushPacket push)) {
            return false;
        }
        PacketEventSilencer.runSilently(() -> {
            connection.send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
            connection.send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
            connection.send(new ServerboundResourcePackPacket(push.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        });
        return true;
    }

    static boolean matchesChannel(final String channel, final List<String> filters) {
        if (channel == null || filters == null || filters.isEmpty()) {
            return false;
        }
        String normalizedChannel = channel.toLowerCase(Locale.ROOT);
        for (String filter : filters) {
            if (filter != null && !filter.isBlank()
                    && normalizedChannel.contains(filter.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
