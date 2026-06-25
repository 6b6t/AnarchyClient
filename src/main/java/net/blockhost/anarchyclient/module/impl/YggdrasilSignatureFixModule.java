package net.blockhost.anarchyclient.module.impl;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

public final class YggdrasilSignatureFixModule extends Module {

    private final BooleanSetting warn = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("warn")
            .name("Warn")
            .defaultValue(true)
            .build()));
    private final BooleanSetting cancelInvalid = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("cancel_invalid")
            .name("Cancel")
            .defaultValue(false)
            .build()));
    private int warnCooldown;

    public YggdrasilSignatureFixModule() {
        super("yggdrasil_signature_fix", "Yggdrasil Signature Fix", ModuleCategory.MISC,
                java.util.List.of("signature_fix"));
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.warnCooldown > 0) {
            this.warnCooldown--;
        }
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundPlayerInfoUpdatePacket info)) {
            return false;
        }
        for (ClientboundPlayerInfoUpdatePacket.Entry entry : info.newEntries()) {
            GameProfile profile = entry.profile();
            if (profile != null && hasUnsignedTextures(profile)) {
                if (this.warn.value() && this.warnCooldown == 0 && client.player != null) {
                    client.player.sendSystemMessage(Component.literal("Unsigned Yggdrasil textures for " + profile.name() + "."));
                    this.warnCooldown = 80;
                }
                return this.cancelInvalid.value();
            }
        }
        return false;
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
