package net.blockhost.anarchyclient.module.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class AutoFarmModule extends Module {

    /** How long a block that refused the item is left alone before it is tried again. */
    private static final int REFUSED_COOLDOWN_TICKS = 40;

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
    private final BooleanSetting till = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("till").name("Till").defaultValue(false).build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    private final SingleBlockMiner miner = new SingleBlockMiner(this);
    private final Map<BlockPos, Integer> refused = new HashMap<>();

    public AutoFarmModule() {
        super(
                "auto_farm",
                "Auto Farm",
                ModuleCategory.WORLD,
                "Harvests mature crops and can replant, bone meal, and till soil around you.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            this.miner.stop(client);
            return;
        }
        this.refused.entrySet().removeIf(entry -> {
            entry.setValue(entry.getValue() - 1);
            return entry.getValue() <= 0;
        });
        if (this.miner.continueCurrent(
                client,
                pos -> this.harvest.value()
                        && isMatureCrop(client.level.getBlockState(pos))
                        && CombatTargets.canReachBlock(player, pos))) {
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
                candidate -> this.isCandidate(client, candidate))) {
            if (actions >= maxActions) {
                break;
            }
            Step step = this.handleTarget(client, target);
            if (step == Step.WAITING) {
                // A rotated action is still turning; retry it next tick instead of moving on.
                return;
            }
            if (step == Step.DONE) {
                actions++;
            } else if (step == Step.REFUSED) {
                this.refused.put(target.pos(), REFUSED_COOLDOWN_TICKS);
            }
        }
    }

    private boolean isCandidate(final Minecraft client, final BlockTargetScanner.BlockTarget target) {
        BlockState state = target.state();
        Block block = state.getBlock();
        if (client.player == null
                || this.refused.containsKey(target.pos())
                || !CombatTargets.canReachBlock(client.player, target.pos())) {
            return false;
        }
        if (this.harvest.value() && isMatureCrop(state)) {
            return true;
        }
        if (this.bonemeal.value() && canBoneMeal(client, target.pos(), state)) {
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

    private Step handleTarget(final Minecraft client, final BlockTargetScanner.BlockTarget target) {
        if (this.harvest.value() && isMatureCrop(target.state())) {
            // Most crops break instantly; cocoa takes a few ticks and must be finished first.
            return switch (this.miner.start(client, target.pos(), Direction.UP)) {
                case BROKEN -> Step.DONE;
                case MINING -> Step.WAITING;
                case FAILED -> Step.SKIPPED;
            };
        }
        if (this.bonemeal.value() && canBoneMeal(client, target.pos(), target.state())) {
            Step step = this.use(client, target.pos(), stack -> stack.is(Items.BONE_MEAL));
            if (step != Step.SKIPPED) {
                return step;
            }
        }
        if (this.plant.value()) {
            for (Item item : plantItems(target.state().getBlock())) {
                Step step = this.use(client, target.pos(), stack -> stack.is(item));
                if (step != Step.SKIPPED) {
                    return step;
                }
            }
        }
        if (this.till.value() && isTillableBlock(target.state().getBlock())) {
            return this.use(client, target.pos(), stack -> stack.is(ItemTags.HOES));
        }
        return Step.SKIPPED;
    }

    private Step use(final Minecraft client, final BlockPos pos, final Predicate<ItemStack> item) {
        return switch (WorldInteraction.useOnBlock(client, this, pos, Direction.UP, item, this.rotate.value())) {
            case DONE -> Step.DONE;
            case WAITING -> Step.WAITING;
            case REFUSED -> Step.REFUSED;
            case MISSING_ITEM -> Step.SKIPPED;
        };
    }

    @Override
    protected void onDisable() {
        this.miner.stop(Minecraft.getInstance());
        this.refused.clear();
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
        return block instanceof SweetBerryBushBlock
                && state.getValue(SweetBerryBushBlock.AGE) >= SweetBerryBushBlock.MAX_AGE;
    }

    /** Bone meal only goes on growing crops, not on grass, moss, or other spreading blocks. */
    private static boolean canBoneMeal(final Minecraft client, final BlockPos pos, final BlockState state) {
        return client.level != null
                && isBoneMealCrop(state)
                && ((BonemealableBlock) state.getBlock())
                        .isValidBonemealTarget(client.level, pos, state, BonemealSource.INTERACTION);
    }

    static boolean isBoneMealCrop(final BlockState state) {
        Block block = state.getBlock();
        return (block instanceof CropBlock
                        || block instanceof StemBlock
                        || block instanceof CocoaBlock
                        || block instanceof SweetBerryBushBlock)
                && !isMatureCrop(state);
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

    private enum Step {
        DONE,
        WAITING,
        /** The block refused the item; it is skipped for a while. */
        REFUSED,
        SKIPPED
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
