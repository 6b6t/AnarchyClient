package net.blockhost.anarchyclient.network;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinecraftBootstrapExtension.class)
class PacketQueueManagerTest {

    private final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
    private final List<Long> delivered = new ArrayList<>();
    private final PacketQueueManager.DirectionQueue queue =
            new PacketQueueManager.DirectionQueue(entry -> this.delivered.add(entry.createdAtMillis()));

    @Test
    void fullQueueReleasesOldestPacketInsteadOfLettingTheNewOneOvertake() {
        this.queue("fake_lag", 1);
        this.queue("fake_lag", 2);
        this.queue("fake_lag", 3);

        assertEquals(List.of(1L), this.delivered);
        assertEquals(2, this.queue.size("fake_lag"));
    }

    @Test
    void flushingOneOwnerKeepsOtherOwnersPacketsQueued() {
        this.queue("backtrack", 1);
        this.queue("ping_spoof", 2);
        this.queue("backtrack", 3);

        this.queue.release("backtrack", entry -> true);

        assertEquals(List.of(1L), this.delivered);
        assertEquals(1, this.queue.size("ping_spoof"));
    }

    @Test
    void releasedPacketsWaitForOlderPacketsOfOtherOwners() {
        this.queue("ping_spoof", 1);
        this.queue("backtrack", 2);

        this.queue.release("backtrack", entry -> true);
        assertEquals(List.of(), this.delivered);

        this.queue.release("ping_spoof", entry -> true);
        assertEquals(List.of(1L, 2L), this.delivered);
    }

    @Test
    void droppingOneOwnerDeliversReleasedPacketsBehindIt() {
        this.queue("ping_spoof", 1);
        this.queue("backtrack", 2);
        this.queue.release("backtrack", entry -> true);

        assertEquals(1, this.queue.drop("ping_spoof"));
        assertEquals(List.of(2L), this.delivered);
        assertEquals(0, this.queue.totalSize());
    }

    private void queue(final String owner, final long sequence) {
        this.queue.queue(owner, this.connection, ServerboundPunchPacket.INSTANCE, 2, sequence);
    }
}
