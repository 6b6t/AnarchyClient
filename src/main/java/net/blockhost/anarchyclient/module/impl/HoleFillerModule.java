package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

public final class HoleFillerModule extends Module {

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
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public HoleFillerModule() {
        super("hole_filler", "Hole Filler", ModuleCategory.COMBAT);
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
            for (int z = center.getZ() - radius; z <= center.getZ() + radius; z++) {
                BlockPos pos = new BlockPos(x, center.getY(), z);
                if (pos.equals(center) || !isHole(client, pos)) {
                    continue;
                }
                if (BlockPlacement.place(client, this, pos, this.rotate.value(), 70.0F) == BlockPlacement.PlacementResult.PLACED
                        && ++placed >= this.blocksPerTick.value().intValue()) {
                    return;
                }
            }
        }
    }

    static boolean isHole(final Minecraft client, final BlockPos pos) {
        if (!client.level.getBlockState(pos).canBeReplaced()
                || !client.level.getBlockState(pos.above()).canBeReplaced()
                || !solidWall(client, pos.below())) {
            return false;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (!solidWall(client, pos.relative(direction))) {
                return false;
            }
        }
        return true;
    }

    private static boolean solidWall(final Minecraft client, final BlockPos pos) {
        return client.level.getBlockState(pos).is(Blocks.BEDROCK)
                || client.level.getBlockState(pos).is(Blocks.OBSIDIAN)
                || client.level.getBlockState(pos).is(Blocks.CRYING_OBSIDIAN)
                || client.level.getBlockState(pos).is(Blocks.ENDER_CHEST);
    }
}
