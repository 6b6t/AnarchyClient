package net.blockhost.anarchyclient.util;

import net.minecraft.client.Minecraft;

/**
 * Moves work onto the client thread.
 *
 * <p>Packet receive events run on the Netty thread so modules can cancel or replace packets
 * before vanilla handles them. Use {@link #run(Runnable)} for any side effect of such an event
 * that touches the level, the player, the GUI, module state, or other client-thread data.
 */
public final class MainThread {

    private MainThread() {}

    /**
     * Runs the task now if the caller is on the client thread, otherwise queues it on the client
     * thread. Queued tasks run in submission order.
     */
    public static void run(final Runnable task) {
        Minecraft client = Minecraft.getInstance();
        if (client.isSameThread()) {
            task.run();
        } else {
            client.execute(task);
        }
    }

    /** Returns whether the caller is on the client thread. */
    public static boolean isClientThread() {
        return Minecraft.getInstance().isSameThread();
    }
}
