package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AnvilBlock;

import java.util.List;

public final class AntiAnvilModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Both")
            .addAllOptions(List.of("Break", "Place", "Both"))
            .build()));
    private final NumberSetting scanHeight = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("scan_height")
            .name("Height")
            .defaultValue(6.0)
            .min(2.0)
            .max(12.0)
            .step(1.0)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public AntiAnvilModule() {
        super("anti_anvil", "Anti Anvil", ModuleCategory.COMBAT,
                "Breaks anvils above your head or places obsidian under them before they land.");
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }
        for (int y = 1; y <= this.scanHeight.value().intValue(); y++) {
            BlockPos pos = client.player.blockPosition().above(y);
            if (client.level.getBlockState(pos).getBlock() instanceof AnvilBlock) {
                if (this.shouldPlace() && y > 1 && this.canPlaceBlocker(client, pos.below())) {
                    // Placing (or waiting for the rotation) takes this tick; the anvil is handled next tick.
                    if (BlockPlacement.place(client, this, pos.below(), this.rotate.value(), 60.0F,
                            stack -> stack.is(Items.OBSIDIAN)) != BlockPlacement.PlacementResult.FILLED) {
                        WorldInteraction.stopBreaking(client, this);
                        return;
                    }
                }
                if (this.shouldBreak()) {
                    WorldInteraction.breakBlock(client, this, pos, Direction.DOWN);
                } else {
                    WorldInteraction.stopBreaking(client, this);
                }
                return;
            }
        }
        WorldInteraction.stopBreaking(client, this);
    }

    @Override
    protected void onDisable() {
        WorldInteraction.stopBreaking(Minecraft.getInstance(), this);
    }

    private boolean canPlaceBlocker(final Minecraft client, final BlockPos blocker) {
        return client.player != null && client.level != null
                && client.level.getBlockState(blocker).canBeReplaced()
                && WorldInteraction.hasHotbarItem(client.player.getInventory(), stack -> stack.is(Items.OBSIDIAN))
                && BlockPlacer.findPlacementTarget(client.level, blocker).isPresent();
    }

    private boolean shouldBreak() {
        return "Break".equals(this.mode.value()) || "Both".equals(this.mode.value());
    }

    private boolean shouldPlace() {
        return "Place".equals(this.mode.value()) || "Both".equals(this.mode.value());
    }
}
