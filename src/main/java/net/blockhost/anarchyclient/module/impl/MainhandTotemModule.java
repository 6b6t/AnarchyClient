package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.OptionalInt;

public final class MainhandTotemModule extends Module {

    private final NumberSetting health = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health")
            .name("Health")
            .defaultValue(8.0)
            .min(0.0)
            .max(36.0)
            .step(0.5)
            .description("Holds a totem once your health, with absorption, is at or below this value.")
            .build()));
    private final BooleanSetting predictExplosions = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("predict_explosions")
            .name("Predict Explosions")
            .defaultValue(true)
            .description("Also holds a totem when a nearby crystal or TNT could bring you below the health value.")
            .build()));
    private final BooleanSetting falling = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("falling")
            .name("Falling")
            .defaultValue(true)
            .description("Also holds a totem while you fall far enough to take heavy damage.")
            .build()));
    private final NumberSetting totemSlot = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("totem_slot")
            .name("Totem Slot")
            .defaultValue(9.0)
            .min(1.0)
            .max(9.0)
            .step(1.0)
            .description("Hotbar slot that receives a totem from your inventory when the hotbar has none.")
            .build()));
    private final BooleanSetting switchBack = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("switch_back")
            .name("Switch Back")
            .defaultValue(true)
            .description("Returns to the previous hotbar slot once you are safe again.")
            .build()));
    private int previousSlot = -1;

    public MainhandTotemModule() {
        super("mainhand_totem", "Mainhand Totem", ModuleCategory.COMBAT,
                "Switches to a totem of undying in your main hand when you are about to die, so you hold one in each "
                        + "hand together with Auto Totem. Pulls a totem into the hotbar when needed.");
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean danger = TotemRiskEstimator.needsTotem(player, client.level, true, false, this.health.value(),
                this.falling.value() ? 12.0 : Double.MAX_VALUE, false, this.predictExplosions.value(), 8.0);
        if (!danger) {
            this.restore(player);
            return;
        }
        if (isTotem(player.getMainHandItem())) {
            return;
        }
        OptionalInt hotbarTotem = InventoryActions.findHotbarSlot(inventory, MainhandTotemModule::isTotem);
        if (hotbarTotem.isPresent()) {
            this.select(player, hotbarTotem.orElseThrow());
            return;
        }
        if (!InventoryActions.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt stored = storageTotemSlot(inventory);
        if (stored.isEmpty()) {
            this.debugValue("state", "no totems");
            return;
        }
        int slot = this.totemSlot.value().intValue() - 1;
        if (InventoryActions.pickupSwap(client, player, InventoryActions.toInventoryMenuSlot(stored.orElseThrow()),
                InventoryActions.toInventoryMenuSlot(slot))) {
            this.select(player, slot);
        }
    }

    private void select(final LocalPlayer player, final int slot) {
        int current = player.getInventory().getSelectedSlot();
        if (current == slot) {
            return;
        }
        if (this.previousSlot < 0) {
            this.previousSlot = current;
        }
        InventoryActions.selectHotbarSlot(player, slot);
    }

    private void restore(final LocalPlayer player) {
        if (this.previousSlot >= 0 && this.switchBack.value() && isTotem(player.getMainHandItem())) {
            InventoryActions.selectHotbarSlot(player, this.previousSlot);
        }
        this.previousSlot = -1;
    }

    /** Finds a totem in the main inventory, skipping the hotbar, armor, and offhand slots. */
    static OptionalInt storageTotemSlot(final Inventory inventory) {
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            if (isTotem(inventory.getItem(slot))) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static boolean isTotem(final ItemStack stack) {
        return stack.is(Items.TOTEM_OF_UNDYING);
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
    }
}
