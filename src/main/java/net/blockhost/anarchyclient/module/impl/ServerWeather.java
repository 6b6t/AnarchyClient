package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;

/**
 * Remembers the weather the server last sent, so a module that overrides the client weather can restore it.
 * The server only sends weather packets when the weather changes, so the level cannot be left to catch up.
 */
final class ServerWeather {

    private volatile float rain;
    private volatile float thunder;

    /** Starts from the level's current weather. Call on the client thread before overriding the weather. */
    void capture(final ClientLevel level) {
        if (level == null) {
            return;
        }
        float rainLevel = level.getRainLevel(1.0F);
        this.rain = rainLevel;
        this.thunder = rainLevel > 0.0F ? Math.min(1.0F, level.getThunderLevel(1.0F) / rainLevel) : 0.0F;
    }

    /** Records weather packets. Safe on the Netty thread: it only stores two floats. */
    void observe(final Packet<?> packet) {
        if (packet instanceof ClientboundLoginPacket || packet instanceof ClientboundRespawnPacket) {
            // A new level starts clear; the server sends its weather right after.
            this.rain = 0.0F;
            this.thunder = 0.0F;
        } else if (packet instanceof ClientboundGameEventPacket event) {
            this.observe(event.getEvent(), event.getParam());
        }
    }

    void observe(final ClientboundGameEventPacket.Type event, final float param) {
        // Vanilla applies START_RAINING as level 0 and STOP_RAINING as level 1; level changes follow.
        if (event == ClientboundGameEventPacket.START_RAINING) {
            this.rain = 0.0F;
        } else if (event == ClientboundGameEventPacket.STOP_RAINING) {
            this.rain = 1.0F;
        } else if (event == ClientboundGameEventPacket.RAIN_LEVEL_CHANGE) {
            this.rain = clamp(param);
        } else if (event == ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
            this.thunder = clamp(param);
        }
    }

    void restore(final ClientLevel level) {
        if (level != null) {
            level.setRainLevel(this.rain);
            level.setThunderLevel(this.thunder);
        }
    }

    float rain() {
        return this.rain;
    }

    float thunder() {
        return this.thunder;
    }

    private static float clamp(final float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }
}
