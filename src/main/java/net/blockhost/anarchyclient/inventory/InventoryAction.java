package net.blockhost.anarchyclient.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.Item;

public sealed interface InventoryAction permits InventoryAction.PickupSwap, InventoryAction.SelectHotbarSlot, InventoryAction.UseHotbarItem {

    boolean canExecute(Minecraft client, LocalPlayer player);

    boolean execute(Minecraft client, LocalPlayer player);

    static InventoryAction pickupSwap(final int sourceMenuSlot, final int targetMenuSlot) {
        return new PickupSwap(sourceMenuSlot, targetMenuSlot);
    }

    static InventoryAction selectHotbarSlot(final int inventorySlot) {
        return new SelectHotbarSlot(inventorySlot);
    }

    static InventoryAction useHotbarItem(final int inventorySlot, final Item item, final boolean restoreSlot) {
        return new UseHotbarItem(inventorySlot, item, restoreSlot);
    }

    record PickupSwap(int sourceMenuSlot, int targetMenuSlot) implements InventoryAction {

        @Override
        public boolean canExecute(final Minecraft client, final LocalPlayer player) {
            return client.gameMode != null && this.sourceMenuSlot >= 0 && this.targetMenuSlot >= 0;
        }

        @Override
        public boolean execute(final Minecraft client, final LocalPlayer player) {
            if (!this.canExecute(client, player)) {
                return false;
            }
            client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, this.sourceMenuSlot, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, this.targetMenuSlot, 0, ContainerInput.PICKUP, player);
            client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, this.sourceMenuSlot, 0, ContainerInput.PICKUP, player);
            return true;
        }
    }

    record SelectHotbarSlot(int inventorySlot) implements InventoryAction {

        @Override
        public boolean canExecute(final Minecraft client, final LocalPlayer player) {
            return Inventory.isHotbarSlot(this.inventorySlot);
        }

        @Override
        public boolean execute(final Minecraft client, final LocalPlayer player) {
            if (!this.canExecute(client, player)) {
                return false;
            }
            player.getInventory().setSelectedSlot(this.inventorySlot);
            return true;
        }
    }

    record UseHotbarItem(int inventorySlot, Item item, boolean restoreSlot) implements InventoryAction {

        @Override
        public boolean canExecute(final Minecraft client, final LocalPlayer player) {
            return client.gameMode != null
                    && Inventory.isHotbarSlot(this.inventorySlot)
                    && player.getInventory().getItem(this.inventorySlot).is(this.item);
        }

        @Override
        public boolean execute(final Minecraft client, final LocalPlayer player) {
            if (!this.canExecute(client, player)) {
                return false;
            }
            int previousSlot = player.getInventory().getSelectedSlot();
            player.getInventory().setSelectedSlot(this.inventorySlot);
            client.gameMode.useItem(player, InteractionHand.MAIN_HAND);
            if (this.restoreSlot) {
                player.getInventory().setSelectedSlot(previousSlot);
            }
            return true;
        }
    }
}
