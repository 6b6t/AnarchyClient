package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.world.HoleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class HoleFillerModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final BooleanSetting support = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("support")
            .name("Support")
            .defaultValue(true)
            .build()));
    private final BooleanSetting onlyNearTarget = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("near_target")
            .name("Smart")
            .defaultValue(true)
            .build()));
    private final NumberSetting targetRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("target_range")
            .name("Target Range")
            .defaultValue(2.5)
            .min(0.5)
            .max(6.0)
            .step(0.5)
            .build()));
    private final BooleanSetting doubleHoles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("double_holes")
            .name("Double")
            .defaultValue(true)
            .build()));

    public HoleFillerModule() {
        super("hole_filler", "Hole Filler", ModuleCategory.COMBAT,
                "Fills nearby safe holes and double holes with hard blocks, by default only near an enemy.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        BlockPos center = player.blockPosition();
        Player target = this.onlyNearTarget.value()
                ? CombatTargets.nearestEnemy(client, this.range.value() + this.targetRange.value())
                : null;
        List<BlockPos> targets = fillTargets(client, center, this.range.value().intValue(), this.doubleHoles.value());
        if (target != null) {
            double targetRangeSqr = this.targetRange.value() * this.targetRange.value();
            targets = targets.stream()
                    .filter(pos -> target.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) <= targetRangeSqr)
                    .toList();
        } else if (this.onlyNearTarget.value()) {
            targets = List.of();
        }
        int placed = CombatPlacementPlanner.placeBatch(client, this, targets, CombatPlacementPlanner.HARD_BLOCKS,
                CombatPlacementPlanner.Options.of(this.blocksPerTick.value().intValue(), this.rotate.value(), this.range.value())
                        .withSupport(this.support.value())
                        .withAvoidSelf(true));
        this.debugValue("holes", targets.size());
        this.debugValue("placed", placed);
    }

    static List<BlockPos> fillTargets(final Minecraft client, final BlockPos center, final int radius,
                                      final boolean doubleHoles) {
        if (client.level == null) {
            return List.of();
        }
        List<BlockPos> positions = new ArrayList<>(HoleManager.nearbyHoles(client.level, center, radius));
        if (doubleHoles) {
            positions.addAll(HoleManager.nearbyDoubleHoleCells(client.level, center, radius));
        }
        return CombatPlacementPlanner.unique(positions);
    }

    static boolean isHole(final Minecraft client, final BlockPos pos) {
        return HoleManager.isHole(client.level, pos);
    }
}
