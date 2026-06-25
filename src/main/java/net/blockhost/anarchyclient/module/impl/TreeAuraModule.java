package net.blockhost.anarchyclient.module.impl;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SaplingBlock;

import java.util.List;

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
    private int plantTimer;
    private int bonemealTimer;

    public TreeAuraModule() {
        super("tree_aura", "Tree Aura", ModuleCategory.WORLD);
    }

    @Override
    protected void onEnable() {
        this.plantTimer = 0;
        this.bonemealTimer = 0;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.plantTimer > 0) {
            this.plantTimer--;
        }
        if (this.bonemealTimer > 0) {
            this.bonemealTimer--;
        }
        BlockTargetScanner.SortMode sortMode = BlockTargetScanner.SortMode.fromSetting(this.sort.value());
        if (this.plantTimer <= 0) {
            BlockPos plantPos = findPlantLocation(client, this.radius.value().intValue(), this.yRange.value().intValue(), sortMode);
            if (plantPos != null) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                        client,
                        this,
                        plantPos,
                        Direction.UP,
                        stack -> Block.byItem(stack.getItem()) instanceof SaplingBlock,
                        this.rotate.value()
                );
                if (result == WorldInteraction.ActionResult.MISSING_ITEM) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Tree Aura disabled: no saplings in hotbar."));
                    this.enabled(false);
                    return;
                }
                if (result == WorldInteraction.ActionResult.DONE) {
                    this.plantTimer = this.plantDelay.value().intValue();
                }
            }
        }
        if (this.bonemealTimer <= 0) {
            BlockPos sapling = findSapling(client, this.radius.value().intValue(), this.yRange.value().intValue(), sortMode);
            if (sapling != null) {
                WorldInteraction.ActionResult result = WorldInteraction.useOnBlock(
                        client,
                        this,
                        sapling,
                        Direction.UP,
                        stack -> stack.is(Items.BONE_MEAL),
                        this.rotate.value()
                );
                if (result == WorldInteraction.ActionResult.MISSING_ITEM) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Tree Aura disabled: no bone meal in hotbar."));
                    this.enabled(false);
                    return;
                }
                if (result == WorldInteraction.ActionResult.DONE) {
                    this.bonemealTimer = this.bonemealDelay.value().intValue();
                }
            }
        }
    }

    static BlockPos findPlantLocation(final Minecraft client, final int radius, final int yRange,
                                      final BlockTargetScanner.SortMode sortMode) {
        return BlockTargetScanner.scan(client, radius, yRange, sortMode, 1,
                        target -> canPlant(client, target.pos()))
                .stream()
                .map(BlockTargetScanner.BlockTarget::pos)
                .findFirst()
                .orElse(null);
    }

    static BlockPos findSapling(final Minecraft client, final int radius, final int yRange,
                                final BlockTargetScanner.SortMode sortMode) {
        return BlockTargetScanner.scan(client, radius, yRange, sortMode, 1,
                        target -> target.state().getBlock() instanceof SaplingBlock)
                .stream()
                .map(BlockTargetScanner.BlockTarget::pos)
                .findFirst()
                .orElse(null);
    }

    static boolean canPlant(final Minecraft client, final BlockPos ground) {
        if (client.level == null || !client.level.isEmptyBlock(ground.above())) {
            return false;
        }
        Block block = client.level.getBlockState(ground).getBlock();
        if (block != Blocks.GRASS_BLOCK && block != Blocks.DIRT && block != Blocks.COARSE_DIRT && block != Blocks.ROOTED_DIRT) {
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
