package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public final class AutoFarmModule extends Module {

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
            .defaultValue(1.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final BooleanSetting harvest = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("harvest")
            .name("Harvest")
            .defaultValue(true)
            .build()));
    private final BooleanSetting plant = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("plant")
            .name("Plant")
            .defaultValue(true)
            .build()));
    private final BooleanSetting bonemeal = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("bonemeal")
            .name("Bone Meal")
            .defaultValue(false)
            .build()));
    private final BooleanSetting till = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("till")
            .name("Till")
            .defaultValue(false)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AutoFarmModule() {
        super("auto_farm", "Auto Farm", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int radius = this.range.value().intValue();
        int maxActions = clampedActions(this.blocksPerTick.value());
        int actions = 0;
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                radius,
                radius,
                BlockTargetScanner.SortMode.CLOSEST,
                scanLimit(radius),
                candidate -> this.isCandidate(client, candidate)
        )) {
            if (actions >= maxActions) {
                break;
            }
            if (this.handleTarget(client, target)) {
                actions++;
            }
        }
    }

    private boolean isCandidate(final Minecraft client, final BlockTargetScanner.BlockTarget target) {
        BlockState state = target.state();
        Block block = state.getBlock();
        if (this.harvest.value() && isMatureCrop(state)) {
            return true;
        }
        if (this.bonemeal.value() && canBoneMeal(state)) {
            return true;
        }
        if (client.level == null || !client.level.isEmptyBlock(target.pos().above())) {
            return false;
        }
        if (this.plant.value() && !plantItems(block).isEmpty()) {
            return true;
        }
        return this.till.value() && isTillableBlock(block);
    }

    private boolean handleTarget(final Minecraft client, final BlockTargetScanner.BlockTarget target) {
        if (this.harvest.value() && isMatureCrop(target.state())) {
            return WorldInteraction.breakBlock(client, target.pos(), Direction.UP, stack -> true);
        }
        if (this.bonemeal.value() && canBoneMeal(target.state())) {
            return WorldInteraction.useOnBlock(client, this, target.pos(), Direction.UP,
                    stack -> stack.is(Items.BONE_MEAL), this.rotate.value()) == WorldInteraction.ActionResult.DONE;
        }
        if (this.plant.value() && this.plant(client, target.pos(), target.state().getBlock())) {
            return true;
        }
        return this.till.value()
                && isTillableBlock(target.state().getBlock())
                && WorldInteraction.useOnBlock(client, this, target.pos(), Direction.UP,
                stack -> stack.getItem() instanceof HoeItem, this.rotate.value()) == WorldInteraction.ActionResult.DONE;
    }

    private boolean plant(final Minecraft client, final BlockPos ground, final Block baseBlock) {
        for (Item item : plantItems(baseBlock)) {
            WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(client, this, ground, Direction.UP,
                    stack -> stack.is(item), this.rotate.value());
            if (result == WorldInteraction.ActionResult.DONE) {
                return true;
            }
        }
        return false;
    }

    static boolean isMatureCrop(final BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (block instanceof NetherWartBlock) {
            return state.getValue(NetherWartBlock.AGE) >= NetherWartBlock.MAX_AGE;
        }
        if (block instanceof CocoaBlock) {
            return state.getValue(CocoaBlock.AGE) >= CocoaBlock.MAX_AGE;
        }
        return block instanceof SweetBerryBushBlock && state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
    }

    static boolean canBoneMeal(final BlockState state) {
        return state.getBlock() instanceof BonemealableBlock && !isMatureCrop(state);
    }

    static boolean isTillableBlock(final Block block) {
        return block == Blocks.GRASS_BLOCK
                || block == Blocks.DIRT
                || block == Blocks.COARSE_DIRT
                || block == Blocks.ROOTED_DIRT
                || block == Blocks.DIRT_PATH;
    }

    static int scanLimit(final int radius) {
        int side = Math.max(1, radius * 2 + 1);
        return Math.min(4096, side * side * side);
    }

    static int clampedActions(final double value) {
        return Math.max(1, Math.min(8, (int) value));
    }

    private static List<Item> plantItems(final Block baseBlock) {
        if (baseBlock == Blocks.FARMLAND) {
            return List.of(Items.WHEAT_SEEDS, Items.CARROT, Items.POTATO, Items.BEETROOT_SEEDS);
        }
        if (baseBlock == Blocks.SOUL_SAND) {
            return List.of(Items.NETHER_WART);
        }
        return List.of();
    }

    static List<String> plantItemIds(final String baseBlockId) {
        if ("farmland".equals(baseBlockId) || "minecraft:farmland".equals(baseBlockId)) {
            return List.of("wheat_seeds", "carrot", "potato", "beetroot_seeds");
        }
        if ("soul_sand".equals(baseBlockId) || "minecraft:soul_sand".equals(baseBlockId)) {
            return List.of("nether_wart");
        }
        return List.of();
    }
}
