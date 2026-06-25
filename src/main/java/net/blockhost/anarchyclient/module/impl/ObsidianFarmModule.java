package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ObsidianFarmModule extends Module {

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
    private final BooleanSetting protectEating = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("protect_eating")
            .name("Protect Eating")
            .defaultValue(true)
            .build()));
    private boolean lockedUntilNether;

    public ObsidianFarmModule() {
        super("obsidian_farm", "Obsidian Farm", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (client.level.dimension() == Level.NETHER) {
            this.lockedUntilNether = false;
            return;
        }
        if (this.lockedUntilNether || this.protectEating.value() && isConsuming(player.getUseItem(), player.isUsingItem())) {
            return;
        }
        if (!WorldInteraction.hasHotbarItem(player.getInventory(), ObsidianFarmModule::isPickaxe)) {
            return;
        }

        BlockPos feet = player.blockPosition();
        BlockPos underFeet = feet.below();
        BlockPos target = null;
        BlockPos fallback = null;
        for (BlockTargetScanner.BlockTarget candidate : BlockTargetScanner.scan(
                client,
                this.range.value().intValue(),
                this.verticalRange.value().intValue(),
                BlockTargetScanner.SortMode.CLOSEST,
                AutoTntModule.scanLimit(this.range.value().intValue(), this.verticalRange.value().intValue()),
                block -> block.state().is(Blocks.OBSIDIAN)
        )) {
            if (candidate.pos().equals(underFeet)) {
                fallback = candidate.pos();
                continue;
            }
            target = candidate.pos();
            break;
        }
        if (target == null) {
            target = fallback;
        }
        if (target == null) {
            return;
        }
        if (WorldInteraction.breakBlock(client, target, Direction.UP, ObsidianFarmModule::isPickaxe)
                && target.equals(underFeet)) {
            this.lockedUntilNether = true;
        }
    }

    @Override
    protected void onDisable() {
        this.lockedUntilNether = false;
    }

    static boolean isConsuming(final ItemStack useStack, final boolean usingItem) {
        return usingItem && !useStack.isEmpty() && useStack.has(DataComponents.CONSUMABLE);
    }

    static boolean isPickaxe(final ItemStack stack) {
        return stack.is(Items.DIAMOND_PICKAXE) || stack.is(Items.NETHERITE_PICKAXE);
    }

    static List<BlockPos> candidatePositions(final BlockPos center, final int horizontalRadius, final int verticalRadius) {
        List<BlockPos> positions = new ArrayList<>();
        int horizontal = Math.max(0, horizontalRadius);
        int vertical = Math.max(0, verticalRadius);
        for (int y = center.getY() - vertical; y <= center.getY() + vertical; y++) {
            for (int x = center.getX() - horizontal; x <= center.getX() + horizontal; x++) {
                for (int z = center.getZ() - horizontal; z <= center.getZ() + horizontal; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
        positions.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return List.copyOf(positions);
    }
}
