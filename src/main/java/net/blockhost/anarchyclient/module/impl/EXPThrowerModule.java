package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionConstraints;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.OptionalInt;

public final class EXPThrowerModule extends Module {

    private static final List<EquipmentSlot> ARMOR_SLOTS = List.of(
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    );

    private final NumberSetting minArmorPercent = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_armor_percent")
            .name("Armor %")
            .defaultValue(80.0)
            .min(1.0)
            .max(100.0)
            .step(1.0)
            .build()));
    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(3.0)
            .min(0.0)
            .max(40.0)
            .step(1.0)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private int cooldownTicks;

    public EXPThrowerModule() {
        super("exp_thrower", "EXP Thrower", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.gui.screen() != null) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (!needsArmorRepair(player, this.minArmorPercent.value())) {
            return;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), stack -> stack.is(Items.EXPERIENCE_BOTTLE));
        if (slot.isEmpty()) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_EQUIPMENT,
                this.delay.value().intValue(),
                InventoryActionConstraints.cautiousPlayerInventory(),
                InventoryAction.useHotbarItem(slot.orElseThrow(), Items.EXPERIENCE_BOTTLE, this.restoreSlot.value())
        ));
        this.cooldownTicks = this.delay.value().intValue();
    }

    static boolean needsArmorRepair(final Iterable<ItemStack> armor, final double minArmorPercent) {
        for (ItemStack stack : armor) {
            if (durabilityPercent(stack) <= minArmorPercent) {
                return true;
            }
        }
        return false;
    }

    static boolean needsArmorRepair(final LocalPlayer player, final double minArmorPercent) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            if (durabilityPercent(player.getItemBySlot(slot)) <= minArmorPercent) {
                return true;
            }
        }
        return false;
    }

    static double durabilityPercent(final ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 100.0;
        }
        return durabilityPercent(stack.getMaxDamage(), stack.getDamageValue());
    }

    static double durabilityPercent(final int maxDamage, final int damageValue) {
        if (maxDamage <= 0) {
            return 100.0;
        }
        return (maxDamage - damageValue) * 100.0 / maxDamage;
    }
}
