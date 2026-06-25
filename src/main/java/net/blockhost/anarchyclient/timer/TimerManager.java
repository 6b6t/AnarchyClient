package net.blockhost.anarchyclient.timer;

import net.blockhost.anarchyclient.request.TimedRequestQueue;

import java.util.Optional;

public final class TimerManager {

    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_MOVEMENT = 50;
    public static final int PRIORITY_COMBAT = 75;

    private static final TimedRequestQueue<TimerRequest> REQUESTS = new TimedRequestQueue<>();

    private TimerManager() {
    }

    public static void request(final String owner, final double multiplier, final int priority, final int ticksUntilReset) {
        REQUESTS.request(owner, priority, ticksUntilReset, new TimerRequest(clampMultiplier(multiplier)));
    }

    public static double multiplier() {
        return REQUESTS.activeValue().map(TimerRequest::multiplier).orElse(1.0);
    }

    public static float adjustMspt(final float defaultMspt) {
        return (float) (defaultMspt / multiplier());
    }

    public static Optional<Double> activeMultiplier() {
        return REQUESTS.activeValue().map(TimerRequest::multiplier);
    }

    public static void tick() {
        REQUESTS.tick();
    }

    public static void clear(final String owner) {
        REQUESTS.clear(owner);
    }

    public static void clearAll() {
        REQUESTS.clearAll();
    }

    static double clampMultiplier(final double multiplier) {
        if (!Double.isFinite(multiplier)) {
            return 1.0;
        }
        return Math.max(0.1, Math.min(10.0, multiplier));
    }

    private record TimerRequest(double multiplier) {
    }
}
