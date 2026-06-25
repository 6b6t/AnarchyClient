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
        return switch (BlockPlacer.place(client, owner.id(), target, rotate, maxTurnDegrees, itemPredicate)) {
            case FILLED -> PlacementResult.FILLED;
            case PLACED -> PlacementResult.PLACED;
            case WAITING, MISSING_ITEM -> PlacementResult.WAITING;
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
        WAITING
    }

}
