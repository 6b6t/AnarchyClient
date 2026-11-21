package net.blockhost.anarchyclient.module.impl;

import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipePropertySet;

public final class AutoSmelterModule extends Module {

    static final int INPUT_SLOT = 0;
    static final int FUEL_SLOT = 1;
    static final int OUTPUT_SLOT = 2;
    private static final int FIRST_PLAYER_SLOT = 3;

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
        super(
                "auto_smelter",
                "Auto Smelter",
                ModuleCategory.WORLD,
                "Takes furnace output, then loads coal or charcoal as fuel and smeltable inventory items as input in an open furnace.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null
                || client.level == null
                || client.gameMode == null
                || !(player.containerMenu instanceof AbstractFurnaceMenu menu)) {
            return;
        }
        if (this.cooldownTicks > 0) {
            this.cooldownTicks--;
            return;
        }
        RecipePropertySet inputs = client.level.recipeAccess().propertySet(inputSet(menu));
        // Each move only happens when its target slot can take the stack, so nothing bounces between
        // the inventory and the hotbar and a full fuel slot never blocks loading the input.
        boolean moved = (menu.getSlot(OUTPUT_SLOT).hasItem()
                        && hasRoomFor(menu, menu.getSlot(OUTPUT_SLOT).getItem())
                        && ContainerActions.quickMove(client, menu, OUTPUT_SLOT))
                || this.load(client, menu, FUEL_SLOT, AutoSmelterModule::isFuel)
                || this.load(client, menu, INPUT_SLOT, stack -> !isFuel(stack) && inputs.test(stack));
        if (moved) {
            this.cooldownTicks = this.delay.value().intValue();
        }
    }

    @Override
    protected void onDisable() {
        this.cooldownTicks = 0;
    }

    private boolean load(
            final Minecraft client,
            final AbstractFurnaceMenu menu,
            final int target,
            final Predicate<ItemStack> accepts) {
        ItemStack current = menu.getSlot(target).getItem();
        for (int slot = FIRST_PLAYER_SLOT; slot < menu.slots.size(); slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (!stack.isEmpty() && accepts.test(stack) && canStackInto(current, stack)) {
                return ContainerActions.quickMove(client, menu, slot);
            }
        }
        return false;
    }

    static boolean isFuel(final ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
    }

    /** Returns whether {@code stack} can be added to a slot that holds {@code current}. */
    static boolean canStackInto(final ItemStack current, final ItemStack stack) {
        return current.isEmpty()
                || (ItemStack.isSameItemSameComponents(current, stack)
                        && current.getCount() < current.getMaxStackSize());
    }

    /** Returns whether a shift-click could move {@code output} into one of the player's inventory slots. */
    static boolean hasRoomFor(final AbstractContainerMenu menu, final ItemStack output) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && canStackInto(slot.getItem(), output)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceKey<RecipePropertySet> inputSet(final AbstractFurnaceMenu menu) {
        if (menu instanceof BlastFurnaceMenu) {
            return RecipePropertySet.BLAST_FURNACE_INPUT;
        }
        if (menu instanceof SmokerMenu) {
            return RecipePropertySet.SMOKER_INPUT;
        }
        return RecipePropertySet.FURNACE_INPUT;
    }
}
