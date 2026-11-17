package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ElytraSwapModule extends Module {

    static final double MIN_FALL_DISTANCE = 2.0;

    public ElytraSwapModule() {
        super("elytra_swap", "Elytra Swap", ModuleCategory.PLAYER,
                "Equips an elytra from the inventory when you fall more than two blocks while wearing something else in the chest slot.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                || !isFalling(player.onGround(), player.fallDistance, player.getAbilities().flying,
                player.isInLiquid() || player.isPassenger())) {
            return;
        }
        int slot = findElytra(player.getInventory());
        InventorySlotRef source = slot < 0 ? null : InventorySlots.storageSlot(slot).orElse(null);
        if (source == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_EQUIPMENT,
                6,
                InventoryAction.pickupSwap(source, InventorySlots.armorSlot(EquipmentSlot.CHEST))
        ));
    }

    static boolean isFalling(final boolean onGround, final double fallDistance, final boolean flying,
                             final boolean inLiquidOrRiding) {
        return !onGround && !flying && !inLiquidOrRiding && fallDistance > MIN_FALL_DISTANCE;
    }

    static int findElytra(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(Items.ELYTRA) && ExtraElytraModule.canGlide(stack)) {
                return slot;
            }
        }
        return -1;
    }
}
