package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoTotemModule extends Module {

    private final NumberSetting healthThreshold = this.setting(new NumberSetting("health_threshold", "Health", 12.0, 1.0, 36.0, 0.5));
    private final BooleanSetting includeAbsorption = this.setting(new BooleanSetting("include_absorption", "Absorption", true));
    private int cooldownTicks;

    public AutoTotemModule() {
        super("auto_totem", "Auto Totem", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || player.containerMenu == null || player.inventoryMenu == null) {
            return;
        }
        if (client.screen != null || player.containerMenu.containerId != InventoryMenu.CONTAINER_ID) {
            return;
        }

        float health = player.getHealth();
        if (this.includeAbsorption.value()) {
            health += player.getAbsorptionAmount();
        }
        if (health > this.healthThreshold.value()) {
            return;
        }
        if (player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            return;
        }

        int inventorySlot = this.findTotemSlot(player.getInventory());
        if (inventorySlot < 0) {
            return;
        }

        int menuSlot = this.toInventoryMenuSlot(inventorySlot);
        if (menuSlot < 0) {
            return;
        }

        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, menuSlot, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, InventoryMenu.SHIELD_SLOT, 0, ContainerInput.PICKUP, player);
        client.gameMode.handleContainerInput(InventoryMenu.CONTAINER_ID, menuSlot, 0, ContainerInput.PICKUP, player);
        this.cooldownTicks = 5;
    }

    private int findTotemSlot(final Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.TOTEM_OF_UNDYING)) {
                return slot;
            }
        }
        return -1;
    }

    private int toInventoryMenuSlot(final int inventorySlot) {
        if (Inventory.isHotbarSlot(inventorySlot)) {
            return InventoryMenu.USE_ROW_SLOT_START + inventorySlot;
        }
        if (inventorySlot >= Inventory.getSelectionSize() && inventorySlot < Inventory.INVENTORY_SIZE) {
            return InventoryMenu.INV_SLOT_START + inventorySlot - Inventory.getSelectionSize();
        }
        return -1;
    }
}
