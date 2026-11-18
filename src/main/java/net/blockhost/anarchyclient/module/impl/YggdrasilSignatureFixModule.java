package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import java.util.List;

public final class YggdrasilSignatureFixModule extends Module {

    private final BooleanSetting warn = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("warn")
            .name("Warn")
            .defaultValue(true)
            .build()));
    private int warnCooldown;

    public YggdrasilSignatureFixModule() {
        super("yggdrasil_signature_fix", "Yggdrasil Signature Fix", ModuleCategory.MISC, "Warns in chat when the server sends player skins without a valid Yggdrasil signature. Vanilla can show"
                        + " the default skin for those players.",
                List.of("signature_fix"));
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.warnCooldown > 0) {
            this.warnCooldown--;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info) || !this.warn.value()) {
            return false;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile != null && hasUnsignedTextures(profile)) {
                String name = profile.name();
                MainThread.run(() -> this.warn(client, name));
                return false;
            }
        }
        return false;
    }

    private void warn(final Minecraft client, final String name) {
        if (this.warnCooldown == 0 && client.player != null) {
            client.player.sendSystemMessage(Translations.component("anarchyclient.message.yggdrasil_signature_fix.unsigned_yggdrasil_textures_for", "Unsigned Yggdrasil textures for %s.", name));
            this.warnCooldown = 80;
        }
    }

    static boolean hasUnsignedTextures(final GameProfile profile) {
        if (profile == null || profile.properties() == null) {
            return false;
        }
        for (Property property : profile.properties().get("textures")) {
            if (!property.hasSignature()) {
                return true;
            }
        }
        return false;
    }
}
