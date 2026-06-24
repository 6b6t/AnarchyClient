package net.blockhost.anarchyclient.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class InventoryActionScheduler {

    public static final int PRIORITY_NORMAL = 0;
    public static final int PRIORITY_EQUIPMENT = 50;
    public static final int PRIORITY_LIFE = 100;

    private static final List<InventoryActionChain> PENDING = new ArrayList<>();
    private static int cooldownTicks;

    private InventoryActionScheduler() {
    }

    public static void schedule(final InventoryActionChain chain) {
        if (!chain.actions().isEmpty()) {
            PENDING.removeIf(candidate -> candidate.owner().equals(chain.owner()));
            PENDING.add(chain);
        }
    }

    public static void tick(final Minecraft client) {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            PENDING.clear();
            return;
        }
        if (PENDING.isEmpty()) {
            PENDING.clear();
            return;
        }
        InventoryActionContext context = InventoryActionContext.of(client).orElse(null);
        if (context == null) {
            PENDING.clear();
            return;
        }
        InventoryActionChain chain = PENDING.stream()
                .max(Comparator.comparingInt(InventoryActionChain::priority))
                .orElseThrow();
        PENDING.clear();
        if (!chain.constraints().allows(context)) {
            return;
        }
        for (InventoryAction action : chain.actions()) {
            if (!action.canExecute(context)) {
                return;
            }
        }
        for (InventoryAction action : chain.actions()) {
            if (!action.execute(context)) {
                return;
            }
        }
        cooldownTicks = chain.delayTicks();
    }

    public static boolean canUseInventoryMenu(final Minecraft client, final LocalPlayer player) {
        return client.gameMode != null
                && client.gui.screen() == null
                && player.containerMenu != null
                && player.inventoryMenu != null
                && player.containerMenu.containerId == InventoryMenu.CONTAINER_ID;
    }

    public static void clear() {
        PENDING.clear();
        cooldownTicks = 0;
    }
}
