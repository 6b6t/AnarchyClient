package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class MossBotModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting cooldownTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("cooldown_ticks")
            .name("Cooldown")
            .defaultValue(100.0)
            .min(20.0)
            .max(300.0)
            .step(10.0)
            .build()));
    private final NumberSetting minReplaceableBlocks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_replaceable_blocks")
            .name("Min Blocks")
            .defaultValue(10.0)
            .min(1.0)
            .max(32.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private final Map<BlockPos, Integer> cooldowns = new HashMap<>();

    public MossBotModule() {
        super("moss_bot", "Moss Bot", ModuleCategory.WORLD);
    }

    @Override
    protected void onDisable() {
        this.cooldowns.clear();
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        this.cooldowns.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
        if (!WorldInteraction.hasHotbarItem(player.getInventory(), stack -> stack.is(Items.BONE_MEAL))) {
            return;
        }
        BlockPos best = bestMossBlock(client, this.range.value().intValue(), this.minReplaceableBlocks.value().intValue(), this.cooldowns);
        if (best == null) {
            return;
        }
        if (!client.level.isEmptyBlock(best.above())) {
            WorldInteraction.breakBlock(client, best.above(), Direction.UP, stack -> true);
            return;
        }
        if (WorldInteraction.useOnBlock(client, this, best, Direction.UP, stack -> stack.is(Items.BONE_MEAL), this.rotate.value())
                == WorldInteraction.ActionResult.DONE) {
            this.cooldowns.put(best, this.cooldownTicks.value().intValue());
        }
    }

    static BlockPos bestMossBlock(final Minecraft client, final int range, final int minReplaceableBlocks,
                                  final Map<BlockPos, Integer> cooldowns) {
        return BlockTargetScanner.scan(client, range, range, BlockTargetScanner.SortMode.CLOSEST, 64,
                        target -> !cooldowns.containsKey(target.pos())
                                && isMoss(target.state())
                                && replaceableAround(client, target.pos()) >= minReplaceableBlocks)
                .stream()
                .max(java.util.Comparator.comparingInt(target -> replaceableAround(client, target.pos())))
                .map(BlockTargetScanner.BlockTarget::pos)
                .orElse(null);
    }

    private static boolean isMoss(final BlockState state) {
        return state.is(Blocks.MOSS_BLOCK) || state.is(Blocks.PALE_MOSS_BLOCK);
    }

    static int replaceableAround(final Minecraft client, final BlockPos pos) {
        if (client.level == null || !client.level.isEmptyBlock(pos.above())) {
            return 0;
        }
        int count = 0;
        for (BlockPos candidate : BlockPos.withinManhattan(pos, 3, 4, 3)) {
            if (client.level.getBlockState(candidate).is(BlockTags.MOSS_REPLACEABLE)
                    && client.level.isEmptyBlock(candidate.above())) {
                count++;
            }
        }
        return count;
    }
}
