package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class AutoBrewerModule extends Module {

    private static final int LAST_BOTTLE_SLOT = 2;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int FIRST_PLAYER_SLOT = 5;

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
        super("auto_brewer", "Auto Brewer", ModuleCategory.WORLD,
                "Moves fuel, nether wart, and water bottles into an open brewing stand between brews and takes out the brewed potions.");
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
                || quickMoveMatching(client, menu, FUEL_SLOT, FUEL_SLOT, stack -> stack.has(DataComponents.BREWING_FUEL))
                || quickMoveMatching(client, menu, INGREDIENT_SLOT, INGREDIENT_SLOT, stack -> stack.is(Items.NETHER_WART))
                || quickMoveMatching(client, menu, 0, LAST_BOTTLE_SLOT, AutoBrewerModule::isWaterBottle)) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    private static boolean quickMoveFinishedPotions(final Minecraft client, final BrewingStandMenu menu) {
        for (int slot = 0; slot <= LAST_BOTTLE_SLOT; slot++) {
            Slot menuSlot = menu.getSlot(slot);
            if (menuSlot.hasItem() && !isWaterBottle(menuSlot.getItem())) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }

    static boolean isWaterBottle(final ItemStack stack) {
        if (!stack.is(Items.POTION) && !stack.is(Items.SPLASH_POTION) && !stack.is(Items.LINGERING_POTION)) {
            return false;
        }
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.WATER);
    }

    /**
     * Quick-moves the first matching inventory stack, but only when one of the stand slots from {@code firstTarget} to
     * {@code lastTarget} can take it. Vanilla sends blaze powder to the ingredient slot when the fuel slot is full.
     */
    private static boolean quickMoveMatching(final Minecraft client, final BrewingStandMenu menu,
                                             final int firstTarget, final int lastTarget,
                                             final Predicate<ItemStack> predicate) {
        for (int slot = FIRST_PLAYER_SLOT; slot < menu.slots.size(); slot++) {
            Slot menuSlot = menu.getSlot(slot);
            if (menuSlot.hasItem() && predicate.test(menuSlot.getItem())
                    && hasRoom(standStacks(menu, firstTarget, lastTarget), menuSlot.getItem())) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }

    private static List<ItemStack> standStacks(final BrewingStandMenu menu, final int firstTarget,
                                               final int lastTarget) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int target = firstTarget; target <= lastTarget; target++) {
            stacks.add(menu.getSlot(target).getItem());
        }
        return stacks;
    }

    static boolean hasRoom(final List<ItemStack> targets, final ItemStack stack) {
        return targets.stream().anyMatch(current -> AutoSmelterModule.canStackInto(current, stack));
    }
}
