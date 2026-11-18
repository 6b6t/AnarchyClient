package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Estimates how fast a block breaks with a tool the player is not holding yet.
 *
 * <p>The server checks a finished break with the tool held when the finish packet arrives, so
 * modules that swap tools only for that packet need the speed of the tool they will swap to.
 * This mirrors vanilla's {@code Player#getDestroySpeed} with the tool's own efficiency level
 * in place of the held item's attribute.
 */
final class MiningProgress {

    private MiningProgress() {
    }

    /** Returns the progress one tick of mining adds with {@code tool}, where 1.0 is a full break. */
    static float perTick(final Player player, final BlockGetter level, final BlockPos pos, final BlockState state,
                         final ItemStack tool) {
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0.0F) {
            return 0.0F;
        }
        if (hardness == 0.0F) {
            return 1.0F;
        }
        float speed = toolSpeed(tool, state, efficiencyLevel(player, tool));
        if (MobEffectUtil.hasDigSpeed(player)) {
            speed *= 1.0F + (MobEffectUtil.getDigSpeedAmplification(player) + 1) * 0.2F;
        }
        if (player.hasEffect(MobEffects.MINING_FATIGUE)) {
            speed *= (float) Math.pow(0.3, player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier() + 1);
        }
        speed *= (float) player.getAttributeValue(Attributes.BLOCK_BREAK_SPEED);
        if (player.isEyeInFluid(FluidTags.WATER)) {
            speed *= (float) player.getAttributeValue(Attributes.SUBMERGED_MINING_SPEED);
        }
        if (!player.onGround()) {
            speed /= 5.0F;
        }
        return speed / hardness / (canHarvest(tool, state) ? 30.0F : 100.0F);
    }

    /** The tool's base speed plus the efficiency bonus vanilla adds for tools faster than a hand. */
    static float toolSpeed(final ItemStack tool, final BlockState state, final int efficiencyLevel) {
        float speed = tool.getDestroySpeed(state);
        if (speed > 1.0F && efficiencyLevel > 0) {
            speed += efficiencyLevel * efficiencyLevel + 1;
        }
        return speed;
    }

    static boolean canHarvest(final ItemStack tool, final BlockState state) {
        return !state.requiresCorrectToolForDrops() || tool.isCorrectToolForDrops(state);
    }

    /**
     * Returns whether the server accepts a finish packet sent now, after {@code ticksMined} ticks
     * since the start packet, when one tick adds {@code perTick} progress.
     */
    static boolean serverAccepts(final float perTick, final int ticksMined) {
        return perTick * (ticksMined + 1) >= SpeedMineModule.SERVER_BREAK_THRESHOLD;
    }

    private static int efficiencyLevel(final Player player, final ItemStack tool) {
        if (tool.isEmpty()) {
            return 0;
        }
        Holder<Enchantment> efficiency = player.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.EFFICIENCY).orElse(null);
        return efficiency == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(efficiency, tool);
    }
}
