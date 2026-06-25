package net.blockhost.anarchyclient.timer;

import java.util.LinkedHashMap;
import java.util.Map;

public final class TimerBalanceService {

    private static final Map<String, Balance> BALANCES = new LinkedHashMap<>();

    private TimerBalanceService() {
    }

    public static synchronized double tick(final String owner, final double maxBalance, final double recovery,
                                           final double requestedMultiplier) {
        Balance previous = BALANCES.getOrDefault(owner, new Balance(maxBalance));
        double max = Math.max(0.0, maxBalance);
        double recovered = Math.min(max, previous.value() + Math.max(0.0, recovery));
        double cost = Math.max(0.0, requestedMultiplier - 1.0);
        double next = Math.max(0.0, recovered - cost);
        BALANCES.put(owner, new Balance(next));
        return next;
    }

    public static synchronized double value(final String owner) {
        return BALANCES.getOrDefault(owner, new Balance(0.0)).value();
    }

    public static synchronized boolean canSpend(final String owner, final double cost) {
        return value(owner) >= Math.max(0.0, cost);
    }

    public static synchronized void clear(final String owner) {
        BALANCES.remove(owner);
    }

    public static synchronized void clearAll() {
        BALANCES.clear();
    }

    private record Balance(double value) {
    }
}
