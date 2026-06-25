package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class HighwayBuilderModule extends Module {

    private final NumberSetting length = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("length")
            .name("Length")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rails = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rails")
            .name("Rails")
            .defaultValue(true)
            .build()));

    public HighwayBuilderModule() {
        super("highway_builder", "Highway Builder", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null) {
            return;
        }
        for (BlockPos pos : plan(client.player.blockPosition(), client.player.getDirection(), this.length.value().intValue(), this.rails.value())) {
            if (BlockPlacement.needsPlacement(client.level, pos)) {
                BlockPlacement.place(client, this, pos, true, 70.0F);
                return;
            }
        }
    }

    static List<BlockPos> plan(final BlockPos origin, final Direction forward, final int length, final boolean rails) {
        Direction side = forward.getClockWise();
        List<BlockPos> positions = new ArrayList<>();
        for (int step = 1; step <= Math.max(1, length); step++) {
            BlockPos base = origin.relative(forward, step).below();
            positions.add(base);
            if (rails) {
                positions.add(base.relative(side));
                positions.add(base.relative(side.getOpposite()));
            }
        }
        return List.copyOf(positions);
    }
}
