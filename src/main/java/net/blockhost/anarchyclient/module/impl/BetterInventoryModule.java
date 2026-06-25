package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class BetterInventoryModule extends Module {

    private final BooleanSetting stackLimits = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("stack_limits")
            .name("Stack Limits")
            .defaultValue(true)
            .build()));
    private final BooleanSetting damagePercent = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("damage_percent")
            .name("Damage %")
            .defaultValue(true)
            .build()));

    public BetterInventoryModule() {
        super("better_inventory", "Better Inventory", ModuleCategory.PLAYER);
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (this.stackLimits.value() && stack.getMaxStackSize() > 1) {
            lines.add(Component.literal("Stack: " + stack.getCount() + " / " + stack.getMaxStackSize())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (this.damagePercent.value() && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            int percent = Math.round(remaining * 100.0F / stack.getMaxDamage());
            lines.add(Component.literal("Condition: " + percent + "%").withStyle(ChatFormatting.GRAY));
        }
    }

    static int durabilityPercent(final int maxDamage, final int damage) {
        return maxDamage <= 0 ? 100 : Math.round(Math.max(0, maxDamage - damage) * 100.0F / maxDamage);
    }
}
