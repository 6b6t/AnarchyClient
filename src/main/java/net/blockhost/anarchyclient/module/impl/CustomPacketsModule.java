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
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;

import java.util.Locale;

public final class CustomPacketsModule extends Module {

    private final BooleanSetting chat = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("chat")
            .name("Chat")
            .defaultValue(true)
            .build()));
    private final BooleanSetting logger = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("logger")
            .name("Logger")
            .defaultValue(true)
            .build()));
    private final StringSetting filter = this.setting(StringSetting.from(StringSetting.builder()
            .id("filter")
            .name("Filter")
            .defaultValue("")
            .build()));

    public CustomPacketsModule() {
        super("custom_packets", "Custom Packets", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundCustomPayloadPacket customPayload)) {
            return false;
        }
        String description = describe(customPayload);
        if (!matches(description, this.filter.value())) {
            return false;
        }
        if (this.logger.value()) {
            AnarchyClient.LOGGER.info("[custom-payload] {}", description);
        }
        if (this.chat.value() && client.player != null) {
            client.player.sendSystemMessage(Component.literal("Custom payload: " + description));
        }
        return false;
    }

    static String describe(final ClientboundCustomPayloadPacket packet) {
        String id = packet.payload().type().id().toString();
        if (packet.payload() instanceof BrandPayload brandPayload) {
            return id + " brand=" + brandPayload.brand();
        }
        return id + " payload=" + packet.payload().getClass().getSimpleName();
    }

    static boolean matches(final String description, final String filter) {
        return filter == null
                || filter.isBlank()
                || description.toLowerCase(Locale.ROOT).contains(filter.trim().toLowerCase(Locale.ROOT));
    }
}
