package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public final class AutoTrapModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.5)
            .min(1.0)
            .max(6.0)
            .step(0.5)
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

    public AutoTrapModule() {
        super("auto_trap", "Auto Trap", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        Player target = nearestPlayer(client, player, this.range.value());
        if (target == null) {
            return;
        }
        int placed = 0;
        for (BlockPos pos : trapPositions(target.blockPosition())) {
            if (BlockPlacement.place(client, this, pos, this.rotate.value(), 70.0F) == BlockPlacement.PlacementResult.PLACED
                    && ++placed >= this.blocksPerTick.value().intValue()) {
                return;
            }
        }
    }

    static List<BlockPos> trapPositions(final BlockPos base) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(base.above(2));
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            positions.add(base.above().relative(direction));
        }
        return positions;
    }

    private static Player nearestPlayer(final Minecraft client, final LocalPlayer player, final double range) {
        double rangeSqr = range * range;
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof Player target) || target == player || !target.isAlive()) {
                continue;
            }
            double distance = target.distanceToSqr(player);
            if (distance <= rangeSqr && distance < bestDistance) {
                best = target;
                bestDistance = distance;
            }
        }
        return best;
    }
}
