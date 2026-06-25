package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;

final class TotemRiskEstimator {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private TotemRiskEstimator() {
    }

    static float effectiveHealth(final LocalPlayer player, final boolean includeAbsorption) {
        float health = player.getHealth();
        if (includeAbsorption) {
            health += player.getAbsorptionAmount();
        }
        return health;
    }

    static double damageUntilThreshold(final double health, final double threshold) {
        return Math.max(0.0, health - threshold);
    }

    static boolean missingArmor(final LocalPlayer player) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (player.getItemBySlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static double predictedExplosionDamage(final Iterable<? extends Entity> entities, final LocalPlayer player,
                                           final double range) {
        double predictedDamage = 0.0;
        double rangeSqr = range * range;
        for (Entity entity : entities) {
            double maxDamage = maxExplosionDamage(entity);
            if (maxDamage <= 0.0) {
                continue;
            }
            double distanceSqr = player.distanceToSqr(entity);
            if (distanceSqr <= rangeSqr) {
                predictedDamage = Math.max(predictedDamage, estimateExplosionDamage(distanceSqr, range, maxDamage));
            }
        }
        return predictedDamage;
    }

    static boolean needsTotem(final LocalPlayer player, final ClientLevel level, final boolean includeAbsorption,
                              final boolean missingArmorTotem, final double healthThreshold,
                              final double fallDistanceThreshold, final boolean fireTotem,
                              final boolean predictExplosions, final double explosionRange) {
        float health = effectiveHealth(player, includeAbsorption);
        if (health <= healthThreshold) {
            return true;
        }
        if (missingArmorTotem && missingArmor(player)) {
            return true;
        }
        if (player.fallDistance >= fallDistanceThreshold) {
            return true;
        }
        if (fireTotem && player.isOnFire()) {
            return true;
        }
        if (!predictExplosions || level == null) {
            return false;
        }
        double predictedDamage = predictedExplosionDamage(level.entitiesForRendering(), player, explosionRange);
        return predictedDamage >= damageUntilThreshold(health, healthThreshold);
    }

    static double estimateExplosionDamage(final double distanceSqr, final double range, final double maxDamage) {
        if (range <= 0.0 || maxDamage <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(Math.max(0.0, distanceSqr));
        double exposure = Math.max(0.0, 1.0 - distance / range);
        return ((exposure * exposure + exposure) / 2.0) * maxDamage;
    }

    private static double maxExplosionDamage(final Entity entity) {
        if (!entity.isAlive()) {
            return 0.0;
        }
        if (entity instanceof EndCrystal || entity instanceof PrimedTnt) {
            return 20.0;
        }
        if (entity instanceof Creeper) {
            return 18.0;
        }
        return 0.0;
    }
}
