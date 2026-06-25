package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;

import java.util.Set;

public final class SoundBlockerModule extends Module {

    private final StringSetting sounds = this.setting(StringSetting.from(StringSetting.builder()
            .id("sounds")
            .name("Sounds")
            .defaultValue("")
            .build()));
    private final BooleanSetting invert = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("invert")
            .name("Invert")
            .defaultValue(false)
            .build()));
    private final BooleanSetting logBlocked = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("log_blocked")
            .name("Log")
            .defaultValue(false)
            .build()));
    private String lastSounds = "";
    private Set<Identifier> parsedSounds = Set.of();

    public SoundBlockerModule() {
        super("sound_blocker", "Sound Blocker", ModuleCategory.MISC);
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        if (!(packet instanceof ClientboundSoundPacket soundPacket)) {
            return false;
        }
        Identifier id = SoundScan.soundId(soundPacket);
        if (!this.shouldBlock(id)) {
            return false;
        }
        if (this.logBlocked.value() && client.player != null) {
            client.player.sendSystemMessage(Component.literal("Blocked sound " + id));
        }
        return true;
    }

    private boolean shouldBlock(final Identifier id) {
        if (!this.lastSounds.equals(this.sounds.value())) {
            this.parsedSounds = SoundScan.parseSoundIds(this.sounds.value());
            this.lastSounds = this.sounds.value();
        }
        return shouldBlock(id, this.parsedSounds, this.invert.value());
    }

    static boolean shouldBlock(final Identifier id, final Set<Identifier> sounds, final boolean invert) {
        if (id == null || sounds.isEmpty()) {
            return false;
        }
        boolean listed = sounds.contains(id);
        return invert ? !listed : listed;
    }
}
