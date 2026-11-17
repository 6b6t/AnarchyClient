package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

final class TotemRiskEstimator {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private static final double TNT_POWER = 4.0;
    private static final double CREEPER_POWER = 3.0;

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
            double power = explosionPower(entity);
            if (power <= 0.0 || player.distanceToSqr(entity) > rangeSqr) {
                continue;
            }
            predictedDamage = Math.max(predictedDamage,
                    DamageEstimator.explosionDamage(player, explosionCenter(entity), power));
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

    /**
     * Generic distance falloff kept for {@code AutoTotemModule.estimateExplosionDamage}. Real
     * predictions use {@link DamageEstimator}.
     */
    static double estimateExplosionDamage(final double distanceSqr, final double range, final double maxDamage) {
        if (range <= 0.0 || maxDamage <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(Math.max(0.0, distanceSqr));
        double exposure = Math.max(0.0, 1.0 - distance / range);
        return ((exposure * exposure + exposure) / 2.0) * maxDamage;
    }

    /** Explosion power vanilla uses when this entity explodes, or 0 if it cannot explode. */
    static double explosionPower(final Entity entity) {
        if (!entity.isAlive()) {
            return 0.0;
        }
        if (entity instanceof EndCrystal) {
            return DamageEstimator.CRYSTAL_POWER;
        }
        if (entity instanceof PrimedTnt) {
            return TNT_POWER;
        }
        if (entity instanceof Creeper creeper) {
            return creeper.isPowered() ? CREEPER_POWER * 2.0 : CREEPER_POWER;
        }
        return 0.0;
    }

    private static Vec3 explosionCenter(final Entity entity) {
        if (entity instanceof PrimedTnt) {
            return new Vec3(entity.getX(), entity.getY(0.0625), entity.getZ());
        }
        return entity.position();
    }
}
