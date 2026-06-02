package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

final class EquipmentScorer {

    private EquipmentScorer() {
    }

    static double weaponScore(final ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0;
        }
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        double damage = modifiers == null ? 1.0 : modifiers.compute(Attributes.ATTACK_DAMAGE, 1.0, EquipmentSlot.MAINHAND);
        double speed = modifiers == null ? 4.0 : modifiers.compute(Attributes.ATTACK_SPEED, 4.0, EquipmentSlot.MAINHAND);
        double weaponBonus = stack.has(DataComponents.WEAPON) ? 4.0 : 0.0;
        double durabilityBonus = durabilityRatio(stack) * 0.25;
        return damage * 2.0 + Math.max(0.0, speed) * 0.3 + weaponBonus + durabilityBonus;
    }

    static double armorScore(final ItemStack stack, final EquipmentSlot slot) {
        if (stack.isEmpty() || !fitsSlot(stack, slot)) {
            return 0.0;
        }
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        double armor = modifiers == null ? 0.0 : modifiers.compute(Attributes.ARMOR, 0.0, slot);
        double toughness = modifiers == null ? 0.0 : modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, slot);
        if (armor <= 0.0 && toughness <= 0.0) {
            return 0.0;
        }
        return armor * 10.0 + toughness * 2.0 + durabilityRatio(stack);
    }

    static boolean fitsSlot(final ItemStack stack, final EquipmentSlot slot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == slot;
    }

    static double durabilityRatio(final ItemStack stack) {
        if (!stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
            return 1.0;
        }
        return Math.max(0.0, (stack.getMaxDamage() - stack.getDamageValue()) / (double) stack.getMaxDamage());
    }
}
