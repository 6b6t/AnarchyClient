package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public final class BetterBeaconsModule extends Module {

    public BetterBeaconsModule() {
        super("better_beacons", "Better Beacons", ModuleCategory.RENDER);
    }

    @Override
    public void itemTooltip(final Minecraft client, final ItemStack stack, final List<Component> lines) {
        addBeaconTooltip(stack.is(Items.BEACON), lines);
    }

    static void addBeaconTooltip(final boolean beacon, final List<Component> lines) {
        if (!beacon) {
            return;
        }
        lines.add(Component.literal("Payments: iron, gold, emerald, diamond, netherite"));
        lines.add(Component.literal("Range: 20/30/40/50 blocks by pyramid tier"));
    }
}
