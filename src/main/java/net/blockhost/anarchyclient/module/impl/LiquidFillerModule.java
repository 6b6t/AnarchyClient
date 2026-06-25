package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class LiquidFillerModule extends Module {

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
    private final BooleanSetting water = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("water")
            .name("Water")
            .defaultValue(true)
            .build()));
    private final BooleanSetting lava = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("lava")
            .name("Lava")
            .defaultValue(true)
            .build()));
    private final BooleanSetting preferSponge = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sponge")
            .name("Sponge")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public LiquidFillerModule() {
        super("liquid_filler", "Liquid Filler", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int placed = 0;
        int radius = this.range.value().intValue();
        BlockPos center = player.blockPosition();
        for (int x = center.getX() - radius; x <= center.getX() + radius; x++) {
            for (int y = center.getY() - radius; y <= center.getY() + radius; y++) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!this.shouldFill(client.level.getFluidState(pos))) {
                        continue;
                    }
                    if (BlockPlacer.place(client, this.id(), pos, this.rotate.value(), 70.0F,
                            stack -> !this.preferSponge.value() || stack.is(Items.SPONGE) || stack.is(Items.WET_SPONGE))
                            == BlockPlacer.PlacementResult.PLACED && ++placed >= this.blocksPerTick.value().intValue()) {
                        return;
                    }
                }
            }
        }
    }

    private boolean shouldFill(final FluidState fluid) {
        return fluid.isSource()
                && (this.water.value() && fluid.getType() == Fluids.WATER
                || this.lava.value() && fluid.getType() == Fluids.LAVA);
    }
}
