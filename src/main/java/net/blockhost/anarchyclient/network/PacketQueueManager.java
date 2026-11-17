package net.blockhost.anarchyclient.network;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.RunningOnDifferentThreadException;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Delays packets for lag modules while keeping their order.
 *
 * <p>Each direction has one FIFO queue. Every entry belongs to the module that queued it. A module
 * releases only its own entries, and released entries are delivered from the head of the queue
 * once every older entry has been delivered too. One module's flush therefore never delivers
 * another module's packets early, and packets never overtake each other. When a module reaches its
 * packet limit, its oldest entry is released instead of letting the new packet bypass the queue.
 */
public final class PacketQueueManager {

    private static final DirectionQueue INCOMING = new DirectionQueue(PacketQueueManager::deliverIncoming);
    private static final DirectionQueue OUTGOING = new DirectionQueue(PacketQueueManager::deliverOutgoing);

    private PacketQueueManager() {
    }

    public static synchronized boolean queueIncoming(final String owner, final Connection connection,
                                                     final Packet<?> packet, final int maxPackets) {
        return INCOMING.queue(owner, connection, packet, maxPackets, System.currentTimeMillis());
    }

    public static synchronized boolean queueOutgoing(final String owner, final Connection connection,
                                                     final Packet<?> packet, final int maxPackets) {
        return OUTGOING.queue(owner, connection, packet, maxPackets, System.currentTimeMillis());
    }

    public static synchronized int flushIncomingOlderThan(final String owner, final long ageMillis) {
        return INCOMING.release(owner, olderThan(ageMillis));
    }

    public static synchronized int flushOutgoingOlderThan(final String owner, final long ageMillis) {
        return OUTGOING.release(owner, olderThan(ageMillis));
    }

    public static synchronized int flushOlderThan(final String owner, final long ageMillis) {
        return flushIncomingOlderThan(owner, ageMillis) + flushOutgoingOlderThan(owner, ageMillis);
    }

    public static synchronized int flushIncoming(final String owner) {
        return INCOMING.release(owner, entry -> true);
    }

    public static synchronized int flushOutgoing(final String owner) {
        return OUTGOING.release(owner, entry -> true);
    }

    public static synchronized int flushAll(final String owner) {
        return flushIncoming(owner) + flushOutgoing(owner);
    }

    public static synchronized int dropIncoming(final String owner) {
        return INCOMING.drop(owner);
    }

    public static synchronized int dropOutgoing(final String owner) {
        return OUTGOING.drop(owner);
    }

    public static synchronized int dropAll(final String owner) {
        return dropIncoming(owner) + dropOutgoing(owner);
    }

    public static synchronized int incomingSize(final String owner) {
        return INCOMING.size(owner);
    }

    public static synchronized int outgoingSize(final String owner) {
        return OUTGOING.size(owner);
    }

    /** Total queued incoming packets of all owners. */
    public static synchronized int incomingSize() {
        return INCOMING.totalSize();
    }

    /** Total queued outgoing packets of all owners. */
    public static synchronized int outgoingSize() {
        return OUTGOING.totalSize();
    }

    public static synchronized Snapshot snapshot() {
        long now = System.currentTimeMillis();
        return new Snapshot(
                INCOMING.totalSize(),
                OUTGOING.totalSize(),
                INCOMING.oldestAgeMillis(now),
                OUTGOING.oldestAgeMillis(now),
                INCOMING.sampleTypes(),
                OUTGOING.sampleTypes()
        );
    }

    private static Predicate<QueuedPacket> olderThan(final long ageMillis) {
        long now = System.currentTimeMillis();
        return entry -> now - entry.createdAtMillis >= ageMillis;
    }

    private static void deliverIncoming(final QueuedPacket entry) {
        PacketListener listener = entry.connection.getPacketListener();
        if (listener == null) {
            return;
        }
        try {
            handle(entry.packet, listener);
        } catch (RunningOnDifferentThreadException ignored) {
            // Vanilla handlers reschedule themselves on the client thread, as in Connection.channelRead0.
        }
    }

    private static void deliverOutgoing(final QueuedPacket entry) {
        PacketEventSilencer.runSilently(() -> entry.connection.send(entry.packet));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(final Packet packet, final PacketListener listener) {
        packet.handle(listener);
    }

    /** One direction's queue. Package-private for tests. */
    static final class DirectionQueue {

        private final Deque<QueuedPacket> entries = new ArrayDeque<>();
        private final Consumer<QueuedPacket> delivery;

        DirectionQueue(final Consumer<QueuedPacket> delivery) {
            this.delivery = delivery;
        }

        boolean queue(final String owner, final Connection connection, final Packet<?> packet, final int maxPackets,
                      final long nowMillis) {
            if (owner == null || owner.isBlank() || connection == null || packet == null) {
                return false;
            }
            int limit = Math.max(1, maxPackets);
            while (this.size(owner) >= limit) {
                this.releaseOldest(owner);
                this.drain();
            }
            this.entries.addLast(new QueuedPacket(owner, connection, packet, nowMillis));
            return true;
        }

        int release(final String owner, final Predicate<QueuedPacket> predicate) {
            for (QueuedPacket entry : this.entries) {
                if (entry.owner.equals(owner) && predicate.test(entry)) {
                    entry.released = true;
                }
            }
            return this.drain();
        }

        int drop(final String owner) {
            int before = this.entries.size();
            this.entries.removeIf(entry -> entry.owner.equals(owner));
            int dropped = before - this.entries.size();
            this.drain();
            return dropped;
        }

        int size(final String owner) {
            int count = 0;
            for (QueuedPacket entry : this.entries) {
                if (entry.owner.equals(owner) && !entry.released) {
                    count++;
                }
            }
            return count;
        }

        int totalSize() {
            return this.entries.size();
        }

        private void releaseOldest(final String owner) {
            for (QueuedPacket entry : this.entries) {
                if (entry.owner.equals(owner) && !entry.released) {
                    entry.released = true;
                    return;
                }
            }
        }

        private int drain() {
            int delivered = 0;
            while (!this.entries.isEmpty() && this.entries.peekFirst().released) {
                this.delivery.accept(this.entries.pollFirst());
                delivered++;
            }
            return delivered;
        }

        private long oldestAgeMillis(final long nowMillis) {
            QueuedPacket oldest = this.entries.peekFirst();
            return oldest == null ? 0L : Math.max(0L, nowMillis - oldest.createdAtMillis);
        }

        private List<String> sampleTypes() {
            return this.entries.stream()
                    .limit(6)
                    .map(entry -> entry.packet.getClass().getSimpleName())
                    .toList();
        }
    }

    static final class QueuedPacket {

        private final String owner;
        private final Connection connection;
        private final Packet<?> packet;
        private final long createdAtMillis;
        private boolean released;

        QueuedPacket(final String owner, final Connection connection, final Packet<?> packet, final long createdAtMillis) {
            this.owner = owner;
            this.connection = Objects.requireNonNull(connection);
            this.packet = Objects.requireNonNull(packet);
            this.createdAtMillis = createdAtMillis;
        }

        String owner() {
            return this.owner;
        }

        long createdAtMillis() {
            return this.createdAtMillis;
        }
    }

    public record Snapshot(int incomingSize, int outgoingSize, long oldestIncomingAgeMillis,
                           long oldestOutgoingAgeMillis, List<String> incomingTypes, List<String> outgoingTypes) {
    }
}
