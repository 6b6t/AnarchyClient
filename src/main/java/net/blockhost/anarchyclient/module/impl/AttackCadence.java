package net.blockhost.anarchyclient.module.impl;

import java.util.Random;

final class AttackCadence {

    private final Random random;
    private int delayTicks;

    AttackCadence(final Random random) {
        this.random = random;
    }

    boolean ready() {
        if (this.delayTicks > 0) {
            this.delayTicks--;
            return false;
        }
        return true;
    }

    void reset(final double minCps, final double maxCps) {
        double lower = Math.min(minCps, maxCps);
        double upper = Math.max(minCps, maxCps);
        double cps = lower + this.random.nextDouble() * (upper - lower);
        this.delayTicks = Math.max(1, (int) Math.round(20.0 / cps));
    }

    void clear() {
        this.delayTicks = 0;
    }
}
