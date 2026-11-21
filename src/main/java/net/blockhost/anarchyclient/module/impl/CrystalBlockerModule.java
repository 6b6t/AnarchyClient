package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.ToDoubleFunction;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class CrystalBlockerModule extends Module {

    private final NumberSetting minDamage = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_damage")
            .name("Min Damage")
            .defaultValue(6.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .description("Blocks crystal spots that could deal at least this much damage to you, in HP.")
            .build()));
    private final NumberSetting enemyRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("enemy_range")
            .name("Enemy Range")
            .defaultValue(10.0)
            .min(0.0)
            .max(24.0)
            .step(1.0)
            .description("Only works while an enemy is this close. 0 works all the time.")
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(1.0)
            .min(1.0)
            .max(4.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public CrystalBlockerModule() {
        super(
                "crystal_blocker",
                "Crystal Blocker",
                ModuleCategory.COMBAT,
                "Fills the open spots around you where an enemy could place an end crystal that would hurt you, "
                        + "most dangerous spots first. Uses obsidian or ender chests.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        double range = this.enemyRange.value();
        if (range > 0.0 && CombatTargets.nearestEnemy(client, range) == null) {
            return;
        }
        List<BlockPos> spots = dangerousSpots(
                client.level,
                player.blockPosition(),
                spot -> DamageEstimator.explosionDamage(
                        player, DamageEstimator.crystalCenter(spot), DamageEstimator.CRYSTAL_POWER),
                this.minDamage.value());
        this.debugValue("spots", spots.size());
        if (spots.isEmpty()) {
            return;
        }
        CombatPlacementPlanner.placeBatch(
                client,
                this,
                spots,
                CombatPlacementPlanner.HARD_BLOCKS,
                CombatPlacementPlanner.Options.of(
                                this.blocksPerTick.value().intValue(),
                                this.rotate.value(),
                                player.blockInteractionRange())
                        .withSupport(false));
    }

    /**
     * Returns the open crystal spots within two blocks of {@code feet} whose damage meets
     * {@code minDamage}, most damaging first. A spot is open air resting on obsidian or bedrock.
     * The player's own cells are never returned.
     */
    static List<BlockPos> dangerousSpots(
            final BlockGetter level,
            final BlockPos feet,
            final ToDoubleFunction<BlockPos> damage,
            final double minDamage) {
        List<BlockPos> spots = new ArrayList<>();
        List<Double> damages = new ArrayList<>();
        for (int dy = -1; dy <= 2; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos spot = feet.offset(dx, dy, dz);
                    if (dx == 0 && dz == 0 && (dy == 0 || dy == 1)) {
                        continue;
                    }
                    if (!isCrystalSpot(level, spot)) {
                        continue;
                    }
                    double value = damage.applyAsDouble(spot);
                    if (value >= minDamage) {
                        spots.add(spot.immutable());
                        damages.add(value);
                    }
                }
            }
        }
        List<Integer> order = new ArrayList<>();
        for (int index = 0; index < spots.size(); index++) {
            order.add(index);
        }
        order.sort(Comparator.comparingDouble(index -> -damages.get(index)));
        return order.stream().map(spots::get).toList();
    }

    /** Mirrors vanilla's End Crystal item: an air cell on obsidian or bedrock. The cell above is not checked. */
    static boolean isCrystalSpot(final BlockGetter level, final BlockPos spot) {
        BlockState base = level.getBlockState(spot.below());
        return (base.is(Blocks.OBSIDIAN) || base.is(Blocks.BEDROCK))
                && level.getBlockState(spot).isAir();
    }
}
