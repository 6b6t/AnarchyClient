package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public final class AutoArmorModule extends Module {

    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final NumberSetting delayTicksSetting = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay_ticks")
            .name("Delay")
            .defaultValue(5.0)
            .min(1.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting keepElytra = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("keep_elytra")
            .name("Keep Elytra")
            .defaultValue(true)
            .build()));

    public AutoArmorModule() {
        super("auto_armor", "Auto Armor", ModuleCategory.COMBAT);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (slot == EquipmentSlot.CHEST && this.keepElytra.value() && player.getItemBySlot(slot).has(net.minecraft.core.component.DataComponents.GLIDER)) {
                continue;
            }
            int inventorySlot = bestArmorSlot(player.getInventory(), slot, player.getItemBySlot(slot));
            if (inventorySlot >= 0) {
                InventoryAction action = InventoryAction.pickupSwap(
                        InventorySlots.toInventoryMenuSlot(inventorySlot),
                        InventorySlots.armorMenuSlot(slot)
                );
                InventoryActionScheduler.schedule(InventoryActionChain.single(
                        this.id(),
                        InventoryActionScheduler.PRIORITY_EQUIPMENT,
                        this.delayTicksSetting.value().intValue(),
                        action
                ));
                return;
            }
        }
    }

    static int bestArmorSlot(final Inventory inventory, final EquipmentSlot slot, final ItemStack current) {
        double currentScore = EquipmentScorer.armorScore(current, slot);
        int bestSlot = -1;
        double bestScore = currentScore;
        for (int inventorySlot = 0; inventorySlot < inventory.getContainerSize(); inventorySlot++) {
            ItemStack stack = inventory.getItem(inventorySlot);
            double score = EquipmentScorer.armorScore(stack, slot);
            if (score > bestScore + 0.1) {
                bestScore = score;
                bestSlot = inventorySlot;
            }
        }
        return bestSlot;
    }
}
