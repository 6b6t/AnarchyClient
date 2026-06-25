package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Locale;
import java.util.Set;

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
        return armorScore(stack, slot, ProtectionPreference.PROTECTION, Set.of(), false);
    }

    static double armorScore(final ItemStack stack, final EquipmentSlot slot, final ProtectionPreference protection,
                             final Set<String> avoidedEnchantments, final boolean antiBreak) {
        if (stack.isEmpty() || !fitsSlot(stack, slot)) {
            return 0.0;
        }
        if (antiBreak && lowDurability(stack)) {
            return 0.0;
        }
        ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        double armor = modifiers == null ? 0.0 : modifiers.compute(Attributes.ARMOR, 0.0, slot);
        double toughness = modifiers == null ? 0.0 : modifiers.compute(Attributes.ARMOR_TOUGHNESS, 0.0, slot);
        if (armor <= 0.0 && toughness <= 0.0) {
            return 0.0;
        }
        double enchantmentScore = enchantmentScore(stack, protection, avoidedEnchantments);
        return armor * 10.0 + toughness * 2.0 + durabilityRatio(stack) + enchantmentScore;
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

    static boolean lowDurability(final ItemStack stack) {
        return stack.isDamageableItem() && stack.getMaxDamage() - stack.getDamageValue() <= 10;
    }

    static boolean hasEnchantment(final ItemStack stack, final ResourceKey<Enchantment> enchantment) {
        return enchantmentLevel(stack, enchantment) > 0;
    }

    static double enchantmentScore(final ItemStack stack, final ProtectionPreference protection,
                                   final Set<String> avoidedEnchantments) {
        double score = 0.0;
        score += 3.0 * enchantmentLevel(stack, protection.enchantment());
        score += enchantmentLevel(stack, Enchantments.PROTECTION);
        score += enchantmentLevel(stack, Enchantments.BLAST_PROTECTION);
        score += enchantmentLevel(stack, Enchantments.FIRE_PROTECTION);
        score += enchantmentLevel(stack, Enchantments.PROJECTILE_PROTECTION);
        score += enchantmentLevel(stack, Enchantments.UNBREAKING);
        score += 2.0 * enchantmentLevel(stack, Enchantments.MENDING);
        for (var enchantment : stack.getEnchantments().entrySet()) {
            if (matchesAny(enchantment.getKey().unwrapKey().map(key -> key.identifier().toString()).orElse(""), avoidedEnchantments)) {
                score -= 100.0;
            }
        }
        return score;
    }

    static int enchantmentLevel(final ItemStack stack, final ResourceKey<Enchantment> enchantment) {
        for (var entry : stack.getEnchantments().entrySet()) {
            if (entry.getKey().is(enchantment)) {
                return entry.getIntValue();
            }
        }
        return 0;
    }

    static Set<String> parseIdentifiers(final String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(value.split("[,;|]"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> token.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    static boolean matchesAny(final String identifier, final Set<String> candidates) {
        String normalized = identifier.toLowerCase(Locale.ROOT);
        String path = normalized.contains(":") ? normalized.substring(normalized.indexOf(':') + 1) : normalized;
        return candidates.contains(normalized) || candidates.contains(path);
    }

    enum ProtectionPreference {
        PROTECTION(Enchantments.PROTECTION),
        BLAST(Enchantments.BLAST_PROTECTION),
        FIRE(Enchantments.FIRE_PROTECTION),
        PROJECTILE(Enchantments.PROJECTILE_PROTECTION);

        private final ResourceKey<Enchantment> enchantment;

        ProtectionPreference(final ResourceKey<Enchantment> enchantment) {
            this.enchantment = enchantment;
        }

        private ResourceKey<Enchantment> enchantment() {
            return this.enchantment;
        }

        static ProtectionPreference fromSetting(final String value) {
            return switch (value) {
                case "Blast" -> BLAST;
                case "Fire" -> FIRE;
                case "Projectile" -> PROJECTILE;
                default -> PROTECTION;
            };
        }
    }
}
