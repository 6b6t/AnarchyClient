package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;

public final class NukerModule extends Module {

    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(3.0)
            .min(1.0)
            .max(6.0)
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
    private final BooleanSetting excludeLiquids = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("exclude_liquids")
            .name("No Liquids")
            .defaultValue(true)
            .build()));

    public NukerModule() {
        super("nuker", "Nuker", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        int actions = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.radius.value().intValue(),
                this.radius.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                this.blocksPerTick.value().intValue() * 5,
                candidate -> !candidate.state().isAir()
                        && (!this.excludeLiquids.value() || candidate.state().getFluidState().isEmpty())
        )) {
            if (WorldInteraction.breakBlock(client, target.pos(), Direction.UP, stack -> true)
                    && ++actions >= this.blocksPerTick.value().intValue()) {
                return;
            }
        }
    }
}
