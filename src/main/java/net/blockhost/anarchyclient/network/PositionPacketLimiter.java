package net.blockhost.anarchyclient.network;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Keeps the client at one position packet per client tick.
 *
 * <p>A 26.3 server kicks a client with "Invalid move player packet received" when a second
 * position packet arrives before the client's tick-end packet. Vanilla sends one per tick, but
 * modules that send their own position packets, or release held ones together, would add more.
 * The limiter lets the first through and strips the position from the rest, keeping their
 * rotation and ground state.
 */
public final class PositionPacketLimiter {

    private static boolean positionSent;

    private PositionPacketLimiter() {}

    /** Returns the packet to send in place of {@code packet}, which is the same packet when it is allowed. */
    public static synchronized Packet<?> limit(final Packet<?> packet) {
        if (packet instanceof ServerboundClientTickEndPacket) {
            positionSent = false;
            return packet;
        }
        if (!(packet instanceof ServerboundMovePlayerPacket move) || !move.hasPosition()) {
            return packet;
        }
        if (!positionSent) {
            positionSent = true;
            return packet;
        }
        return withoutPosition(move);
    }

    public static synchronized void reset() {
        positionSent = false;
    }

    static ServerboundMovePlayerPacket withoutPosition(final ServerboundMovePlayerPacket move) {
        if (move.hasRotation()) {
            return new ServerboundMovePlayerPacket.Rot(
                    move.getYRot(0.0F), move.getXRot(0.0F), move.isOnGround(), move.horizontalCollision());
        }
        return new ServerboundMovePlayerPacket.StatusOnly(move.isOnGround(), move.horizontalCollision());
    }
}
