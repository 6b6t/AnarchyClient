package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;

/**
 * Estimates explosion damage with the vanilla formulas from {@code ExplosionDamageCalculator},
 * {@code ServerExplosion}, {@code Player.hurtServer}, and {@code CombatRules}.
 *
 * <p>Enchantment protection is not included because the client cannot evaluate it.
 */
public final class DamageEstimator {

    /** Explosion power of an end crystal. */
    public static final double CRYSTAL_POWER = 6.0;
    /** Explosion power of a respawn anchor outside the Nether. */
    public static final double ANCHOR_POWER = 5.0;
    /** Explosion power of a bed outside the Overworld. */
    public static final double BED_POWER = 5.0;

    private DamageEstimator() {
    }

    /**
     * Returns the damage an explosion of the given power at {@code center} deals to the entity,
     * including terrain exposure, difficulty scaling for players, armor, and Resistance.
     */
    public static double explosionDamage(final LivingEntity entity, final Vec3 center, final double power) {
        if (entity == null || center == null || power <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(entity.distanceToSqr(center));
        if (distance > power * 2.0) {
            return 0.0;
        }
        double exposure = ServerExplosion.getSeenPercent(center, entity);
        return applyDefenses(entity, rawDamage(distance, power, exposure));
    }

    /** Same as {@link #explosionDamage} but assumes no blocks shield the entity. */
    public static double explosionDamageIgnoringTerrain(final LivingEntity entity, final Vec3 center, final double power) {
        if (entity == null || center == null || power <= 0.0) {
            return 0.0;
        }
        return applyDefenses(entity, rawDamage(Math.sqrt(entity.distanceToSqr(center)), power, 1.0));
    }

    /** Explosion center of an end crystal standing in {@code crystalPos} (the block above the obsidian). */
    public static Vec3 crystalCenter(final BlockPos crystalPos) {
        return Vec3.atBottomCenterOf(crystalPos);
    }

    /** Explosion center of a respawn anchor or bed block. */
    public static Vec3 blockCenter(final BlockPos pos) {
        return Vec3.atCenterOf(pos);
    }

    /**
     * Vanilla {@code ExplosionDamageCalculator.getEntityDamageAmount}: damage before difficulty,
     * armor, and effects. Returns 0 when the entity is farther than {@code 2 * power}.
     */
    public static double rawDamage(final double distance, final double power, final double exposure) {
        if (power <= 0.0) {
            return 0.0;
        }
        double doubleRadius = power * 2.0;
        double dist = distance / doubleRadius;
        if (dist > 1.0) {
            return 0.0;
        }
        double pow = (1.0 - dist) * Mth.clamp(exposure, 0.0, 1.0);
        return (pow * pow + pow) / 2.0 * 7.0 * doubleRadius + 1.0;
    }

    /** Vanilla {@code Player.hurtServer} difficulty scaling for damage that scales with difficulty. */
    public static double scaleForDifficulty(final double damage, final Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0.0;
            case EASY -> Math.min(damage / 2.0 + 1.0, damage);
            case NORMAL -> damage;
            case HARD -> damage * 3.0 / 2.0;
        };
    }

    /** Vanilla {@code CombatRules.getDamageAfterAbsorb} without weapon enchantments. */
    public static double afterArmor(final double damage, final double armor, final double armorToughness) {
        double toughness = 2.0 + armorToughness / 4.0;
        double realArmor = Mth.clamp(armor - damage / toughness, armor * 0.2, 20.0);
        return damage * (1.0 - realArmor / 25.0);
    }

    /** Vanilla Resistance reduction; {@code amplifier} is -1 when the effect is absent. */
    public static double afterResistance(final double damage, final int amplifier) {
        if (amplifier < 0) {
            return damage;
        }
        int absorb = 25 - (amplifier + 1) * 5;
        return Math.max(damage * absorb / 25.0, 0.0);
    }

    private static double applyDefenses(final LivingEntity entity, final double rawDamage) {
        if (rawDamage <= 0.0) {
            return 0.0;
        }
        double damage = rawDamage;
        if (entity instanceof Player) {
            damage = scaleForDifficulty(damage, entity.level().getDifficulty());
        }
        damage = afterArmor(damage, entity.getArmorValue(), entity.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        MobEffectInstance resistance = entity.getEffect(MobEffects.RESISTANCE);
        return afterResistance(damage, resistance == null ? -1 : resistance.getAmplifier());
    }
}
