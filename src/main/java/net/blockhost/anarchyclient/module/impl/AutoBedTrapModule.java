package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AutoBedTrapModule extends Module {

    private final StringSetting blocks = this.setting(StringSetting.from(StringSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue("obsidian,crying_obsidian,ender_chest")
            .build()));
    private final NumberSetting range = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("range")
            .name("Range")
            .defaultValue(5.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final NumberSetting verticalRange = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("vertical_range")
            .name("Vertical")
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
    private String lastBlocks = "";
    private Set<Block> parsedBlocks = Set.of();

    public AutoBedTrapModule() {
        super("auto_bed_trap", "Auto Bed Trap", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (!this.lastBlocks.equals(this.blocks.value())) {
            this.parsedBlocks = BlockScan.parseBlocks(this.blocks.value());
            this.lastBlocks = this.blocks.value();
        }
        if (this.parsedBlocks.isEmpty()) {
            return;
        }

        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        for (BlockTargetScanner.BlockTarget target : BlockTargetScanner.scan(
                client,
                this.range.value().intValue(),
                this.verticalRange.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                64,
                candidate -> candidate.state().getBlock() instanceof BedBlock
        )) {
            Direction connected = BedBlock.getConnectedDirection(target.state());
            positions.addAll(trapPositions(target.pos(), target.pos().relative(connected)));
        }
        if (positions.isEmpty()) {
            return;
        }

        List<BlockPlacer.PlacementRequest> requests = new ArrayList<>();
        for (BlockPos pos : positions) {
            requests.add(new BlockPlacer.PlacementRequest(pos, stack -> matchesAny(stack, this.parsedBlocks)));
        }
        BlockPlacer.placeBatch(client, this.id(), requests, this.blocksPerTick.value().intValue(),
                this.rotate.value(), 70.0F);
    }

    static List<BlockPos> trapPositions(final BlockPos firstHalf, final BlockPos secondHalf) {
        LinkedHashSet<BlockPos> positions = new LinkedHashSet<>();
        addShell(firstHalf, positions);
        addShell(secondHalf, positions);
        positions.remove(firstHalf);
        positions.remove(secondHalf);
        return List.copyOf(positions);
    }

    private static void addShell(final BlockPos bedHalf, final Set<BlockPos> positions) {
        positions.add(bedHalf.above());
        positions.add(bedHalf.below());
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            positions.add(bedHalf.relative(direction));
        }
    }

    private static boolean matchesAny(final ItemStack stack, final Set<Block> blocks) {
        return stack.getItem() instanceof BlockItem blockItem && blocks.contains(blockItem.getBlock());
    }
}
