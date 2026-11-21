package net.blockhost.anarchyclient.module.impl;

import java.util.Random;
import net.minecraft.client.player.LocalPlayer;

final class KillAuraTiming {

    private final AttackCadence attackCadence;

    KillAuraTiming() {
        this(new Random());
    }

    KillAuraTiming(final Random random) {
        this.attackCadence = new AttackCadence(random);
    }

    boolean readyToAttack(final LocalPlayer player, final double minCharge) {
        return this.attackCadence.ready() && player.getAttackStrengthScale(0.0F) >= minCharge;
    }

    void markAttack(final double minCps, final double maxCps) {
        this.attackCadence.reset(minCps, maxCps);
    }

    void clear() {
        this.attackCadence.clear();
    }
}
