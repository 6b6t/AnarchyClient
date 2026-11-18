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
    private static int cooldownPriority = Integer.MIN_VALUE;

    private InventoryActionScheduler() {
    }

    /** Queues a chain for this tick, replacing the owner's earlier chain. Chains must be scheduled every tick. */
    public static void schedule(final InventoryActionChain chain) {
        if (!chain.actions().isEmpty()) {
            PENDING.removeIf(candidate -> candidate.owner().equals(chain.owner()));
            PENDING.add(chain);
        }
    }

    /**
     * Runs the highest-priority chain that can run now. A chain whose constraints or actions are
     * blocked is skipped in favour of the next one. After a chain runs, its delay only holds back
     * chains of the same or lower priority, so a higher-priority chain is never delayed by a lower
     * one. Life chains (a totem swap) ignore the delay entirely, since a second pop can come within
     * a few ticks.
     */
    public static void tick(final Minecraft client) {
        boolean coolingDown = cooldownTicks > 0;
        if (coolingDown) {
            cooldownTicks--;
        }
        List<InventoryActionChain> pending = List.copyOf(PENDING);
        PENDING.clear();
        if (pending.isEmpty()) {
            return;
        }
        InventoryActionContext context = InventoryActionContext.of(client).orElse(null);
        if (context == null) {
            return;
        }
        for (InventoryActionChain chain : eligible(pending, coolingDown, cooldownPriority)) {
            if (!canRun(chain, context)) {
                continue;
            }
            for (InventoryAction action : chain.actions()) {
                if (!action.execute(context)) {
                    return;
                }
            }
            cooldownTicks = chain.delayTicks();
            cooldownPriority = chain.priority();
            return;
        }
    }

    /** Orders chains by priority (highest first, then schedule order) and removes those held back by the cooldown. */
    static List<InventoryActionChain> eligible(final List<InventoryActionChain> pending, final boolean coolingDown,
                                               final int cooldownPriority) {
        return pending.stream()
                .filter(chain -> !coolingDown || chain.priority() >= PRIORITY_LIFE || chain.priority() > cooldownPriority)
                .sorted(Comparator.comparingInt(InventoryActionChain::priority).reversed())
                .toList();
    }

    private static boolean canRun(final InventoryActionChain chain, final InventoryActionContext context) {
        if (!chain.constraints().allows(context)) {
            return false;
        }
        for (InventoryAction action : chain.actions()) {
            if (!action.canExecute(context)) {
                return false;
            }
        }
        return true;
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
        cooldownPriority = Integer.MIN_VALUE;
    }
}
