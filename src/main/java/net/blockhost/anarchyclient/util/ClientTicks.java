package net.blockhost.anarchyclient.util;

/** Counts client ticks, for timeouts that must follow game time rather than wall-clock time (lag spikes, pauses). */
public final class ClientTicks {

    private static long count;

    private ClientTicks() {
    }

    /** Called once at the start of every client tick. */
    public static void advance() {
        count++;
    }

    public static long current() {
        return count;
    }
}
