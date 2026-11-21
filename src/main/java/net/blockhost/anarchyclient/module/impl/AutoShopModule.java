package net.blockhost.anarchyclient.module.impl;

import java.util.Set;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class AutoShopModule extends Module {

    private final StringSetting buyItems = this.setting(StringSetting.from(StringSetting.builder()
            .id("buy_items")
            .name("Buy")
            .defaultValue("")
            .description("Comma-separated item ids to quick-move from shop menus.")
            .build()));
    private final NumberSetting delayTicks = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(6.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private final BooleanSetting screenOnly = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("screen_only")
            .name("Screen")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;

    public AutoShopModule() {
        super(
                "auto_shop",
                "Auto Shop",
                ModuleCategory.PLAYER,
                "Quick-moves configured items out of open container menus, such as server shop GUIs, on a fixed delay.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null
                || client.gameMode == null
                || this.screenOnly.value() && !(client.gui.screen() instanceof AbstractContainerScreen<?>)) {
            this.cooldownTicks = 0;
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        AbstractContainerMenu menu = player.containerMenu;
        if (menu == player.inventoryMenu) {
            return;
        }
        Set<Item> targets = ItemScan.parseItems(this.buyItems.value());
        int slot = firstMatchingSlot(menu, targets);
        if (slot >= 0 && ContainerActions.quickMove(client, menu, slot)) {
            this.cooldownTicks = this.delayTicks.value().intValue();
        }
    }

    static int firstMatchingSlot(final AbstractContainerMenu menu, final Set<Item> targets) {
        if (menu == null || targets == null || targets.isEmpty()) {
            return -1;
        }
        for (int slot = 0; slot < menu.slots.size(); slot++) {
            Slot menuSlot = menu.slots.get(slot);
            ItemStack stack = menuSlot.getItem();
            // The player's own inventory slots are part of the menu too; only shop slots are bought from.
            if (!(menuSlot.container instanceof Inventory) && !stack.isEmpty() && targets.contains(stack.getItem())) {
                return slot;
            }
        }
        return -1;
    }
}
