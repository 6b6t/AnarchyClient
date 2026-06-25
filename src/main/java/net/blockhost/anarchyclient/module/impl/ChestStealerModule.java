package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public final class ChestStealerModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(4.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting filterJunk = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("filter_junk")
            .name("Filter Junk")
            .defaultValue(false)
            .build()));
    private final BooleanSetting closeWhenEmpty = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("close_when_empty")
            .name("Auto Close")
            .defaultValue(true)
            .build()));
    private final StringSetting ignoredItems = this.setting(StringSetting.from(StringSetting.builder()
            .id("ignored_items")
            .name("Ignored")
            .defaultValue("")
            .build()));
    private int cooldownTicks;

    public ChestStealerModule() {
        super("chest_stealer", "Chest Stealer", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null
                || client.gameMode == null
                || !(player.containerMenu instanceof ChestMenu menu)
                || !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            this.cooldownTicks = 0;
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }

        int containerSlots = menu.getRowCount() * 9;
        Set<Item> ignored = ItemScan.parseItems(this.ignoredItems.value());
        int slot = firstStealableSlot(menu.getItems(), containerSlots, this.filterJunk.value(), ignored);
        if (slot >= 0) {
            client.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.QUICK_MOVE, player);
            this.cooldownTicks = this.delayTicks.value().intValue();
        } else if (this.closeWhenEmpty.value()) {
            player.closeContainer();
        }
    }

    static int firstStealableSlot(final List<ItemStack> stacks, final int containerSlots, final boolean filterJunk,
                                  final Set<Item> ignoredItems) {
        int limit = Math.min(Math.max(0, containerSlots), stacks.size());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = stacks.get(slot);
            if (shouldSteal(stack, filterJunk, ignoredItems)) {
                return slot;
            }
        }
        return -1;
    }

    static boolean shouldSteal(final ItemStack stack, final boolean filterJunk, final Set<Item> ignoredItems) {
        return !stack.isEmpty()
                && !ignoredItems.contains(stack.getItem())
                && (!filterJunk || !InventoryCleanerModule.isConfiguredJunk(stack, InventoryCleanerModule.defaultJunkItems()));
    }
}
