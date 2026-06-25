package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

public final class InfinityMinerModule extends Module {

    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .addAllDefaultValue(List.of(Blocks.STONE, Blocks.DEEPSLATE, Blocks.NETHERRACK))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));

    public InfinityMinerModule() {
        super("infinity_miner", "Infinity Miner", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.range.value().intValue(),
                this.range.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                8,
                candidate -> this.blocks.value().contains(candidate.state().getBlock())
        )) {
            if (WorldInteraction.breakBlock(client, target.pos(), Direction.UP, stack -> true)) {
                return;
            }
        }
    }
}
