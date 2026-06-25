package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.rotation.Rotation;
import net.blockhost.anarchyclient.rotation.RotationManager;
import net.blockhost.anarchyclient.rotation.RotationRequest;
import net.blockhost.anarchyclient.rotation.RotationTurnMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;
import java.util.OptionalInt;

final class BlockPlacement {

    private BlockPlacement() {
    }

    static PlacementResult place(final Minecraft client, final Module owner, final BlockPos target,
                                 final boolean rotate, final float maxTurnDegrees) {
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.gameMode == null) {
            return PlacementResult.WAITING;
        }
        if (!needsPlacement(client.level, target)) {
            return PlacementResult.FILLED;
        }
        Optional<PlacementTarget> placementTarget = findPlacementTarget(client.level, target);
        if (placementTarget.isEmpty()) {
            return PlacementResult.WAITING;
        }
        OptionalInt hotbarSlot = findPlaceableHotbarSlot(player.getInventory(), client.level, player, target);
        if (hotbarSlot.isEmpty()) {
            return PlacementResult.WAITING;
        }
        InventoryActions.selectHotbarSlot(player, hotbarSlot.orElseThrow());
        PlacementTarget placement = placementTarget.orElseThrow();
        if (rotate) {
            Rotation rotation = Rotation.lookingAt(placement.hitLocation(), player.getEyePosition());
            RotationManager.request(new RotationRequest(
                    owner.id(),
                    rotation,
                    70,
                    maxTurnDegrees,
                    2,
                    2.0F,
                    RotationTurnMode.STEPPED,
                    true
            ));
            RotationManager.apply(player);
        }
        InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, placement.hitResult());
        if (result.consumesAction()) {
            player.swing(InteractionHand.MAIN_HAND);
            return PlacementResult.PLACED;
        }
        return PlacementResult.WAITING;
    }

    static boolean needsPlacement(final ClientLevel level, final BlockPos target) {
        return level.getBlockState(target).canBeReplaced();
    }

    static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                               final LocalPlayer player, final BlockPos target) {
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (isPlaceableBlock(stack, level, player, target)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    static boolean isPlaceableBlock(final ItemStack stack, final ClientLevel level, final LocalPlayer player,
                                    final BlockPos target) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        Block block = blockItem.getBlock();
        BlockState state = block.defaultBlockState();
        if (!needsPlacement(level, target)
                || !Block.isShapeFullBlock(state.getCollisionShape(level, target))
                || block instanceof FallingBlock && FallingBlock.isFree(level.getBlockState(target.below()))) {
            return false;
        }
        return level.isUnobstructed(state, target, CollisionContext.placementContext(player));
    }

    private static Optional<PlacementTarget> findPlacementTarget(final ClientLevel level, final BlockPos target) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = target.relative(direction);
            Direction face = direction.getOpposite();
            if (level.getBlockState(neighbor).isFaceSturdy(level, neighbor, face)) {
                Vec3 hitLocation = Vec3.atCenterOf(neighbor).relative(face, 0.5);
                return Optional.of(new PlacementTarget(new BlockHitResult(hitLocation, face, neighbor, false), hitLocation));
            }
        }
        return Optional.empty();
    }

    enum PlacementResult {
        FILLED,
        PLACED,
        WAITING
    }

    private record PlacementTarget(BlockHitResult hitResult, Vec3 hitLocation) {
    }
}
