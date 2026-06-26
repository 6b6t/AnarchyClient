package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.mixin.MinecraftAccessor;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.ItemListSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class FastUseModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("All")
            .addAllOptions(List.of("All", "Some"))
            .build()));
    private final ItemListSetting items = this.setting(ItemListSetting.from(ItemListSetting.builder()
            .id("items")
            .name("Items")
            .addAllDefaultValue(List.of(Items.EXPERIENCE_BOTTLE, Items.ENDER_PEARL, Items.FIREWORK_ROCKET))
            .build()));
    private final BooleanSetting blocks = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("blocks")
            .name("Blocks")
            .defaultValue(false)
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(0.0)
            .min(0.0)
            .max(4.0)
            .step(1.0)
            .build()));

    public FastUseModule() {
        super("fast_use", "Fast Use", ModuleCategory.PLAYER);
        this.items.visibleWhen(() -> "Some".equals(this.mode.value()));
        this.blocks.visibleWhen(() -> "Some".equals(this.mode.value()));
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.player == null || client.gui.screen() != null) {
            return;
        }
        if (!this.shouldApply(client.player.getMainHandItem()) && !this.shouldApply(client.player.getOffhandItem())) {
            return;
        }
        MinecraftAccessor accessor = (MinecraftAccessor) client;
        int delay = this.delayTicks.value().intValue();
        if (accessor.anarchyclient$rightClickDelay() > delay) {
            accessor.anarchyclient$setRightClickDelay(delay);
        }
    }

    private boolean shouldApply(final ItemStack stack) {
        return shouldApply(stack, this.mode.value(), this.items.value(), this.blocks.value());
    }

    static boolean shouldApply(final ItemStack stack, final String mode,
                               final List<net.minecraft.world.item.Item> items, final boolean blocks) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return shouldApplyItem(stack.getItem(), mode, items, blocks);
    }

    static boolean shouldApplyItem(final Item item, final String mode, final List<Item> items, final boolean blocks) {
        return item != null
                && (!"Some".equals(mode)
                || items.contains(item)
                || blocks && item instanceof BlockItem);
    }
}
