package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

public final class BetterTooltipsModule extends Module {

    private final BooleanSetting durability = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("durability")
            .name("Durability")
            .defaultValue(true)
            .build()));
    private final BooleanSetting containers = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("containers")
            .name("Containers")
            .defaultValue(true)
            .build()));
    private final BooleanSetting potions = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("potions")
            .name("Potions")
            .defaultValue(true)
            .build()));
    private final NumberSetting maxContainerLines = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("container_lines")
            .name("Lines")
            .defaultValue(6.0)
            .min(1.0)
            .max(18.0)
            .step(1.0)
            .build()));

    public BetterTooltipsModule() {
        super("better_tooltips", "Better Tooltips", ModuleCategory.RENDER);
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (this.durability.value() && stack.isDamageableItem()) {
            int remaining = Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
            lines.add(Component.literal("Durability: " + remaining + " / " + stack.getMaxDamage())
                    .withStyle(ChatFormatting.GRAY));
        }
        if (this.containers.value()) {
            addContainerLines(stack, lines, this.maxContainerLines.value().intValue());
        }
        if (this.potions.value()) {
            addPotionLines(stack, lines);
        }
    }

    static void addContainerLines(final ItemStack stack, final List<Component> lines, final int maxLines) {
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (contents == null) {
            return;
        }
        List<ItemStack> items = contents.nonEmptyItemCopyStream().toList();
        if (items.isEmpty()) {
            return;
        }
        lines.add(Component.literal("Contents: " + items.size() + " stacks").withStyle(ChatFormatting.GRAY));
        for (int index = 0; index < Math.min(items.size(), Math.max(1, maxLines)); index++) {
            ItemStack item = items.get(index);
            lines.add(Component.literal(" - " + item.getCount() + "x ")
                    .withStyle(ChatFormatting.DARK_GRAY)
                    .append(item.getHoverName().copy().withStyle(ChatFormatting.GRAY)));
        }
        if (items.size() > maxLines) {
            lines.add(Component.literal(" - +" + (items.size() - maxLines) + " more").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    static void addPotionLines(final ItemStack stack, final List<Component> lines) {
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        if (contents == null || !contents.hasEffects()) {
            return;
        }
        lines.add(Component.literal("Effects").withStyle(ChatFormatting.GRAY));
        for (MobEffectInstance effect : contents.getAllEffects()) {
            lines.add(Component.literal(" - ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.translatable(effect.getDescriptionId()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "")
                            .withStyle(ChatFormatting.GRAY)));
        }
    }
}
