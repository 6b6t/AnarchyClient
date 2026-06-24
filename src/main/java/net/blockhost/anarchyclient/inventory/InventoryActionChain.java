package net.blockhost.anarchyclient.inventory;

import java.util.List;

public record InventoryActionChain(String owner, int priority, int delayTicks, List<InventoryAction> actions) {

    public InventoryActionChain {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Inventory action owner is required");
        }
        delayTicks = Math.max(0, delayTicks);
        actions = List.copyOf(actions);
    }

    public static InventoryActionChain single(final String owner, final int priority, final int delayTicks,
                                              final InventoryAction action) {
        return new InventoryActionChain(owner, priority, delayTicks, List.of(action));
    }
}
