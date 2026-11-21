package net.blockhost.anarchyclient.inventory;

import java.util.OptionalInt;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Picks the hotbar item that mines a block fastest. */
public final class ToolSelector {

    /** Accepts every hotbar item, including an empty selected hand. */
    public static final Predicate<ItemStack> ANY = stack -> true;

    private static final double CORRECT_TOOL_BONUS = 1_000_000.0;

    private ToolSelector() {}

    /**
     * Returns the hotbar slot whose item mines {@code state} best among the items accepted by
     * {@code candidates}. Items that can harvest the block's drops rank above faster items that
     * cannot. The selected slot wins ties, so the hand only changes for a better tool. Empty
     * slots are only considered when they are the selected slot.
     */
    public static OptionalInt bestHotbarTool(
            final Inventory inventory, final BlockState state, final Predicate<ItemStack> candidates) {
        int selected = inventory.getSelectedSlot();
        int bestSlot = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        if (Inventory.isHotbarSlot(selected) && candidates.test(inventory.getItem(selected))) {
            bestSlot = selected;
            bestScore = score(inventory.getItem(selected), state);
        }
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (slot == selected || stack.isEmpty() || !candidates.test(stack)) {
                continue;
            }
            double score = score(stack, state);
            if (score > bestScore) {
                bestSlot = slot;
                bestScore = score;
            }
        }
        return bestSlot < 0 ? OptionalInt.empty() : OptionalInt.of(bestSlot);
    }

    /** Ranks a tool for a block: harvesting the drops first, then destroy speed. */
    public static double score(final ItemStack stack, final BlockState state) {
        boolean harvests = !state.requiresCorrectToolForDrops() || stack.isCorrectToolForDrops(state);
        return (harvests ? CORRECT_TOOL_BONUS : 0.0) + stack.getDestroySpeed(state);
    }
}
