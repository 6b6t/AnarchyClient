package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

public final class ChestCleanerModule extends Module {

    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(4.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final StringSetting junkItems = this.setting(StringSetting.from(StringSetting.builder()
            .id("junk_items")
            .name("Junk")
            .defaultValue("rotten_flesh, poisonous_potato, spider_eye")
            .build()));
    private int cooldownTicks;

    public ChestCleanerModule() {
        super("chest_cleaner", "Chest Cleaner", ModuleCategory.PLAYER);
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
        int slot = firstJunkSlot(menu.getItems(), containerSlots, ItemScan.parseItems(this.junkItems.value()));
        if (slot >= 0 && ContainerActions.throwSlot(client, menu, slot)) {
            this.cooldownTicks = this.delayTicks.value().intValue();
        }
    }

    static int firstJunkSlot(final List<ItemStack> stacks, final int containerSlots, final Set<Item> junkItems) {
        int limit = Math.min(Math.max(0, containerSlots), stacks.size());
        for (int slot = 0; slot < limit; slot++) {
            ItemStack stack = stacks.get(slot);
            if (InventoryCleanerModule.isConfiguredJunk(stack, junkItems)) {
                return slot;
            }
        }
        return -1;
    }
}
