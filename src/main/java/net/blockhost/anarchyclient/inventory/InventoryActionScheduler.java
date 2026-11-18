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
    private static String cooldownOwner = "";

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
     * one. Life chains (a totem swap) also skip a delay another owner set, so a food move cannot hold
     * back a totem; a chain's own delay still applies, which keeps a rejected swap from repeating
     * every tick.
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
        for (InventoryActionChain chain : eligible(pending, coolingDown, cooldownPriority, cooldownOwner)) {
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
            cooldownOwner = chain.owner();
            return;
        }
    }

    /** Orders chains by priority (highest first, then schedule order) and removes those held back by the cooldown. */
    static List<InventoryActionChain> eligible(final List<InventoryActionChain> pending, final boolean coolingDown,
                                               final int cooldownPriority, final String cooldownOwner) {
        return pending.stream()
                .filter(chain -> !coolingDown
                        || chain.priority() > cooldownPriority
                        || chain.priority() >= PRIORITY_LIFE && !chain.owner().equals(cooldownOwner))
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
        cooldownOwner = "";
    }
}
