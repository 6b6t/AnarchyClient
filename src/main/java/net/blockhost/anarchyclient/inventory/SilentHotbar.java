package net.blockhost.anarchyclient.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Predicate;

public final class SilentHotbar {

    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_COMBAT = 50;
    public static final int PRIORITY_LIFE = 100;

    private static Request active;
    private static int previousSlot = -1;

    private SilentHotbar() {
    }

    public static boolean select(final LocalPlayer player, final String owner, final int slot,
                                 final int priority, final int ticks, final boolean restore) {
        if (player == null || owner == null || owner.isBlank() || !Inventory.isHotbarSlot(slot)) {
            return false;
        }
        if (active != null && !active.owner().equals(owner) && active.priority() > priority) {
            return false;
        }
        int selected = player.getInventory().getSelectedSlot();
        if (previousSlot < 0 && selected != slot && restore) {
            previousSlot = selected;
        }
        active = new Request(owner, slot, priority, Math.max(1, ticks), restore);
        player.getInventory().setSelectedSlot(slot);
        return true;
    }

    public static OptionalInt selectMatching(final LocalPlayer player, final String owner,
                                             final Predicate<ItemStack> predicate, final int priority,
                                             final int ticks, final boolean restore) {
        OptionalInt slot = findHotbarSlot(player, predicate);
        if (slot.isPresent() && select(player, owner, slot.orElseThrow(), priority, ticks, restore)) {
            return slot;
        }
        return OptionalInt.empty();
    }

    public static Optional<InteractionHand> usableHand(final LocalPlayer player, final String owner,
                                                       final Predicate<ItemStack> predicate, final int priority,
                                                       final int ticks, final boolean restore) {
        if (player == null) {
            return Optional.empty();
        }
        if (predicate.test(player.getOffhandItem())) {
            return Optional.of(InteractionHand.OFF_HAND);
        }
        return selectMatching(player, owner, predicate, priority, ticks, restore).isPresent()
                ? Optional.of(InteractionHand.MAIN_HAND)
                : Optional.empty();
    }

    public static OptionalInt findHotbarSlot(final LocalPlayer player, final Predicate<ItemStack> predicate) {
        if (player == null || predicate == null) {
            return OptionalInt.empty();
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && predicate.test(stack)) {
                return OptionalInt.of(slot);
            }
        }
        return OptionalInt.empty();
    }

    public static OptionalInt activeSlot() {
        return active == null ? OptionalInt.empty() : OptionalInt.of(active.slot());
    }

    public static Optional<String> activeOwner() {
        return active == null ? Optional.empty() : Optional.of(active.owner());
    }

    public static void tick(final Minecraft client) {
        LocalPlayer player = client == null ? null : client.player;
        if (player == null) {
            clearAll();
            return;
        }
        if (active == null) {
            restore(player);
            return;
        }
        if (active.ticks() <= 0) {
            active = null;
            restore(player);
            return;
        }
        if (Inventory.isHotbarSlot(active.slot())) {
            player.getInventory().setSelectedSlot(active.slot());
        }
        active = active.tickDown();
    }

    public static void clear(final String owner) {
        if (active != null && active.owner().equals(owner)) {
            active = null;
        }
    }

    public static void clearAll() {
        active = null;
        previousSlot = -1;
    }

    private static void restore(final LocalPlayer player) {
        if (previousSlot >= 0 && Inventory.isHotbarSlot(previousSlot)) {
            player.getInventory().setSelectedSlot(previousSlot);
        }
        previousSlot = -1;
    }

    private record Request(String owner, int slot, int priority, int ticks, boolean restore) {

        private Request tickDown() {
            return new Request(this.owner, this.slot, this.priority, this.ticks - 1, this.restore);
        }
    }
}
