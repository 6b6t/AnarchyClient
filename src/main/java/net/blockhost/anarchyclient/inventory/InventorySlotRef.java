package net.blockhost.anarchyclient.inventory;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;

public sealed interface InventorySlotRef permits ArmorInventorySlot, HotbarInventorySlot, MainInventorySlot, OffhandInventorySlot {

    int menuSlot();

    OptionalInt inventorySlot();

    default Optional<ItemStack> stack(final Inventory inventory) {
        OptionalInt slot = this.inventorySlot();
        if (slot.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(inventory.getItem(slot.orElseThrow()));
    }
}
