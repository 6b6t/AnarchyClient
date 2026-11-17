package net.blockhost.anarchyclient.rotation;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * Remembers the last rotation the client sent to the server.
 *
 * <p>The connection mixin reports every packet that actually leaves the client, including packets
 * changed by modules and packets released late by lag modules, so this matches what the server
 * knows. Packets can be sent from the Netty thread, hence the volatile field.
 */
public final class ServerRotationTracker {

    private static volatile @Nullable Rotation lastSent;

    private ServerRotationTracker() {
    }

    public static void onSent(final Packet<?> packet) {
        if (packet instanceof ServerboundMovePlayerPacket move && move.hasRotation()) {
            lastSent = new Rotation(move.getYRot(0.0F), move.getXRot(0.0F));
        } else if (packet instanceof ServerboundAcceptTeleportationPacket accept) {
            lastSent = new Rotation(accept.yRot(), accept.xRot());
        }
    }

    public static Optional<Rotation> lastSent() {
        return Optional.ofNullable(lastSent);
    }

    public static void reset() {
        lastSent = null;
    }
}
