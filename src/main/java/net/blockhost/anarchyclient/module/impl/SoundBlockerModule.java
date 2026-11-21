package net.blockhost.anarchyclient.module.impl;

import java.util.Set;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.blockhost.anarchyclient.util.MainThread;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.sounds.SoundEvent;

public final class SoundBlockerModule extends Module {

    private final StringSetting sounds = this.setting(StringSetting.from(
            StringSetting.builder().id("sounds").name("Sounds").defaultValue("").build()));
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
    /** Parsed sound list, cached by its source text. Read on the Netty thread. */
    private volatile ParsedSounds parsedSounds = new ParsedSounds("", Set.of());

    public SoundBlockerModule() {
        super(
                "sound_blocker",
                "Sound Blocker",
                ModuleCategory.MISC,
                "Cancels selected server sounds, including entity sounds and the sound of explosions that do not push you.");
    }

    @Override
    public boolean receivePacket(final Minecraft client, final Connection connection, final Packet<?> packet) {
        return switch (packet) {
            case ClientboundSoundPacket sound -> this.block(client, soundId(sound.getSound()));
            case ClientboundSoundEntityPacket sound -> this.block(client, soundId(sound.getSound()));
            // Explosions that push you are left to Velocity, which replaces those packets itself; two
            // replacements would apply the explosion twice.
            case ClientboundExplodePacket explosion
            when explosion.playSound() && explosion.playerKnockback().isEmpty() -> {
                if (!this.block(client, soundId(explosion.explosionSound()))) {
                    yield false;
                }
                // Keep the explosion itself (knockback, particles) and only drop its sound.
                handleReplacement(
                        connection,
                        new ClientboundExplodePacket(
                                explosion.center(),
                                explosion.radius(),
                                explosion.blockCount(),
                                explosion.playerKnockback(),
                                explosion.explosionParticle(),
                                explosion.explosionSound(),
                                explosion.blockParticles(),
                                false));
                yield true;
            }
            default -> false;
        };
    }

    private boolean block(final Minecraft client, final Identifier id) {
        if (!this.shouldBlock(id)) {
            return false;
        }
        if (this.logBlocked.value()) {
            MainThread.run(() -> {
                if (client.player != null) {
                    client.player.sendSystemMessage(Translations.component(
                            "anarchyclient.message.sound_blocker.blocked_sound", "Blocked sound %s", id));
                }
            });
        }
        return true;
    }

    private boolean shouldBlock(final Identifier id) {
        String value = this.sounds.value();
        ParsedSounds parsed = this.parsedSounds;
        if (!parsed.source().equals(value)) {
            parsed = new ParsedSounds(value, SoundScan.parseSoundIds(value));
            this.parsedSounds = parsed;
        }
        return shouldBlock(id, parsed.ids(), this.invert.value());
    }

    private static Identifier soundId(final Holder<SoundEvent> sound) {
        return sound.unwrapKey()
                .map(ResourceKey::identifier)
                .orElseGet(() -> sound.value().location());
    }

    /** Hands a replacement packet to vanilla the same way {@code Connection.channelRead0} does. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handleReplacement(final Connection connection, final Packet packet) {
        PacketListener listener = connection.getPacketListener();
        if (listener == null) {
            return;
        }
        try {
            packet.handle(listener);
        } catch (RunningOnDifferentThreadException ignored) {
            // Vanilla handlers queue themselves on the client thread.
        }
    }

    static boolean shouldBlock(final Identifier id, final Set<Identifier> sounds, final boolean invert) {
        if (id == null || sounds.isEmpty()) {
            return false;
        }
        boolean listed = sounds.contains(id);
        return invert ? !listed : listed;
    }

    private record ParsedSounds(String source, Set<Identifier> ids) {}
}
