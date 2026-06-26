package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class BetterBeaconsModule extends Module {

    private final BooleanSetting payments = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("payments")
            .name("Payments")
            .defaultValue(true)
            .build()));
    private final BooleanSetting ranges = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("ranges")
            .name("Ranges")
            .defaultValue(true)
            .build()));
    private final BooleanSetting effects = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("effects")
            .name("Effects")
            .defaultValue(true)
            .build()));

    public BetterBeaconsModule() {
        super("better_beacons", "Better Beacons", ModuleCategory.RENDER);
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        addBeaconTooltip(stack.is(Items.BEACON), lines, this.payments.value(), this.ranges.value(), this.effects.value());
    }

    static void addBeaconTooltip(final boolean beacon, final List<Component> lines, final boolean payments,
                                 final boolean ranges, final boolean effects) {
        if (!beacon) {
            return;
        }
        if (payments) {
            lines.add(Component.literal("Payments: iron, gold, emerald, diamond, netherite").withStyle(ChatFormatting.GRAY));
        }
        if (ranges) {
            lines.add(Component.literal("Ranges: 20, 30, 40, 50 blocks by tier").withStyle(ChatFormatting.GRAY));
        }
        if (effects) {
            lines.add(Component.literal("Tier 1: speed, haste").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("Tier 2: resistance, jump boost").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("Tier 3: strength").withStyle(ChatFormatting.DARK_GRAY));
            lines.add(Component.literal("Tier 4: regeneration or level II primary").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
