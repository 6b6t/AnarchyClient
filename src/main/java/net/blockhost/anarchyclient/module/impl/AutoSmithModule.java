package net.blockhost.anarchyclient.module.impl;

import java.util.function.Predicate;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AutoSmithModule extends Module {

    private final NumberSetting delay = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("delay")
            .name("Delay")
            .defaultValue(2.0)
            .min(1.0)
            .max(20.0)
            .step(1.0)
            .description("Ticks between two clicks.")
            .build()));
    private int cooldown;

    public AutoSmithModule() {
        super(
                "auto_smith",
                "Auto Smith",
                ModuleCategory.WORLD,
                "While a smithing table is open, upgrades every diamond tool, weapon, and armor piece in your "
                        + "inventory to netherite, using your upgrade templates and netherite ingots.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || !(player.containerMenu instanceof SmithingMenu menu)) {
            this.cooldown = 0;
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        int click = nextClick(menu);
        if (click < 0) {
            this.debugValue("state", "done");
            return;
        }
        client.gameMode.handleContainerInput(menu.containerId, click, 0, ContainerInput.QUICK_MOVE, player);
        this.cooldown = this.delay.value().intValue();
    }

    /** Returns the menu slot to shift-click next, or -1 when there is nothing left to do. */
    static int nextClick(final SmithingMenu menu) {
        ItemStack result = menu.getSlot(SmithingMenu.RESULT_SLOT).getItem();
        if (!result.isEmpty()) {
            // With a full inventory the shift-click moves nothing, and the next upgrade would have nowhere to go.
            return AutoSmelterModule.hasRoomFor(menu, result) ? SmithingMenu.RESULT_SLOT : -1;
        }
        int base = inventorySlot(menu, AutoSmithModule::isDiamondGear);
        boolean baseReady = !menu.getSlot(SmithingMenu.BASE_SLOT).getItem().isEmpty();
        if (!baseReady && base < 0) {
            return -1;
        }
        if (menu.getSlot(SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()) {
            return inventorySlot(menu, stack -> stack.is(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE));
        }
        if (menu.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem().isEmpty()) {
            return inventorySlot(menu, stack -> stack.is(Items.NETHERITE_INGOT));
        }
        return baseReady ? -1 : base;
    }

    private static int inventorySlot(final SmithingMenu menu, final Predicate<ItemStack> predicate) {
        for (Slot slot : menu.slots) {
            if (slot.container instanceof Inventory && predicate.test(slot.getItem())) {
                return slot.index;
            }
        }
        return -1;
    }

    static boolean isDiamondGear(final ItemStack stack) {
        return !stack.isEmpty()
                && stack.isDamageableItem()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().startsWith("diamond_");
    }

    @Override
    protected void onDisable() {
        this.cooldown = 0;
    }
}
