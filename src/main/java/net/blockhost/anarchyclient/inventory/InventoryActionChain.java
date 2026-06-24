package net.blockhost.anarchyclient.inventory;

import java.util.List;

public record InventoryActionChain(String owner, int priority, int delayTicks, InventoryActionConstraints constraints,
                                   List<InventoryAction> actions) {

    public InventoryActionChain(final String owner, final int priority, final int delayTicks,
                                final List<InventoryAction> actions) {
        this(owner, priority, delayTicks, InventoryActionConstraints.playerInventory(), actions);
    }

    public InventoryActionChain {
        if (owner == null || owner.isBlank()) {
            throw new IllegalArgumentException("Inventory action owner is required");
        }
        delayTicks = Math.max(0, delayTicks);
        constraints = constraints == null ? InventoryActionConstraints.playerInventory() : constraints;
        actions = List.copyOf(actions);
    }

    public static InventoryActionChain single(final String owner, final int priority, final int delayTicks,
                                              final InventoryAction action) {
        return new InventoryActionChain(owner, priority, delayTicks, List.of(action));
    }

    public static InventoryActionChain single(final String owner, final int priority, final int delayTicks,
                                              final InventoryActionConstraints constraints,
                                              final InventoryAction action) {
        return new InventoryActionChain(owner, priority, delayTicks, constraints, List.of(action));
    }
}
