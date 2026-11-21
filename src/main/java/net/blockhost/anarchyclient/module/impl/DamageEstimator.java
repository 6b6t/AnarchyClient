package net.blockhost.anarchyclient.module.impl;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

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

    private DamageEstimator() {}

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

    /**
     * Same as {@link #explosionDamage} but treats {@code removedBlocks} as air. Respawn anchors and
     * beds remove themselves before they explode, so the block that explodes must not shield the
     * entity from its own explosion.
     */
    public static double explosionDamage(
            final LivingEntity entity, final Vec3 center, final double power, final Set<BlockPos> removedBlocks) {
        if (entity == null || center == null || power <= 0.0) {
            return 0.0;
        }
        double distance = Math.sqrt(entity.distanceToSqr(center));
        if (distance > power * 2.0) {
            return 0.0;
        }
        double exposure = seenPercent(
                new RemovedBlocksGetter(entity.level(), removedBlocks),
                center,
                entity.getBoundingBox(),
                CollisionContext.of(entity));
        return applyDefenses(entity, rawDamage(distance, power, exposure));
    }

    /**
     * Vanilla {@code ServerExplosion.getSeenPercent} against an arbitrary block view: the share of
     * sample points in {@code box} with an unobstructed collider line to {@code center}.
     */
    static float seenPercent(
            final BlockGetter level, final Vec3 center, final AABB box, final CollisionContext context) {
        double xs = 1.0 / ((box.maxX - box.minX) * 2.0 + 1.0);
        double ys = 1.0 / ((box.maxY - box.minY) * 2.0 + 1.0);
        double zs = 1.0 / ((box.maxZ - box.minZ) * 2.0 + 1.0);
        double xOffset = (1.0 - Math.floor(1.0 / xs) * xs) / 2.0;
        double zOffset = (1.0 - Math.floor(1.0 / zs) * zs) / 2.0;
        if (xs < 0.0 || ys < 0.0 || zs < 0.0) {
            return 0.0F;
        }
        int hits = 0;
        int count = 0;
        for (double xx = 0.0; xx <= 1.0; xx += xs) {
            for (double yy = 0.0; yy <= 1.0; yy += ys) {
                for (double zz = 0.0; zz <= 1.0; zz += zs) {
                    double x = Mth.lerp(xx, box.minX, box.maxX);
                    double y = Mth.lerp(yy, box.minY, box.maxY);
                    double z = Mth.lerp(zz, box.minZ, box.maxZ);
                    Vec3 from = new Vec3(x + xOffset, y, z + zOffset);
                    ClipContext clip =
                            new ClipContext(from, center, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context);
                    if (level.clip(clip).getType() == HitResult.Type.MISS) {
                        hits++;
                    }
                    count++;
                }
            }
        }
        return (float) hits / count;
    }

    /** Same as {@link #explosionDamage} but assumes no blocks shield the entity. */
    public static double explosionDamageIgnoringTerrain(
            final LivingEntity entity, final Vec3 center, final double power) {
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

    /** A block view that reports air at the removed positions and delegates everything else. */
    record RemovedBlocksGetter(BlockGetter level, Set<BlockPos> removed) implements BlockGetter {

        @Override
        public @Nullable BlockEntity getBlockEntity(final BlockPos pos) {
            return this.removed.contains(pos) ? null : this.level.getBlockEntity(pos);
        }

        @Override
        public BlockState getBlockState(final BlockPos pos) {
            return this.removed.contains(pos) ? Blocks.AIR.defaultBlockState() : this.level.getBlockState(pos);
        }

        @Override
        public FluidState getFluidState(final BlockPos pos) {
            return this.removed.contains(pos)
                    ? Blocks.AIR.defaultBlockState().getFluidState()
                    : this.level.getFluidState(pos);
        }

        @Override
        public int getHeight() {
            return this.level.getHeight();
        }

        @Override
        public int getMinY() {
            return this.level.getMinY();
        }
    }
}
