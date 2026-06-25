package net.blockhost.anarchyclient.module.impl;

public record SelfDamagePolicy(double maxSelfDamage, double minTargetDamage, double minDamageRatio) {

    public boolean allows(final double selfDamage, final double targetDamage) {
        if (targetDamage < this.minTargetDamage || selfDamage > this.maxSelfDamage) {
            return false;
        }
        if (selfDamage <= 0.0) {
            return true;
        }
        return targetDamage / selfDamage >= this.minDamageRatio;
    }
}
