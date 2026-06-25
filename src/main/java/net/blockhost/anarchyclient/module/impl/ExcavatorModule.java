package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class ExcavatorModule extends Module {

    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .addAllDefaultValue(List.of(Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.GRAVEL, Blocks.SAND))
            .build()));
    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(3.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(12.0)
            .step(1.0)
            .build()));

    public ExcavatorModule() {
        super("excavator", "Excavator", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        int actions = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.radius.value().intValue(),
                this.radius.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                this.blocksPerTick.value().intValue() * 4,
                candidate -> this.blocks.value().contains(candidate.state().getBlock())
        )) {
            if (WorldInteraction.breakBlock(client, target.pos(), Direction.UP, stack -> true)
                    && ++actions >= this.blocksPerTick.value().intValue()) {
                return;
            }
        }
    }
}
