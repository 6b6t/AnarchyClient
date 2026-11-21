package net.blockhost.anarchyclient.network;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

/**
 * Delays packets for lag modules while keeping each module's order.
 *
 * <p>Each direction keeps one FIFO queue per module. A module releases only its own packets, and
 * they are delivered right away in the order the module queued them. Packets that another module
 * still holds never block them, so a long hold such as a spoofed keep-alive does not stall another
 * module's flush. When a module reaches its packet limit, its oldest packet is delivered instead of
 * letting the new packet bypass the queue.
 */
public final class PacketQueueManager {

    private static final DirectionQueue INCOMING = new DirectionQueue(PacketQueueManager::deliverIncoming);
    private static final DirectionQueue OUTGOING = new DirectionQueue(PacketQueueManager::deliverOutgoing);

    private PacketQueueManager() {}

    public static synchronized boolean queueIncoming(
            final String owner, final Connection connection, final Packet<?> packet, final int maxPackets) {
        return INCOMING.queue(owner, connection, packet, maxPackets, System.currentTimeMillis());
    }

    public static synchronized boolean queueOutgoing(
            final String owner, final Connection connection, final Packet<?> packet, final int maxPackets) {
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
                OUTGOING.sampleTypes());
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
            schedule(entry.packet, listener);
        } catch (RejectedExecutionException ignored) {
            // The client is shutting down.
        }
    }

    private static void deliverOutgoing(final QueuedPacket entry) {
        PacketEventSilencer.runSilently(() -> entry.connection.send(entry.packet));
    }

    /**
     * Released packets always go through vanilla's packet queue, from any thread. Handling some directly and some
     * through the queue could apply an older packet after a newer one, and the queue also skips packets after a
     * disconnect and reports handler errors the vanilla way.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void schedule(final Packet packet, final PacketListener listener) {
        Minecraft.getInstance().packetProcessor().scheduleIfPossible(listener, packet);
    }

    /** One direction's queues, one per owner. Package-private for tests. */
    static final class DirectionQueue {

        private final Map<String, Deque<QueuedPacket>> owners = new LinkedHashMap<>();
        private final Consumer<QueuedPacket> delivery;

        DirectionQueue(final Consumer<QueuedPacket> delivery) {
            this.delivery = delivery;
        }

        boolean queue(
                final String owner,
                final Connection connection,
                final Packet<?> packet,
                final int maxPackets,
                final long nowMillis) {
            if (owner == null || owner.isBlank() || connection == null || packet == null) {
                return false;
            }
            Deque<QueuedPacket> entries = this.owners.computeIfAbsent(owner, key -> new ArrayDeque<>());
            int limit = Math.max(1, maxPackets);
            while (entries.size() >= limit) {
                this.delivery.accept(entries.pollFirst());
            }
            entries.addLast(new QueuedPacket(owner, connection, packet, nowMillis));
            return true;
        }

        /**
         * Delivers the owner's packets from the oldest one while {@code predicate} matches. Stopping at
         * the first packet that does not match keeps the owner's packets in order.
         */
        int release(final String owner, final Predicate<QueuedPacket> predicate) {
            Deque<QueuedPacket> entries = this.owners.get(owner);
            if (entries == null) {
                return 0;
            }
            int delivered = 0;
            while (!entries.isEmpty() && predicate.test(entries.peekFirst())) {
                this.delivery.accept(entries.pollFirst());
                delivered++;
            }
            if (entries.isEmpty()) {
                this.owners.remove(owner);
            }
            return delivered;
        }

        int drop(final String owner) {
            Deque<QueuedPacket> entries = this.owners.remove(owner);
            return entries == null ? 0 : entries.size();
        }

        int size(final String owner) {
            Deque<QueuedPacket> entries = this.owners.get(owner);
            return entries == null ? 0 : entries.size();
        }

        int totalSize() {
            int total = 0;
            for (Deque<QueuedPacket> entries : this.owners.values()) {
                total += entries.size();
            }
            return total;
        }

        private long oldestAgeMillis(final long nowMillis) {
            long oldest = Long.MAX_VALUE;
            for (Deque<QueuedPacket> entries : this.owners.values()) {
                oldest = Math.min(oldest, entries.peekFirst().createdAtMillis);
            }
            return oldest == Long.MAX_VALUE ? 0L : Math.max(0L, nowMillis - oldest);
        }

        private List<String> sampleTypes() {
            return this.owners.values().stream()
                    .flatMap(Deque::stream)
                    .sorted(Comparator.comparingLong(QueuedPacket::createdAtMillis))
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

        QueuedPacket(
                final String owner, final Connection connection, final Packet<?> packet, final long createdAtMillis) {
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

    public record Snapshot(
            int incomingSize,
            int outgoingSize,
            long oldestIncomingAgeMillis,
            long oldestOutgoingAgeMillis,
            List<String> incomingTypes,
            List<String> outgoingTypes) {}
}
