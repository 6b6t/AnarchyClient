package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    protected void renderHudElement(final Minecraft client, final GuiGraphicsExtractor graphics) {
        Inventory inventory = client.player.getInventory();
        int columns = 9;
        int rows = 4;
        int cell = 18;
        int width = columns * cell + 8;
        int height = rows * cell + 8;
        HudPosition position = this.position(graphics, width, height);
        int x = position.x();
        int y = position.y();
        graphics.fill(x, y, x + width, y + height, 0xAA101418);
        graphics.outline(x, y, width, height, 0x55FFFFFF);
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slot = row == rows - 1 ? column : 9 + row * columns + column;
                ItemStack stack = inventory.getItem(slot);
                int slotX = x + 4 + column * cell;
                int slotY = y + 4 + row * cell;
                graphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x6621262D);
                if (!stack.isEmpty()) {
                    graphics.item(stack, slotX, slotY);
                    graphics.itemDecorations(client.font, stack, slotX, slotY);
                }
            }
        }
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
