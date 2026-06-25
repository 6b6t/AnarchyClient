package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

final class ContainerActions {

    private ContainerActions() {
    }

    static boolean quickMove(final Minecraft client, final AbstractContainerMenu menu, final int slot) {
        return click(client, menu, slot, 0, ContainerInput.QUICK_MOVE);
    }

    static boolean throwSlot(final Minecraft client, final AbstractContainerMenu menu, final int slot) {
        return click(client, menu, slot, 1, ContainerInput.THROW);
    }

    static boolean swapWithHotbar(final Minecraft client, final AbstractContainerMenu menu, final int slot,
                                  final int hotbarSlot) {
        return click(client, menu, slot, hotbarSlot, ContainerInput.SWAP);
    }

    static boolean click(final Minecraft client, final AbstractContainerMenu menu, final int slot, final int button,
                         final ContainerInput input) {
        LocalPlayer player = client.player;
        if (player == null || client.gameMode == null || menu == null || player.containerMenu != menu) {
            return false;
        }
        client.gameMode.handleContainerInput(menu.containerId, slot, button, input, player);
        return true;
    }
}
