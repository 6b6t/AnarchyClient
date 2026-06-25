package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.MinecraftAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class FastPlaceModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(4.0)
            .step(1.0)
            .build()));
    private final BooleanSetting blocksOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blocks_only")
            .name("Blocks Only")
            .defaultValue(true)
            .build()));
    private final BooleanSetting xpBottles = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("xp_bottles")
            .name("XP Bottles")
            .defaultValue(false)
            .build()));
    private final BooleanSetting crystals = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("crystals")
            .name("Crystals")
            .defaultValue(false)
            .build()));

    public FastPlaceModule() {
        super("fast_place", "Fast Place", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null || !client.options.keyUse.isDown()) {
            return;
        }
        if (!shouldAccelerate(player.getMainHandItem(), this.blocksOnly.value(), this.xpBottles.value(), this.crystals.value())) {
            return;
        }
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        int delay = this.delayTicks.value().intValue();
        if (accessor.anarchyclient$rightClickDelay() > delay) {
            accessor.anarchyclient$setRightClickDelay(delay);
        }
    }

    static boolean shouldAccelerate(final ItemStack stack, final boolean blocksOnly, final boolean xpBottles,
                                    final boolean crystals) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof BlockItem) {
            return true;
        }
        return !blocksOnly
                || xpBottles && stack.is(Items.EXPERIENCE_BOTTLE)
                || crystals && stack.is(Items.END_CRYSTAL);
    }
}
