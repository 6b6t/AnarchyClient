package net.blockhost.anarchyclient.module.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

public final class LiquidFillerModule extends Module {

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
            .defaultValue(2.0)
            .min(1.0)
            .max(8.0)
            .step(1.0)
            .build()));
    private final BooleanSetting water = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("water")
            .name("Water")
            .defaultValue(true)
            .build()));
    private final BooleanSetting lava = this.setting(BooleanSetting.from(
            BooleanSetting.builder().id("lava").name("Lava").defaultValue(true).build()));
    private final BooleanSetting preferSponge = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("sponge")
            .name("Sponge")
            .defaultValue(true)
            .build()));
    private final BooleanSetting rotate = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("rotate")
            .name("Rotate")
            .defaultValue(true)
            .build()));

    public LiquidFillerModule() {
        super(
                "liquid_filler",
                "Liquid Filler",
                ModuleCategory.WORLD,
                "Places blocks into nearby water and lava source blocks, preferring dry sponges when enabled and falling back to other blocks.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        int radius = this.range.value().intValue();
        BlockPos center = player.blockPosition();
        Predicate<ItemStack> items = this.fillItems(player);
        List<BlockPlacer.PlacementRequest> requests = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
            if (this.shouldFill(client.level.getFluidState(pos)) && CombatTargets.canReachBlock(player, pos)) {
                requests.add(new BlockPlacer.PlacementRequest(pos.immutable(), items));
            }
        }
        // The batch stops at a spot that is still rotating, so it is retried next tick.
        BlockPlacer.placeBatch(
                client, this.id(), requests, this.blocksPerTick.value().intValue(), this.rotate.value(), 70.0F);
    }

    /** Uses dry sponges when preferred and available, otherwise any full block. */
    private Predicate<ItemStack> fillItems(final LocalPlayer player) {
        Predicate<ItemStack> sponge = stack -> stack.is(Items.SPONGE);
        if (this.preferSponge.value() && WorldInteraction.hasHotbarItem(player.getInventory(), sponge)) {
            return sponge;
        }
        return stack -> true;
    }

    private boolean shouldFill(final FluidState fluid) {
        return fluid.isSource()
                && (this.water.value() && fluid.getType() == Fluids.WATER
                        || this.lava.value() && fluid.getType() == Fluids.LAVA);
    }
}
