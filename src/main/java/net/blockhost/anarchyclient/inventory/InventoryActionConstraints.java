package net.blockhost.anarchyclient.inventory;

import java.util.EnumSet;
import java.util.Set;

public record InventoryActionConstraints(Set<InventoryActionConstraint> constraints) {

    public InventoryActionConstraints {
        constraints = constraints.isEmpty()
                ? Set.of()
                : Set.copyOf(constraints);
    }

    public static InventoryActionConstraints none() {
        return new InventoryActionConstraints(Set.of());
    }

    public static InventoryActionConstraints playerInventory() {
        return new InventoryActionConstraints(EnumSet.of(
                InventoryActionConstraint.PLAYER_INVENTORY_MENU,
                InventoryActionConstraint.SCREEN_CLOSED
        ));
    }

    public static InventoryActionConstraints cautiousPlayerInventory() {
        return new InventoryActionConstraints(EnumSet.of(
                InventoryActionConstraint.PLAYER_INVENTORY_MENU,
                InventoryActionConstraint.SCREEN_CLOSED,
                InventoryActionConstraint.NOT_USING_ITEM,
                InventoryActionConstraint.NOT_BREAKING_BLOCK,
                InventoryActionConstraint.NO_ACTIVE_ROTATION
        ));
    }

    public boolean allows(final InventoryActionContext context) {
        for (InventoryActionConstraint constraint : this.constraints) {
            if (!constraint.allows(context)) {
                return false;
            }
        }
        return true;
    }
}
