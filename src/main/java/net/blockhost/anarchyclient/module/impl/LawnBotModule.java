package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Set;

public final class LawnBotModule extends Module {

    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(10.0)
            .step(1.0)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting useShovel = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("use_shovel")
            .name("Shovel")
            .defaultValue(true)
            .build()));
    private final Set<BlockPos> pendingGrass = new LinkedHashSet<>();
    private int delay;

    public LawnBotModule() {
        super("lawn_bot", "Lawn Bot", ModuleCategory.WORLD);
    }

    @Override
    protected void onDisable() {
        this.pendingGrass.clear();
        this.delay = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.delay > 0) {
            this.delay--;
            return;
        }
        if (!WorldInteraction.hasHotbarItem(player.getInventory(), stack -> stack.is(Items.GRASS_BLOCK))) {
            return;
        }
        if (this.placePendingGrass(client)) {
            this.delay = this.delayTicks.value().intValue();
            return;
        }
        BlockPos target = findReplaceable(client, this.range.value().intValue());
        if (target == null) {
            return;
        }
        if (WorldInteraction.breakBlock(client, target, Direction.UP,
                stack -> !this.useShovel.value() || stack.getItem() instanceof ShovelItem)) {
            this.pendingGrass.add(target);
            this.delay = this.delayTicks.value().intValue();
        }
    }

    private boolean placePendingGrass(final Minecraft client) {
        if (client.level == null) {
            return false;
        }
        this.pendingGrass.removeIf(pos -> !client.level.isLoaded(pos) || !client.level.isEmptyBlock(pos) && !isReplaceable(client, pos));
        Iterator<BlockPos> iterator = this.pendingGrass.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (client.level.isEmptyBlock(pos) && !client.level.isEmptyBlock(pos.below())) {
                if (WorldInteraction.useOnBlock(client, this, pos.below(), Direction.UP,
                        stack -> stack.is(Items.GRASS_BLOCK), false) == WorldInteraction.ActionResult.DONE) {
                    iterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

    static BlockPos findReplaceable(final Minecraft client, final int range) {
        return BlockTargetScanner.scan(client, range, 3, BlockTargetScanner.SortMode.CLOSEST, 1,
                        target -> isReplaceableBlock(target.state().getBlock()))
                .stream()
                .map(BlockTargetScanner.BlockTarget::pos)
                .findFirst()
                .orElse(null);
    }

    private static boolean isReplaceable(final Minecraft client, final BlockPos pos) {
        return client.level != null && isReplaceableBlock(client.level.getBlockState(pos).getBlock());
    }

    static boolean isReplaceableBlock(final Block block) {
        return block == Blocks.MYCELIUM
                || block == Blocks.PODZOL
                || block == Blocks.DIRT_PATH
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT;
    }
}
