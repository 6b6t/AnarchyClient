package net.blockhost.anarchyclient.event;

public final class PacketEventSilencer {

    private static final ThreadLocal<Boolean> SILENT = ThreadLocal.withInitial(() -> false);

    private PacketEventSilencer() {
    }

    public static boolean silent() {
        return SILENT.get();
    }

    public static void runSilently(final Runnable runnable) {
        boolean previous = SILENT.get();
        SILENT.set(true);
        try {
            runnable.run();
        } finally {
            SILENT.set(previous);
        }
    }
}
