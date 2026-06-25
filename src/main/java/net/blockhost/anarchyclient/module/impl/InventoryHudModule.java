package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class InventoryHudModule extends HudElementModule {

    public InventoryHudModule() {
        super("inventory_hud", "Inventory HUD", "Bottom Right");
    }

    @Override
    protected int color() {
        return 0xFF8EEAD5;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        Inventory inventory = client.player.getInventory();
        int occupied = occupiedSlots(inventory, 36);
        return List.of(
                "Inventory " + occupied + "/36",
                "Free " + Math.max(0, 36 - occupied),
                "Totems " + count(inventory, Items.TOTEM_OF_UNDYING),
                "Crystals " + count(inventory, Items.END_CRYSTAL),
                "Gapples " + count(inventory, Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE),
                "Obsidian " + count(inventory, Items.OBSIDIAN)
        );
    }

    static int occupiedSlots(final Inventory inventory, final int maxSlots) {
        int occupied = 0;
        for (int slot = 0; slot < Math.min(maxSlots, inventory.getContainerSize()); slot++) {
            if (!inventory.getItem(slot).isEmpty()) {
                occupied++;
            }
        }
        return occupied;
    }

    static int count(final Inventory inventory, final Item... items) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            for (Item item : items) {
                if (stack.is(item)) {
                    total += stack.getCount();
                    break;
                }
            }
        }
        return total;
    }
}
