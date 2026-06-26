package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AutoCityModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(0.5)
            .build()));
    private final BooleanSetting includeEnderChests = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ender_chests")
            .name("EChests")
            .defaultValue(true)
            .build()));
    private final BooleanSetting mineBurrow = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("burrow")
            .name("Burrow")
            .defaultValue(true)
            .build()));
    private final BooleanSetting exposedOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("exposed_only")
            .name("Exposed")
            .defaultValue(true)
            .build()));

    public AutoCityModule() {
        super("auto_city", "Auto City", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        Player target = CombatTargets.nearestEnemy(client, this.range.value());
        if (target == null || client.level == null || client.player == null) {
            return;
        }
        CityTarget cityTarget = bestCityTarget(client, target, this.includeEnderChests.value(),
                this.mineBurrow.value(), this.exposedOnly.value());
        if (cityTarget != null) {
            WorldInteraction.breakBlock(client, cityTarget.pos(), cityTarget.face(), stack -> true);
            this.debugValue("target", target.getScoreboardName());
            this.debugValue("block", cityTarget.pos().toShortString());
        }
    }

    static CityTarget bestCityTarget(final Minecraft client, final Player target, final boolean includeEnderChests,
                                     final boolean mineBurrow, final boolean exposedOnly) {
        if (client.level == null || client.player == null) {
            return null;
        }
        List<CityTarget> targets = new ArrayList<>();
        BlockPos base = target.blockPosition();
        if (mineBurrow) {
            addTarget(client, targets, base, Direction.UP, includeEnderChests, exposedOnly);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            addTarget(client, targets, base.relative(direction), direction.getOpposite(), includeEnderChests, exposedOnly);
        }
        targets.sort(Comparator.comparingDouble(targetPos -> Vec3.atCenterOf(targetPos.pos()).distanceToSqr(client.player.getEyePosition())));
        return targets.isEmpty() ? null : targets.getFirst();
    }

    private static void addTarget(final Minecraft client, final List<CityTarget> targets, final BlockPos pos,
                                  final Direction face, final boolean includeEnderChests, final boolean exposedOnly) {
        BlockState state = client.level.getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && (!includeEnderChests || !state.is(Blocks.ENDER_CHEST))) {
            return;
        }
        if (exposedOnly && !hasBreakFace(client, pos)) {
            return;
        }
        targets.add(new CityTarget(pos.immutable(), face));
    }

    private static boolean hasBreakFace(final Minecraft client, final BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (client.level.getBlockState(neighbor).isAir()
                    || client.level.getBlockState(neighbor).canBeReplaced()) {
                return true;
            }
        }
        return false;
    }

    record CityTarget(BlockPos pos, Direction face) {
    }
}
