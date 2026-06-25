package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class SurroundModule extends Module {

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

    public SurroundModule() {
        super("surround", "Surround", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int placed = 0;
        for (BlockPos pos : targetPositions(player.blockPosition())) {
            if (BlockPlacement.place(client, this, pos, this.rotate.value(), 70.0F) == BlockPlacement.PlacementResult.PLACED
                    && ++placed >= this.blocksPerTick.value().intValue()) {
                return;
            }
        }
    }

    static List<BlockPos> targetPositions(final BlockPos base) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(base.below());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            positions.add(base.relative(direction));
        }
        return positions;
    }
}
