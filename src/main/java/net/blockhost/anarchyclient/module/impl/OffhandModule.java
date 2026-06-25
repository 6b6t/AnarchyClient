package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class OffhandModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Totem")
            .addAllOptions(List.of("Totem", "Crystal", "Golden Apple", "Shield", "Wind Charge"))
            .build()));
    private final NumberSetting emergencyHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("emergency_health")
            .name("Emergency HP")
            .defaultValue(12.0)
            .min(1.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final BooleanSetting includeAbsorption = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("include_absorption")
            .name("Absorption")
            .defaultValue(true)
            .build()));
    private final BooleanSetting emergencyTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("emergency_totem")
            .name("Emergency")
            .defaultValue(true)
            .build()));

    public OffhandModule() {
        super("offhand", "Offhand", ModuleCategory.COMBAT, List.of("auto_offhand"));
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        Item desired = this.desiredItem(player);
        if (player.getOffhandItem().is(desired)) {
            return;
        }
        int inventorySlot = findItemSlot(player.getInventory(), desired);
        InventorySlotRef source = inventorySlot < 0 ? null : InventorySlots.storageSlot(inventorySlot).orElse(null);
        if (source == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                desired == Items.TOTEM_OF_UNDYING ? InventoryActionScheduler.PRIORITY_LIFE : InventoryActionScheduler.PRIORITY_EQUIPMENT,
                4,
                InventoryAction.pickupSwap(source, InventorySlots.offhandSlot())
        ));
    }

    private Item desiredItem(final LocalPlayer player) {
        if (this.emergencyTotem.value() && needsEmergencyTotem(player, this.emergencyHealth.value(), this.includeAbsorption.value())) {
            return Items.TOTEM_OF_UNDYING;
        }
        return switch (this.mode.value()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            case "Wind Charge" -> Items.WIND_CHARGE;
            default -> Items.TOTEM_OF_UNDYING;
        };
    }

    static boolean needsEmergencyTotem(final LocalPlayer player, final double threshold, final boolean includeAbsorption) {
        float health = player.getHealth() + (includeAbsorption ? player.getAbsorptionAmount() : 0.0F);
        return threshold > 0.0 && health <= threshold;
    }

    static int findItemSlot(final Inventory inventory, final Item item) {
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }
}
