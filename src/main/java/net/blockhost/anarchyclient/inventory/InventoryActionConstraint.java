package net.blockhost.anarchyclient.inventory;

import net.blockhost.anarchyclient.rotation.RotationManager;

public enum InventoryActionConstraint {
    PLAYER_INVENTORY_MENU {
        @Override
        boolean allows(final InventoryActionContext context) {
            return context.canUsePlayerInventoryMenu();
        }
    },
    SCREEN_CLOSED {
        @Override
        boolean allows(final InventoryActionContext context) {
            return context.client().gui.screen() == null;
        }
    },
    NOT_USING_ITEM {
        @Override
        boolean allows(final InventoryActionContext context) {
            return !context.player().isUsingItem();
        }
    },
    NOT_BREAKING_BLOCK {
        @Override
        boolean allows(final InventoryActionContext context) {
            return context.client().gameMode == null || !context.client().gameMode.isDestroying();
        }
    },
    NO_MOVEMENT {
        @Override
        boolean allows(final InventoryActionContext context) {
            var input = context.player().input;
            return input == null || !input.keyPresses.forward()
                    && !input.keyPresses.backward()
                    && !input.keyPresses.left()
                    && !input.keyPresses.right()
                    && !input.keyPresses.jump();
        }
    },
    NO_ACTIVE_ROTATION {
        @Override
        boolean allows(final InventoryActionContext context) {
            return !RotationManager.hasActiveRequest();
        }
    };

    abstract boolean allows(InventoryActionContext context);
}
