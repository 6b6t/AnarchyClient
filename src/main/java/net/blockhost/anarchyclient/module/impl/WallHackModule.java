package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.CuboidMarker;
import net.blockhost.anarchyclient.render.MarkerManager;
import net.blockhost.anarchyclient.render.MarkerStyle;
import net.blockhost.anarchyclient.setting.BlockListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class WallHackModule extends Module {

    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue(List.of(
                    Blocks.DIAMOND_ORE,
                    Blocks.DEEPSLATE_DIAMOND_ORE,
                    Blocks.EMERALD_ORE,
                    Blocks.DEEPSLATE_EMERALD_ORE,
                    Blocks.ANCIENT_DEBRIS,
                    Blocks.SPAWNER
            ))
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(48.0)
            .min(8.0)
            .max(96.0)
            .step(4.0)
            .build()));
    private final NumberSetting maxBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_blocks")
            .name("Max")
            .defaultValue(128.0)
            .min(16.0)
            .max(512.0)
            .step(16.0)
            .build()));
    private int cooldownTicks;

    public WallHackModule() {
        super("wall_hack", "Wall Hack", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldownTicks-- > 0) {
            return;
        }
        int max = this.maxBlocks.value().intValue();
        for (BlockPos pos : BlockScan.matchingBlocks(client, SetBackedBlocks.ids(this.blocks.value()),
                this.range.value().intValue(), this.range.value().intValue(), max)) {
            MarkerManager.put(new CuboidMarker("wall_hack:" + pos.asLong(), new AABB(pos), MarkerStyle.CYAN, 45));
        }
        this.cooldownTicks = 40;
    }

    @Override
    protected void onDisable() {
        MarkerManager.markers().stream()
                .map(marker -> marker.id())
                .filter(id -> id.startsWith("wall_hack:"))
                .forEach(MarkerManager::remove);
    }
}
