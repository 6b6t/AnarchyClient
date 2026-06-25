package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class SelfTrapModule extends Module {

    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));

    public SelfTrapModule() {
        super("self_trap", "Self Trap", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        int placed = 0;
        for (BlockPos pos : trapPositions(client.player.blockPosition())) {
            if (BlockPlacement.place(client, this, pos, true, 70.0F) == BlockPlacement.PlacementResult.PLACED
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
            positions.add(base.relative(direction));
        }
        return List.copyOf(positions);
    }
}
