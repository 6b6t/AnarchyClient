package net.blockhost.anarchyclient.module.impl;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

/**
 * Makes the server end the session itself by sending a movement packet with invalid coordinates.
 *
 * <p>The server kicks the player as soon as it reads the packet, so the player leaves the world at
 * once, even behind a proxy that keeps the backend connection open after a normal disconnect.
 */
final class IllegalDisconnect {

    private IllegalDisconnect() {
    }

    static ServerboundMovePlayerPacket packet() {
        return new ServerboundMovePlayerPacket.Pos(Double.NaN, Double.NaN, Double.NaN, false, false);
    }

    static void send(final Connection connection) {
        if (connection != null && connection.isConnected()) {
            connection.send(packet());
        }
    }
}
