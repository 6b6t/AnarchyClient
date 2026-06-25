package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.InventoryAction;
import net.blockhost.anarchyclient.inventory.InventoryActionChain;
import net.blockhost.anarchyclient.inventory.InventoryActionScheduler;
import net.blockhost.anarchyclient.inventory.InventorySlotRef;
import net.blockhost.anarchyclient.inventory.InventorySlots;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.OptionalInt;

public final class AutoSoupModule extends Module {

    private final NumberSetting healthThreshold = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("health_threshold")
            .name("Health")
            .defaultValue(14.0)
            .min(1.0)
            .max(20.0)
            .step(0.5)
            .build()));
    private final BooleanSetting restoreSlot = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("restore_slot")
            .name("Restore")
            .defaultValue(true)
            .build()));
    private final BooleanSetting searchInventory = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("search_inventory")
            .name("Inventory")
            .defaultValue(true)
            .build()));
    private final BooleanSetting avoidClickable = this.setting(BooleanSetting.from(BooleanSetting.builder()
            .id("avoid_clickable")
            .name("Safe Use")
            .defaultValue(true)
            .build()));
    private int previousSlot = -1;

    public AutoSoupModule() {
        super("auto_soup", "Auto Soup", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || client.level == null || client.gui.screen() != null) {
            return;
        }
        if (this.previousSlot >= 0 && !player.isUsingItem()) {
            if (this.restoreSlot.value()) {
                InventoryActions.selectHotbarSlot(player, this.previousSlot);
            }
            this.previousSlot = -1;
        }
        if (player.isUsingItem() || player.getHealth() > this.healthThreshold.value()) {
            return;
        }
        if (this.avoidClickable.value() && isClickable(client.hitResult, client.level)) {
            return;
        }
        InteractionHand hand = soupHand(player.getMainHandItem(), player.getOffhandItem());
        if (hand == null) {
            OptionalInt slot = findSoupSlot(player.getInventory(), 0, Inventory.getSelectionSize() - 1);
            if (slot.isPresent()) {
                int selectedSlot = player.getInventory().getSelectedSlot();
                if (slot.orElseThrow() != selectedSlot) {
                    this.previousSlot = selectedSlot;
                    InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
                }
                hand = InteractionHand.MAIN_HAND;
            } else if (this.searchInventory.value()) {
                this.queueSoupMove(client, player);
                return;
            }
        }
        if (hand != null) {
            client.gameMode.useItem(player, hand);
        }
    }

    private void queueSoupMove(final Minecraft client, final LocalPlayer player) {
        if (!InventoryActionScheduler.canUseInventoryMenu(client, player)) {
            return;
        }
        OptionalInt sourceSlot = findSoupSlot(player.getInventory(), Inventory.getSelectionSize(), Inventory.INVENTORY_SIZE - 1);
        OptionalInt targetSlot = findEmptyHotbarSlot(player.getInventory());
        if (sourceSlot.isEmpty() || targetSlot.isEmpty()) {
            return;
        }
        InventorySlotRef source = InventorySlots.storageSlot(sourceSlot.orElseThrow()).orElse(null);
        InventorySlotRef target = InventorySlots.storageSlot(targetSlot.orElseThrow()).orElse(null);
        if (source == null || target == null) {
            return;
        }
        InventoryActionScheduler.schedule(InventoryActionChain.single(
                this.id(),
                InventoryActionScheduler.PRIORITY_LIFE,
                2,
                InventoryAction.pickupSwap(source, target)
        ));
    }

    private static OptionalInt findEmptyHotbarSlot(final Inventory inventory) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static OptionalInt findSoupSlot(final Inventory inventory, final int start, final int end) {
        for (int slot = start; slot <= end; slot++) {
            if (isSoup(inventory.getItem(slot))) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static InteractionHand soupHand(final ItemStack mainHand, final ItemStack offHand) {
        if (isSoup(mainHand)) {
            return InteractionHand.MAIN_HAND;
        }
        if (isSoup(offHand)) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    static boolean isSoup(final ItemStack stack) {
        return !stack.isEmpty() && isSoupItemId(stack.getItem().builtInRegistryHolder().key().identifier().getPath());
    }

    static boolean isSoupItemId(final String itemId) {
        return "mushroom_stew".equals(itemId) || "rabbit_stew".equals(itemId) || "beetroot_soup".equals(itemId);
    }

    static boolean isClickable(final HitResult hitResult, final Level level) {
        if (hitResult instanceof EntityHitResult entityHitResult) {
            Entity entity = entityHitResult.getEntity();
            return entity instanceof Villager || entity instanceof TamableAnimal;
        }
        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = blockHitResult.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            return block instanceof BaseEntityBlock || block instanceof CraftingTableBlock;
        }
        return false;
    }

    @Override
    protected void onDisable() {
        this.previousSlot = -1;
    }
}
