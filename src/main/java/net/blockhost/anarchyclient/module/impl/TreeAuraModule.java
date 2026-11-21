package net.blockhost.anarchyclient.module.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.i18n.Translations;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;

public final class TreeAuraModule extends Module {

    private final NumberSetting radius = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("radius")
            .name("Radius")
            .defaultValue(4.0)
            .min(1.0)
            .max(6.0)
            .step(1.0)
            .build()));
    private final NumberSetting yRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("y_range")
            .name("Y Range")
            .defaultValue(3.0)
            .min(1.0)
            .max(5.0)
            .step(1.0)
            .build()));
    private final NumberSetting plantDelay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("plant_delay")
            .name("Plant Delay")
            .defaultValue(6.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final NumberSetting bonemealDelay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("bonemeal_delay")
            .name("Meal Delay")
            .defaultValue(3.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(false)
            .build()));
    private final SelectSetting sort = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("sort")
            .name("Sort")
            .defaultValue("Farthest")
            .addAllOptions(List.of("Closest", "Farthest", "Random"))
            .build()));
    /** Bone meal uses after which a sapling counts as blocked: a tree that has room grows well before this. */
    static final int MAX_BONE_MEAL_USES = 16;

    private final Map<BlockPos, Integer> boneMealUses = new HashMap<>();
    private final Set<BlockPos> skipped = new HashSet<>();
    private Level level;
    private int plantTimer;
    private int bonemealTimer;

    public TreeAuraModule() {
        super(
                "tree_aura",
                "Tree Aura",
                ModuleCategory.WORLD,
                "Plants saplings on open dirt around you and grows them with bone meal from your hotbar.");
    }

    @Override
    protected void onEnable() {
        this.plantTimer = 0;
        this.bonemealTimer = 0;
        this.forgetTargets();
    }

    @Override
    protected void onDisable() {
        this.forgetTargets();
    }

    private void forgetTargets() {
        this.boneMealUses.clear();
        this.skipped.clear();
        this.level = null;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (client.level != this.level) {
            this.forgetTargets();
            this.level = client.level;
        }
        if (this.plantTimer > 0) {
            this.plantTimer--;
        }
        if (this.bonemealTimer > 0) {
            this.bonemealTimer--;
        }
        BlockTargetScanner.SortMode sortMode = BlockTargetScanner.SortMode.fromSetting(this.sort.value());
        if (this.plantTimer <= 0) {
            BlockPos plantPos = findPlantLocation(
                    client,
                    this.radius.value().intValue(),
                    this.yRange.value().intValue(),
                    sortMode,
                    pos -> CombatTargets.canReachBlock(player, pos) && !this.skipped.contains(pos));
            if (plantPos != null) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                        client,
                        this,
                        plantPos,
                        Direction.UP,
                        stack -> Block.byItem(stack.getItem()) instanceof SaplingBlock,
                        this.rotate.value());
                if (result == WorldInteraction.ActionResult.MISSING_ITEM) {
                    player.sendSystemMessage(Translations.component(
                            "anarchyclient.message.tree_aura.tree_aura_disabled_no_saplings_in",
                            "Tree Aura disabled: no saplings in hotbar."));
                    this.enabled(false);
                    return;
                }
                if (result == WorldInteraction.ActionResult.REFUSED) {
                    this.skipped.add(plantPos);
                }
                if (result == WorldInteraction.ActionResult.DONE || result == WorldInteraction.ActionResult.REFUSED) {
                    this.plantTimer = this.plantDelay.value().intValue();
                }
            }
        }
        if (this.bonemealTimer <= 0) {
            BlockPos sapling = findSapling(
                    client,
                    this.radius.value().intValue(),
                    this.yRange.value().intValue(),
                    sortMode,
                    pos -> CombatTargets.canReachBlock(player, pos) && !this.skipped.contains(pos));
            if (sapling != null) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                        client, this, sapling, Direction.UP, stack -> stack.is(Items.BONE_MEAL), this.rotate.value());
                if (result == WorldInteraction.ActionResult.MISSING_ITEM) {
                    player.sendSystemMessage(Translations.component(
                            "anarchyclient.message.tree_aura.tree_aura_disabled_no_bone_meal",
                            "Tree Aura disabled: no bone meal in hotbar."));
                    this.enabled(false);
                    return;
                }
                // A refused or endlessly accepted sapling cannot grow (no room, or a lone 2x2 sapling); move on.
                if (result == WorldInteraction.ActionResult.REFUSED
                        || result == WorldInteraction.ActionResult.DONE
                                && this.boneMealUses.merge(sapling, 1, Integer::sum) >= MAX_BONE_MEAL_USES) {
                    this.skipped.add(sapling);
                }
                if (result == WorldInteraction.ActionResult.DONE || result == WorldInteraction.ActionResult.REFUSED) {
                    this.bonemealTimer = this.bonemealDelay.value().intValue();
                }
            }
        }
    }

    static BlockPos findPlantLocation(
            final Minecraft client,
            final int radius,
            final int yRange,
            final BlockTargetScanner.SortMode sortMode,
            final Predicate<BlockPos> usable) {
        return BlockTargetScanner.scan(
                        client,
                        radius,
                        yRange,
                        sortMode,
                        1,
                        target -> usable.test(target.pos()) && canPlant(client, target.pos()))
                .stream()
                .map(BlockTargetScanner.BlockTarget::pos)
                .findFirst()
                .orElse(null);
    }

    static BlockPos findSapling(
            final Minecraft client,
            final int radius,
            final int yRange,
            final BlockTargetScanner.SortMode sortMode,
            final Predicate<BlockPos> usable) {
        return BlockTargetScanner.scan(
                        client,
                        radius,
                        yRange,
                        sortMode,
                        1,
                        target -> target.state().getBlock() instanceof SaplingBlock
                                && usable.test(target.pos())
                                && canGrow(
                                        client.level,
                                        target.pos(),
                                        target.state().getBlock()))
                .stream()
                .map(BlockTargetScanner.BlockTarget::pos)
                .findFirst()
                .orElse(null);
    }

    /** Dark oak and pale oak saplings only grow as a 2x2 group; bone meal on a lone one is refused. */
    static boolean canGrow(final BlockGetter level, final BlockPos pos, final Block sapling) {
        if (sapling != Blocks.DARK_OAK_SAPLING && sapling != Blocks.PALE_OAK_SAPLING) {
            return true;
        }
        for (int dx = -1; dx <= 0; dx++) {
            for (int dz = -1; dz <= 0; dz++) {
                BlockPos corner = pos.offset(dx, 0, dz);
                if (level.getBlockState(corner).is(sapling)
                        && level.getBlockState(corner.east()).is(sapling)
                        && level.getBlockState(corner.south()).is(sapling)
                        && level.getBlockState(corner.south().east()).is(sapling)) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean canPlant(final Minecraft client, final BlockPos ground) {
        if (client.level == null || !client.level.isEmptyBlock(ground.above())) {
            return false;
        }
        Block block = client.level.getBlockState(ground).getBlock();
        if (block != Blocks.GRASS_BLOCK
                && block != Blocks.DIRT
                && block != Blocks.COARSE_DIRT
                && block != Blocks.ROOTED_DIRT) {
            return false;
        }
        for (int height = 2; height <= 5; height++) {
            BlockPos check = ground.above(height);
            if (!client.level.isEmptyBlock(check)) {
                return false;
            }
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                if (!client.level.isEmptyBlock(check.relative(direction))) {
                    return false;
                }
            }
        }
        return true;
    }
}
