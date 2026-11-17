package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.inventory.ToolSelector;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.blockhost.anarchyclient.world.MiningControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.SwingAnimation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.OptionalInt;
import java.util.function.Predicate;

final class WorldInteraction {

    private WorldInteraction() {
    }

    /**
     * Right-clicks a face of {@code pos} with a hotbar item. With {@code rotate}, returns
     * {@link ActionResult#WAITING} until the rotation toward the face has reached the server.
     */
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
            if (!RotationManager.serverLookHits(player, new AABB(pos), player.blockInteractionRange() + 1.0)) {
                return ActionResult.WAITING;
            }
        }
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND,
                new BlockHitResult(hit, face, pos, false));
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
            return ActionResult.DONE;
        }
        return ActionResult.WAITING;
    }

    /**
     * Mines {@code pos} with the best hotbar tool. Call it every tick until the block breaks;
     * vanilla does not abort the break while the calls continue. Call
     * {@link #stopBreaking(Minecraft, Module)} when the module gives up on the block.
     */
    static boolean breakBlock(final Minecraft client, final Module owner, final BlockPos pos, final Direction face) {
        return mine(client, owner.id(), pos, face, ToolSelector.ANY);
    }

    /**
     * Mines {@code pos} with the best hotbar tool accepted by {@code tools}. Keeps the current
     * slot when no accepted tool is better, and does not switch when no hotbar item is accepted.
     */
    static boolean breakBlock(final Minecraft client, final Module owner, final BlockPos pos, final Direction face,
                              final Predicate<ItemStack> tools) {
        return mine(client, owner.id(), pos, face, tools);
    }

    /** Mines {@code pos} with whatever is in the main hand now. */
    static boolean breakBlockWithHeldItem(final Minecraft client, final Module owner, final BlockPos pos,
                                          final Direction face) {
        return mine(client, owner.id(), pos, face, null);
    }


    /** Aborts the module's break in progress and gives mining back to vanilla. */
    static void stopBreaking(final Minecraft client, final Module owner) {
        MiningControl.release(client, owner.id());
    }

    static boolean hasHotbarItem(final Inventory inventory, final Predicate<ItemStack> predicate) {
        return InventoryActions.findHotbarSlot(inventory, predicate).isPresent();
    }

    private static boolean mine(final Minecraft client, final String owner, final BlockPos pos, final Direction face,
                                final @Nullable Predicate<ItemStack> tools) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return false;
        }
        if (tools != null) {
            ToolSelector.bestHotbarTool(player.getInventory(), client.level.getBlockState(pos), tools)
                    .ifPresent(slot -> InventoryActions.selectHotbarSlot(player, slot));
        }
        return MiningControl.mine(client, owner, pos, face);
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
