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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class WallHackModule extends Module {

    private final BlockListSetting blocks = this.setting(BlockListSetting.from(BlockListSetting.builder()
            .id("blocks")
            .name("Blocks")
            .addAllDefaultValue(List.of(
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
    private final Set<BlockPos> highlighted = new HashSet<>();
    private int cooldownTicks;

    public WallHackModule() {
        super("wall_hack", "Wall Hack", ModuleCategory.RENDER,
                "Highlights selected blocks such as diamond ore, ancient debris, and spawners through walls.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.level == null) {
            this.removeAllMarkers();
            this.cooldownTicks = 0;
            return;
        }
        Set<Block> targets = SetBackedBlocks.ids(this.blocks.value());
        // Mined or replaced blocks lose their highlight right away instead of waiting for the next scan.
        List<BlockPos> stale = stalePositions(this.highlighted, targets, pos -> client.level.getBlockState(pos).getBlock());
        stale.forEach(this::removeMarker);
        if (this.cooldownTicks-- > 0) {
            return;
        }
        int max = this.maxBlocks.value().intValue();
        Set<BlockPos> found = new HashSet<>(BlockScan.matchingBlocks(client, targets,
                this.range.value().intValue(), this.range.value().intValue(), max));
        List.copyOf(this.highlighted).stream()
                .filter(pos -> !found.contains(pos))
                .forEach(this::removeMarker);
        for (BlockPos pos : found) {
            MarkerManager.put(new CuboidMarker(markerId(pos), new AABB(pos), MarkerStyle.CYAN, 45));
            this.highlighted.add(pos);
        }
        this.cooldownTicks = 40;
    }

    @Override
    protected void onDisable() {
        this.removeAllMarkers();
        this.cooldownTicks = 0;
    }

    static List<BlockPos> stalePositions(final Collection<BlockPos> highlighted, final Set<Block> targets,
                                         final Function<BlockPos, Block> blockAt) {
        return highlighted.stream()
                .filter(pos -> !targets.contains(blockAt.apply(pos)))
                .toList();
    }

    private void removeAllMarkers() {
        List.copyOf(this.highlighted).forEach(this::removeMarker);
    }

    private void removeMarker(final BlockPos pos) {
        MarkerManager.remove(markerId(pos));
        this.highlighted.remove(pos);
    }

    private static String markerId(final BlockPos pos) {
        return "wall_hack:" + pos.asLong();
    }
}
