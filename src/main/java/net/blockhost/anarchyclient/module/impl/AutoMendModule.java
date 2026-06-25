package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

public final class AutoMendModule extends Module {

    private final NumberSetting minDurabilityPercent = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_durability_percent")
            .name("Min %")
            .defaultValue(75.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final BooleanSetting requireMending = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("require_mending")
            .name("Mending")
            .defaultValue(true)
            .build()));
    private final BooleanSetting protectTotem = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("protect_totem")
            .name("Totem")
            .defaultValue(true)
            .build()));
    private final NumberSetting totemHealth = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("totem_health")
            .name("Totem HP")
            .defaultValue(18.0)
            .min(1.0)
            .max(36.0)
            .step(0.5)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(5.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoMendModule() {
        super("auto_mend", "Auto Mend", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || !InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (this.protectTotem.value() && protectedTotem(player, this.totemHealth.value())) {
            return;
        }
        if (isRepairCandidate(player.getOffhandItem(), this.minDurabilityPercent.value(), this.requireMending.value())) {
            return;
        }
        int inventorySlot = findRepairSlot(player.getInventory(), this.minDurabilityPercent.value(), this.requireMending.value());
        if (inventorySlot < 0) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(inventorySlot).orElse(null);
        if (source == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_EQUIPMENT,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.pickupSwap(source, InventorySlots.offhandSlot())
        ));
        this.cooldownTicks = this.delay.value().intValue();
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
    }

    static boolean needsMending(final int maxDamage, final int damageValue, final double minDurabilityPercent) {
        return EXPThrowerModule.durabilityPercent(maxDamage, damageValue) <= minDurabilityPercent;
    }

    static boolean protectedTotem(final float health, final float absorption, final boolean offhandTotem,
                                  final double threshold) {
        return offhandTotem && AutoLogModule.effectiveHealth(health, absorption, true) <= threshold;
    }

    private static boolean protectedTotem(final LocalPlayer player, final double threshold) {
        return protectedTotem(player.getHealth(), player.getAbsorptionAmount(),
                player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), threshold);
    }

    private static int findRepairSlot(final Inventory inventory, final double minDurabilityPercent,
                                      final boolean requireMending) {
        int bestSlot = -1;
        double bestPercent = Double.MAX_VALUE;
        int highestDamage = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!isRepairCandidate(stack, minDurabilityPercent, requireMending)) {
                continue;
            }
            double percent = EXPThrowerModule.durabilityPercent(stack);
            int damage = stack.getDamageValue();
            if (percent < bestPercent || percent == bestPercent && damage > highestDamage) {
                bestSlot = slot;
                bestPercent = percent;
                highestDamage = damage;
            }
        }
        return bestSlot;
    }

    private static boolean isRepairCandidate(final ItemStack stack, final double minDurabilityPercent,
                                             final boolean requireMending) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem()
                || !needsMending(stack.getMaxDamage(), stack.getDamageValue(), minDurabilityPercent)) {
            return false;
        }
        return !requireMending || EquipmentScorer.hasEnchantment(stack, Enchantments.MENDING);
    }
}
