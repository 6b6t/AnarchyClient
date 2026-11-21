package net.blockhost.anarchyclient.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class PositionPacketLimiterTest {

    @BeforeEach
    void reset() {
        PositionPacketLimiter.reset();
    }

    @Test
    void allowsOnePositionPacketPerClientTick() {
        ServerboundMovePlayerPacket first = new ServerboundMovePlayerPacket.Pos(1.0, 64.0, 1.0, true, false);
        ServerboundMovePlayerPacket second =
                new ServerboundMovePlayerPacket.PosRot(2.0, 65.0, 2.0, 90.0F, 10.0F, false, false);

        assertSame(first, PositionPacketLimiter.limit(first));
        Packet<?> stripped = PositionPacketLimiter.limit(second);

        ServerboundMovePlayerPacket rotation = assertInstanceOf(ServerboundMovePlayerPacket.Rot.class, stripped);
        assertFalse(rotation.hasPosition());
        assertEquals(90.0F, rotation.getYRot(0.0F));
        assertFalse(rotation.isOnGround());
    }

    @Test
    void tickEndPacketOpensTheNextTick() {
        PositionPacketLimiter.limit(new ServerboundMovePlayerPacket.Pos(1.0, 64.0, 1.0, true, false));
        PositionPacketLimiter.limit(ServerboundClientTickEndPacket.INSTANCE);
        ServerboundMovePlayerPacket next = new ServerboundMovePlayerPacket.Pos(1.0, 65.0, 1.0, true, false);

        assertSame(next, PositionPacketLimiter.limit(next));
    }

    @Test
    void extraPositionOnlyPacketsKeepTheirGroundState() {
        PositionPacketLimiter.limit(new ServerboundMovePlayerPacket.Pos(1.0, 64.0, 1.0, true, false));

        Packet<?> stripped =
                PositionPacketLimiter.limit(new ServerboundMovePlayerPacket.Pos(1.0, 70.0, 1.0, true, true));

        ServerboundMovePlayerPacket status = assertInstanceOf(ServerboundMovePlayerPacket.StatusOnly.class, stripped);
        assertTrue(status.isOnGround());
        assertTrue(status.horizontalCollision());
    }

    @Test
    void ignoresOtherPackets() {
        ServerboundMovePlayerPacket rotation = new ServerboundMovePlayerPacket.Rot(0.0F, 0.0F, true, false);
        PositionPacketLimiter.limit(new ServerboundMovePlayerPacket.Pos(1.0, 64.0, 1.0, true, false));

        assertSame(rotation, PositionPacketLimiter.limit(rotation));
    }
}
