package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ArmorHudModule extends HudElementModule {

    private static final List<ArmorLine> ARMOR = List.of(
            new ArmorLine("Helm", EquipmentSlot.HEAD),
            new ArmorLine("Chest", EquipmentSlot.CHEST),
            new ArmorLine("Legs", EquipmentSlot.LEGS),
            new ArmorLine("Boots", EquipmentSlot.FEET)
    );

    public ArmorHudModule() {
        super("armor_hud", "Armor HUD", "Bottom Left");
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        List<String> lines = new ArrayList<>();
        for (ArmorLine armor : ARMOR) {
            ItemStack stack = client.player.getItemBySlot(armor.slot());
            lines.add(armor.label() + " " + describe(stack));
        }
        return lines;
    }

    static String describe(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        if (stack.isDamageableItem()) {
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            int percent = stack.getMaxDamage() <= 0 ? 100 : Math.round(remaining * 100.0F / stack.getMaxDamage());
            return stack.getHoverName().getString() + " " + percent + "%";
        }
        return stack.getHoverName().getString();
    }

    private record ArmorLine(String label, EquipmentSlot slot) {
    }
}
