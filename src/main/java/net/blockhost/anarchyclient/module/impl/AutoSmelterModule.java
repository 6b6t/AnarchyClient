package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public final class AutoSmelterModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoSmelterModule() {
        super("auto_smelter", "Auto Smelter", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (quickMoveIfPresent(client, menu, 2)
                || quickMoveMatching(client, menu, 3, menu.slots.size(), slot -> menu.slots.get(slot).getItem().is(Items.COAL)
                || menu.slots.get(slot).getItem().is(Items.CHARCOAL))
                || quickMoveMatching(client, menu, 3, menu.slots.size(), slot -> menu.getSlot(0).getItem().isEmpty()
                && !menu.slots.get(slot).getItem().is(Items.COAL)
                && !menu.slots.get(slot).getItem().is(Items.CHARCOAL))) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    private static boolean quickMoveIfPresent(final Minecraft client, final AbstractFurnaceMenu menu, final int slot) {
        return menu.getSlot(slot).hasItem() && ContainerActions.quickMove(client, menu, slot);
    }

    private static boolean quickMoveMatching(final Minecraft client, final AbstractFurnaceMenu menu, final int start,
                                             final int end, final SlotPredicate predicate) {
        for (int slot = start; slot < end; slot++) {
            Slot menuSlot = menu.getSlot(slot);
            if (menuSlot.hasItem() && predicate.test(slot)) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }

    private interface SlotPredicate {
        boolean test(int slot);
    }
}
