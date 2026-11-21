package net.blockhost.anarchyclient.module.impl;

import java.util.Optional;
import java.util.OptionalInt;
import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.inventory.SilentHotbar;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

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
    private static MainhandTotemModule active;
    private int heldSlot = -1;

    public MainhandTotemModule() {
        super(
                "mainhand_totem",
                "Mainhand Totem",
                ModuleCategory.COMBAT,
                "Switches to a totem of undying in your main hand when you are about to die, so you hold one in each "
                        + "hand together with Auto Totem. Pulls a totem into the hotbar when needed.");
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean danger = TotemRiskEstimator.needsTotem(
                player,
                client.level,
                true,
                false,
                this.health.value(),
                this.falling.value() ? 12.0 : Double.MAX_VALUE,
                false,
                this.predictExplosions.value(),
                8.0);
        if (!danger) {
            this.release();
            return;
        }
        OptionalInt hotbarTotem = isTotem(player.getMainHandItem())
                ? OptionalInt.of(inventory.getSelectedSlot())
                : InventoryActions.findHotbarSlot(inventory, MainhandTotemModule::isTotem);
        if (hotbarTotem.isPresent()) {
            // Renewed every tick at life priority, so combat modules' silent swaps cannot take the hand back.
            // SilentHotbar switches back on its own once this stops, when Switch Back is on.
            if (SilentHotbar.select(
                    player,
                    this.id(),
                    hotbarTotem.getAsInt(),
                    SilentHotbar.PRIORITY_LIFE,
                    3,
                    this.switchBack.value())) {
                this.heldSlot = hotbarTotem.getAsInt();
            }
            return;
        }
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt stored = storageTotemSlot(inventory);
        if (stored.isEmpty()) {
            this.debugValue("state", "no totems");
            return;
        }
        int slot = this.totemSlot.value().intValue() - 1;
        Optional<InventorySlotRef> source = InventorySlots.storageSlot(stored.getAsInt());
        Optional<InventorySlotRef> target = InventorySlots.storageSlot(slot);
        if (source.isPresent() && target.isPresent()) {
            // Through the shared queue, so this never interleaves with Auto Totem's own offhand swap.
            InventoryActionScheduler.schedule(InventoryActionChain.single(
                    this.id(),
                    InventoryActionScheduler.PRIORITY_LIFE,
                    2,
                    InventoryAction.pickupSwap(source.get(), target.get())));
        }
    }

    private void release() {
        SilentHotbar.clear(this.id());
        this.heldSlot = -1;
    }

    /** The hotbar slot this module is holding a totem in, or -1. Auto Totem leaves that totem alone. */
    static int heldSlot() {
        MainhandTotemModule module = active;
        return module == null ? -1 : module.heldSlot;
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
        this.release();
        if (active == this) {
            active = null;
        }
    }
}
