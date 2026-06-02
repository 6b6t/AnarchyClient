package net.blockhost.anarchyclient.module.impl;

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

public final class AutoTotemModule extends Module {

    private final SelectSetting mode = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("mode")
            .name("Mode")
            .defaultValue("Totem")
            .addAllOptions(List.of("Totem", "Crystal", "Golden Apple", "Shield"))
            .build()));
    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
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
    private final NumberSetting fallDistanceThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fall_distance_threshold")
            .name("Fall")
            .defaultValue(12.0)
            .min(0.0)
            .max(80.0)
            .step(1.0)
            .build()));
    private final BooleanSetting fireTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("fire_totem")
            .name("Fire")
            .defaultValue(true)
            .build()));
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
        if (player == null || !InventoryActions.canUseInventoryMenu(client, player)) {
            return;
        }

        Item desiredItem = this.desiredItem(player);
        if (player.getOffhandItem().is(desiredItem)) {
            return;
        }

        int inventorySlot = this.findItemSlot(player.getInventory(), desiredItem);
        if (inventorySlot < 0) {
            return;
        }

        if (InventoryActions.moveToOffhand(client, player, inventorySlot)) {
            this.cooldownTicks = 5;
        }
    }

    private Item desiredItem(final LocalPlayer player) {
        if (this.emergencyTotem.value() && this.needsTotem(player)) {
            return Items.TOTEM_OF_UNDYING;
        }
        return switch (this.mode.value()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.TOTEM_OF_UNDYING;
        };
    }

    private boolean needsTotem(final LocalPlayer player) {
        float health = player.getHealth();
        if (this.includeAbsorption.value()) {
            health += player.getAbsorptionAmount();
        }
        return health <= this.healthThreshold.value()
                || player.fallDistance >= this.fallDistanceThreshold.value()
                || this.fireTotem.value() && player.isOnFire();
    }

    private int findItemSlot(final Inventory inventory, final Item item) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(item)) {
                return slot;
            }
        }
        return -1;
    }

    static int toInventoryMenuSlot(final int inventorySlot) {
        return InventoryActions.toInventoryMenuSlot(inventorySlot);
    }
}
