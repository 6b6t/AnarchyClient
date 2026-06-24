package net.blockhost.anarchyclient.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.Optional;

public record InventoryActionContext(Minecraft client, LocalPlayer player) {

    public InventoryActionContext {
        if (client == null) {
            throw new IllegalArgumentException("Minecraft client is required");
        }
        if (player == null) {
            throw new IllegalArgumentException("Local player is required");
        }
    }

    public static Optional<InventoryActionContext> of(final Minecraft client) {
        if (client == null || client.player == null) {
            return Optional.empty();
        }
        return Optional.of(new InventoryActionContext(client, client.player));
    }

    public boolean canUsePlayerInventoryMenu() {
        return this.client.gameMode != null
                && this.player.containerMenu != null
                && this.player.inventoryMenu != null
                && this.player.containerMenu.containerId == InventoryMenu.CONTAINER_ID;
    }
}
