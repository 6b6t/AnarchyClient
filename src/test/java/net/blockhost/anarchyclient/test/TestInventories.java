package net.blockhost.anarchyclient.test;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Builds player inventories for tests without a player entity.
 */
public final class TestInventories {

    private TestInventories() {
    }

    /**
     * Returns an inventory whose slots start at index 0 with the given stacks.
     */
    public static Inventory withSlots(final ItemStack... stacks) {
        Inventory inventory = new Inventory(null, new EntityEquipment());
        for (int slot = 0; slot < stacks.length; slot++) {
            inventory.setItem(slot, stacks[slot]);
        }
        return inventory;
    }
}
