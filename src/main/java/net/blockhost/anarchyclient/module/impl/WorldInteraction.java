package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.OptionalInt;
import java.util.function.Predicate;

final class WorldInteraction {

    private WorldInteraction() {
    }

    static ActionResult useOnBlock(final Minecraft client, final Module owner, final BlockPos pos, final Direction face,
                                   final Predicate<ItemStack> itemPredicate, final boolean rotate) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return ActionResult.WAITING;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), itemPredicate);
        if (slot.isEmpty()) {
            return ActionResult.MISSING_ITEM;
        }
        InventoryActions.selectHotbarSlot(player, slot.orElseThrow());
        Vec3 hit = Vec3.atCenterOf(pos).relative(face, 0.5);
        if (rotate) {
            rotateToward(owner, player, hit);
        }
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, pos, false));
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            return ActionResult.DONE;
        }
        return ActionResult.WAITING;
    }

    static boolean breakBlock(final Minecraft client, final BlockPos pos, final Direction face,
                              final Predicate<ItemStack> toolPredicate) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return false;
        }
        OptionalInt slot = InventoryActions.findHotbarSlot(player.getInventory(), toolPredicate);
        slot.ifPresent(value -> InventoryActions.selectHotbarSlot(player, value));
        if (client.gameMode.continueDestroyBlock(pos, face)) {
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    static boolean hasHotbarItem(final Inventory inventory, final Predicate<ItemStack> predicate) {
        return InventoryActions.findHotbarSlot(inventory, predicate).isPresent();
    }

    private static void rotateToward(final Module owner, final LocalPlayer player, final Vec3 target) {
        RotationManager.request(new RotationRequest(
                owner.id(),
                Rotation.lookingAt(target, player.getEyePosition()),
                60,
                90.0F,
                1,
                2.0F,
                RotationTurnMode.STEPPED,
                true
        ));
        RotationManager.apply(player);
    }

    enum ActionResult {
        DONE,
        WAITING,
        MISSING_ITEM
    }
}
