package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public final class BungeeCordSpoofModule extends Module {

    private final StringSetting forwardedIp = this.setting(StringSetting.from(StringSetting.builder()
            .id("ip")
            .name("IP")
            .defaultValue("127.0.0.1")
            .build()));
    private final BooleanSetting includeProfile = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("profile")
            .name("Profile")
            .defaultValue(true)
            .build()));
    private final StringSetting serverFilter = this.setting(StringSetting.from(StringSetting.builder()
            .id("servers")
            .name("Servers")
            .defaultValue("")
            .description("Comma-separated host fragments. Empty matches all.")
            .build()));

    public BungeeCordSpoofModule() {
        super("bungeecord_spoof", "BungeeCord Spoof", ModuleCategory.MISC);
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientIntentionPacket intention) || intention.intention() != ClientIntent.LOGIN
                || !matches(intention.hostName(), this.serverFilter.value())) {
            return packet;
        }
        UUID profileId = client.getUser().getProfileId();
        String host = intention.hostName() + "\0" + this.forwardedIp.value().trim() + "\0"
                + profileId.toString().replace("-", "");
        if (this.includeProfile.value()) {
            host += "\0[{\"name\":\"name\",\"value\":\"" + client.getUser().getName() + "\"}]";
        }
        return new ClientIntentionPacket(intention.protocolVersion(), host, intention.port(), intention.intention());
    }

    static boolean matches(final String host, final String filters) {
        if (filters == null || filters.isBlank()) {
            return true;
        }
        String normalized = host == null ? "" : host.toLowerCase(Locale.ROOT);
        return Arrays.stream(filters.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .anyMatch(normalized::contains);
    }
}
