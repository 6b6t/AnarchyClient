package net.blockhost.anarchyclient.network;

import net.blockhost.anarchyclient.event.PacketEventSilencer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.function.Predicate;

public final class PacketQueueManager {

    private static final Queue<QueuedPacket> INCOMING = new ArrayDeque<>();
    private static final Queue<QueuedPacket> OUTGOING = new ArrayDeque<>();

    private PacketQueueManager() {
    }

    public static synchronized boolean queueIncoming(final Connection connection, final Packet<?> packet, final int maxPackets) {
        return queue(INCOMING, connection, packet, maxPackets);
    }

    public static synchronized boolean queueOutgoing(final Connection connection, final Packet<?> packet, final int maxPackets) {
        return queue(OUTGOING, connection, packet, maxPackets);
    }

    public static synchronized int flushOlderThan(final long ageMillis) {
        long now = System.currentTimeMillis();
        return flushIncoming(entry -> now - entry.createdAtMillis >= ageMillis)
                + flushOutgoing(entry -> now - entry.createdAtMillis >= ageMillis);
    }

    public static synchronized int flushAll() {
        return flushIncoming(entry -> true) + flushOutgoing(entry -> true);
    }

    public static synchronized int dropAll() {
        int count = INCOMING.size() + OUTGOING.size();
        INCOMING.clear();
        OUTGOING.clear();
        return count;
    }

    public static synchronized int incomingSize() {
        return INCOMING.size();
    }

    public static synchronized int outgoingSize() {
        return OUTGOING.size();
    }

    private static boolean queue(final Queue<QueuedPacket> queue, final Connection connection, final Packet<?> packet,
                                 final int maxPackets) {
        if (connection == null || packet == null || queue.size() >= Math.max(1, maxPackets)) {
            return false;
        }
        queue.add(new QueuedPacket(connection, packet, System.currentTimeMillis()));
        return true;
    }

    private static int flushIncoming(final Predicate<QueuedPacket> predicate) {
        int count = 0;
        Iterator<QueuedPacket> iterator = INCOMING.iterator();
        while (iterator.hasNext()) {
            QueuedPacket entry = iterator.next();
            if (!predicate.test(entry)) {
                continue;
            }
            iterator.remove();
            PacketListener listener = entry.connection.getPacketListener();
            if (listener != null) {
                handle(entry.packet, listener);
                count++;
            }
        }
        return count;
    }

    private static int flushOutgoing(final Predicate<QueuedPacket> predicate) {
        int count = 0;
        Iterator<QueuedPacket> iterator = OUTGOING.iterator();
        while (iterator.hasNext()) {
            QueuedPacket entry = iterator.next();
            if (!predicate.test(entry)) {
                continue;
            }
            iterator.remove();
            PacketEventSilencer.runSilently(() -> entry.connection.send(entry.packet));
            count++;
        }
        return count;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void handle(final Packet packet, final PacketListener listener) {
        packet.handle(listener);
    }

    private record QueuedPacket(Connection connection, Packet<?> packet, long createdAtMillis) {
    }
}
