package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;
import java.util.Set;

public final class AutoDropModule extends Module {

    private final StringSetting items = this.setting(StringSetting.from(StringSetting.builder()
            .id("items")
            .name("Items")
            .defaultValue("dirt,cobblestone")
            .build()));
    private final BooleanSetting includeHotbar = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("include_hotbar")
            .name("Hotbar")
            .defaultValue(false)
            .build()));
    private final BooleanSetting fullStack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("full_stack")
            .name("Stack")
            .defaultValue(true)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(10.0)
            .min(1.0)
            .max(60.0)
            .step(1.0)
            .build()));
    private String lastItems = "";
    private Set<Item> parsedItems = Set.of();

    public AutoDropModule() {
        super("auto_drop", "Auto Drop", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null) {
            return;
        }
        if (!this.lastItems.equals(this.items.value())) {
            this.parsedItems = ItemScan.parseItems(this.items.value());
            this.lastItems = this.items.value();
        }
        if (this.parsedItems.isEmpty()) {
            return;
        }
        OptionalInt slot = this.findDropSlot(player.getInventory());
        if (slot.isEmpty()) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_NORMAL,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryActions.dropSlot(slot.orElseThrow(), this.fullStack.value())
        ));
    }

    private OptionalInt findDropSlot(final Inventory inventory) {
        int startSlot = this.includeHotbar.value() ? 0 : Inventory.getSelectionSize();
        for (int slot = startSlot; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && this.parsedItems.contains(stack.getItem())) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }
}
