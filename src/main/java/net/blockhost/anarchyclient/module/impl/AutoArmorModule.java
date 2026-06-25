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
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Set;

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
    private final SelectSetting preferredProtection = this.setting(SelectSetting.from(SelectSetting.builder()
            .id("preferred_protection")
            .name("Protection")
            .defaultValue("Protection")
            .addAllOptions(List.of("Protection", "Blast", "Fire", "Projectile"))
            .build()));
    private final StringSetting avoidedEnchantments = this.setting(StringSetting.from(StringSetting.builder()
            .id("avoided_enchantments")
            .name("Avoid")
            .defaultValue("binding_curse, frost_walker")
            .build()));
    private final BooleanSetting antiBreak = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("anti_break")
            .name("Anti Break")
            .defaultValue(false)
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

        EquipmentScorer.ProtectionPreference protection = EquipmentScorer.ProtectionPreference.fromSetting(this.preferredProtection.value());
        Set<String> avoided = EquipmentScorer.parseIdentifiers(this.avoidedEnchantments.value());
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack current = player.getItemBySlot(slot);
            if (slot == EquipmentSlot.CHEST && this.keepElytra.value() && current.has(net.minecraft.core.component.DataComponents.GLIDER)) {
                continue;
            }
            if (EquipmentScorer.hasEnchantment(current, net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE)) {
                continue;
            }
            int inventorySlot = bestArmorSlot(player.getInventory(), slot, current, protection, avoided, this.antiBreak.value());
            if (inventorySlot >= 0) {
                InventorySlotRef source = InventorySlots.storageSlot(inventorySlot).orElse(null);
                if (source == null) {
                    continue;
                }
                InventoryAction action = InventoryAction.pickupSwap(
                        source,
                        InventorySlots.armorSlot(slot)
                );
                InventoryActionScheduler.schedule(InventoryActionChain.single(
                        this.id(),
                        InventoryActionScheduler.PRIORITY_EQUIPMENT,
                        this.delayTicksSetting.value().intValue(),
                        InventoryActionConstraints.cautiousPlayerInventory(),
                        action
                ));
                return;
            }
        }
    }

    static int bestArmorSlot(final Inventory inventory, final EquipmentSlot slot, final ItemStack current) {
        return bestArmorSlot(inventory, slot, current, EquipmentScorer.ProtectionPreference.PROTECTION, Set.of(), false);
    }

    static int bestArmorSlot(final Inventory inventory, final EquipmentSlot slot, final ItemStack current,
                             final EquipmentScorer.ProtectionPreference protection, final Set<String> avoidedEnchantments,
                             final boolean antiBreak) {
        double currentScore = EquipmentScorer.armorScore(current, slot, protection, avoidedEnchantments, antiBreak);
        int bestSlot = -1;
        double bestScore = currentScore;
        for (int inventorySlot = 0; inventorySlot < inventory.getContainerSize(); inventorySlot++) {
            ItemStack stack = inventory.getItem(inventorySlot);
            double score = EquipmentScorer.armorScore(stack, slot, protection, avoidedEnchantments, antiBreak);
            if (score > bestScore + 0.1) {
                bestScore = score;
                bestSlot = inventorySlot;
            }
        }
        return bestSlot;
    }
}
