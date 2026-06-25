package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Items;

public final class AutoBrewerModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(4.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .build()));
    private int cooldownTicks;

    public AutoBrewerModule() {
        super("auto_brewer", "Auto Brewer", ModuleCategory.WORLD);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof BrewingStandMenu menu)) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        if (menu.getBrewingTicks() > 0) {
            return;
        }
        if (quickMoveFinishedPotions(client, menu)
                || quickMoveMatching(client, menu, stack -> stack.is(ItemTags.BREWING_FUEL))
                || quickMoveMatching(client, menu, stack -> stack.is(Items.NETHER_WART))
                || quickMoveMatching(client, menu, stack -> stack.is(Items.POTION) || stack.is(Items.GLASS_BOTTLE))) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    private static boolean quickMoveFinishedPotions(final Minecraft client, final BrewingStandMenu menu) {
        for (int slot = 0; slot <= 2; slot++) {
            Slot menuSlot = menu.getSlot(slot);
            if (menuSlot.hasItem() && !menuSlot.getItem().is(Items.POTION)) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }

    private static boolean quickMoveMatching(final Minecraft client, final BrewingStandMenu menu,
                                             final java.util.function.Predicate<net.minecraft.world.item.ItemStack> predicate) {
        for (int slot = 5; slot < menu.slots.size(); slot++) {
            Slot menuSlot = menu.getSlot(slot);
            if (menuSlot.hasItem() && predicate.test(menuSlot.getItem())) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }
}
