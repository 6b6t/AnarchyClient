package net.blockhost.anarchyclient.module.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.authlib.properties.Property;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.jspecify.annotations.Nullable;

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
        super(
                "bungeecord_spoof",
                "BungeeCord Spoof",
                ModuleCategory.MISC,
                "Adds a forwarded IP, your UUID, and optionally your profile properties such as your skin to the login handshake so BungeeCord-mode backend servers accept a direct connection.");
    }

    @Override
    public Packet<?> replaceSendPacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientIntentionPacket intention)
                || intention.intention() != ClientIntent.LOGIN
                || !matches(intention.hostName(), this.serverFilter.value())) {
            return packet;
        }
        String host = forwardedHost(
                intention.hostName(),
                this.forwardedIp.value().trim(),
                client.getUser().getProfileId(),
                this.includeProfile.value()
                        ? client.getGameProfile().properties().values()
                        : null);
        // Vanilla has no non-deprecated way to build a handshake packet; Connection uses this constructor too.
        @SuppressWarnings("deprecation")
        ClientIntentionPacket forwarded =
                new ClientIntentionPacket(intention.protocolVersion(), host, intention.port(), intention.intention());
        return forwarded;
    }

    /**
     * Builds the BungeeCord IP forwarding host: {@code host\0ip\0uuid[\0properties]}, where the optional
     * properties are a JSON array of {@code {name, value, signature}} profile properties such as skin textures.
     */
    static String forwardedHost(
            final String host, final String ip, final UUID profileId, final @Nullable Collection<Property> properties) {
        String forwarded = host + "\0" + ip + "\0" + profileId.toString().replace("-", "");
        return properties == null ? forwarded : forwarded + "\0" + propertiesJson(properties);
    }

    static String propertiesJson(final Collection<Property> properties) {
        JsonArray array = new JsonArray();
        for (Property property : properties) {
            JsonObject json = new JsonObject();
            json.addProperty("name", property.name());
            json.addProperty("value", property.value());
            if (property.hasSignature()) {
                json.addProperty("signature", property.signature());
            }
            array.add(json);
        }
        return array.toString();
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
