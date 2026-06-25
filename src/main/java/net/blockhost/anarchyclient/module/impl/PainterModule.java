package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PainterModule extends Module {

    private final StringSetting block = this.setting(StringSetting.from(StringSetting.builder()
            .id("block")
            .name("Block")
            .defaultValue("stone_button")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(4.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
            .defaultValue(3.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting blocksPerTick = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blocks_per_tick")
            .name("Blocks")
            .defaultValue(2.0)
            .min(1.0)
            .max(12.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(1.0)
            .min(0.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private final BooleanSetting topSurface = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("top_surface")
            .name("Top")
            .defaultValue(true)
            .build()));
    private final BooleanSetting sides = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sides")
            .name("Sides")
            .defaultValue(true)
            .build()));
    private final BooleanSetting bottomSurface = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("bottom_surface")
            .name("Bottom")
            .defaultValue(false)
            .build()));
    private final BooleanSetting oneBlockHeight = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("one_block_height")
            .name("One High")
            .defaultValue(false)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));
    private String lastBlock = "";
    private Block selectedBlock;
    private int cooldownTicks;

    public PainterModule() {
        super("painter", "Painter", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (!this.lastBlock.equals(this.block.value())) {
            this.selectedBlock = parseBlock(this.block.value());
            this.lastBlock = this.block.value();
        }
        if (this.selectedBlock == null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        int horizontal = this.range.value().intValue();
        int vertical = this.verticalRange.value().intValue();
        List<BlockPlacer.PlacementRequest> requests = new ArrayList<>();
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                horizontal,
                vertical,
                BlockTargetScanner.SortMode.CLOSEST,
                AutoTntModule.scanLimit(horizontal, vertical),
                candidate -> shouldPaint(client.level, candidate.pos(), this.selectedBlock,
                        this.topSurface.value(), this.sides.value(), this.bottomSurface.value(), this.oneBlockHeight.value())
        )) {
            requests.add(new BlockPlacer.PlacementRequest(
                    target.pos(),
                    stack -> matchesBlock(stack, this.selectedBlock),
                    BlockPlacer.PlacementOptions.NON_FULL
            ));
        }

        int placed = BlockPlacer.placeBatch(client, this.id(), requests, this.blocksPerTick.value().intValue(),
                this.rotate.value(), 70.0F);
        if (placed > 0) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
    }

    static boolean shouldPaint(final BlockGetter level, final BlockPos target, final Block selectedBlock,
                               final boolean topSurface, final boolean sides, final boolean bottomSurface,
                               final boolean oneBlockHeight) {
        boolean targetReplaceable = level.getBlockState(target).canBeReplaced();
        boolean aboveReplaceable = level.getBlockState(target.above()).canBeReplaced();
        boolean hasTopSurface = paintableSurface(level, target.below(), selectedBlock);
        boolean hasBottomSurface = paintableSurface(level, target.above(), selectedBlock);
        boolean hasSideSurface = false;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (paintableSurface(level, target.relative(direction), selectedBlock)) {
                hasSideSurface = true;
                break;
            }
        }
        return shouldPaintSurface(targetReplaceable, aboveReplaceable, topSurface, sides, bottomSurface,
                hasTopSurface, hasSideSurface, hasBottomSurface, oneBlockHeight);
    }

    static boolean shouldPaintSurface(final boolean targetReplaceable, final boolean aboveReplaceable,
                                      final boolean topSurface, final boolean sides, final boolean bottomSurface,
                                      final boolean hasTopSurface, final boolean hasSideSurface,
                                      final boolean hasBottomSurface, final boolean oneBlockHeight) {
        if (!targetReplaceable || oneBlockHeight && aboveReplaceable) {
            return false;
        }
        return topSurface && hasTopSurface || sides && hasSideSurface || bottomSurface && hasBottomSurface;
    }

    private static boolean paintableSurface(final BlockGetter level, final BlockPos pos, final Block selectedBlock) {
        BlockState state = level.getBlockState(pos);
        return !state.canBeReplaced()
                && state.getBlock() != selectedBlock
                && !state.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean matchesBlock(final ItemStack stack, final Block block) {
        return stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == block;
    }

    static Block parseBlock(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String id = value.trim().toLowerCase(Locale.ROOT);
        Identifier identifier = id.contains(":") ? Identifier.tryParse(id) : Identifier.withDefaultNamespace(id);
        if (identifier == null) {
            return null;
        }
        return BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
    }
}
