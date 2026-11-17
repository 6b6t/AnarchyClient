package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.placement.BlockPlacer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.OptionalInt;
import java.util.function.Predicate;

final class BlockPlacement {

    private BlockPlacement() {
    }

    static PlacementResult place(final Minecraft client, final Module owner, final BlockPos target,
                                 final boolean rotate, final float maxTurnDegrees) {
        return place(client, owner, target, rotate, maxTurnDegrees, stack -> true);
    }

    static PlacementResult place(final Minecraft client, final Module owner, final BlockPos target,
                                 final boolean rotate, final float maxTurnDegrees,
                                 final Predicate<ItemStack> itemPredicate) {
        return place(client, owner, target, rotate, maxTurnDegrees, itemPredicate, BlockPlacer.PlacementOptions.DEFAULT);
    }

    /**
     * Places a block with explicit options. Use {@link BlockPlacer.PlacementOptions#NON_FULL} for
     * blocks without a full collision box, such as string, slabs, soul sand, and cobwebs.
     *
     * <p>Returns {@link PlacementResult#UNPLACEABLE} when no neighbour can support the block or no
     * hotbar item fits the spot (for example because an entity or the player is in the way), and
     * {@link PlacementResult#WAITING} while the placement only needs more time, such as a rotation.
     */
    static PlacementResult place(final Minecraft client, final Module owner, final BlockPos target,
                                 final boolean rotate, final float maxTurnDegrees,
                                 final Predicate<ItemStack> itemPredicate, final BlockPlacer.PlacementOptions options) {
        ClientLevel level = client.level;
        if (level != null && BlockPlacer.needsPlacement(level, target)
                && BlockPlacer.findPlacementTarget(level, target).isEmpty()) {
            return PlacementResult.UNPLACEABLE;
        }
        return switch (BlockPlacer.place(client, owner.id(), target, rotate, maxTurnDegrees, itemPredicate, options)) {
            case FILLED -> PlacementResult.FILLED;
            case PLACED -> PlacementResult.PLACED;
            case WAITING -> PlacementResult.WAITING;
            case MISSING_ITEM -> PlacementResult.UNPLACEABLE;
        };
    }

    static boolean needsPlacement(final ClientLevel level, final BlockPos target) {
        return BlockPlacer.needsPlacement(level, target);
    }

    static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                               final LocalPlayer player, final BlockPos target) {
        return findPlaceableHotbarSlot(inventory, level, player, target, stack -> true);
    }

    static OptionalInt findPlaceableHotbarSlot(final Inventory inventory, final ClientLevel level,
                                               final LocalPlayer player, final BlockPos target,
                                               final Predicate<ItemStack> itemPredicate) {
        return BlockPlacer.findPlaceableHotbarSlot(inventory, level, player, target, itemPredicate);
    }

    static boolean isPlaceableBlock(final ItemStack stack, final ClientLevel level, final LocalPlayer player,
                                    final BlockPos target) {
        return BlockPlacer.isPlaceableBlock(stack, level, player, target);
    }

    enum PlacementResult {
        FILLED,
        PLACED,
        WAITING,
        UNPLACEABLE
    }

}
