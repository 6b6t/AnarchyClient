package net.blockhost.anarchyclient.inventory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryActionSchedulerTest {

    private static final InventoryActionChain ARMOR = chain("auto_armor", InventoryActionScheduler.PRIORITY_EQUIPMENT);
    private static final InventoryActionChain TOTEM = chain("auto_totem", InventoryActionScheduler.PRIORITY_LIFE);
    private static final InventoryActionChain EAT = chain("smart_eat", InventoryActionScheduler.PRIORITY_LIFE);
    private static final InventoryActionChain MEND = chain("auto_mend", InventoryActionScheduler.PRIORITY_NORMAL);

    @Test
    void ordersChainsByPriorityKeepingLowerOnesAsFallbacks() {
        assertEquals(List.of(TOTEM, ARMOR, MEND),
                InventoryActionScheduler.eligible(List.of(MEND, ARMOR, TOTEM), false, Integer.MIN_VALUE, ""));
    }

    @Test
    void cooldownOnlyHoldsBackChainsOfTheSameOrLowerPriority() {
        assertEquals(List.of(TOTEM),
                InventoryActionScheduler.eligible(List.of(MEND, ARMOR, TOTEM), true, InventoryActionScheduler.PRIORITY_EQUIPMENT, "auto_armor"));
        assertEquals(List.of(),
                InventoryActionScheduler.eligible(List.of(MEND, ARMOR), true, InventoryActionScheduler.PRIORITY_EQUIPMENT, "auto_armor"));
    }

    @Test
    void lifeChainsSkipOnlyAnotherOwnersCooldown() {
        assertEquals(List.of(TOTEM),
                InventoryActionScheduler.eligible(List.of(MEND, TOTEM), true, InventoryActionScheduler.PRIORITY_LIFE, "smart_eat"));
        assertEquals(List.of(EAT),
                InventoryActionScheduler.eligible(List.of(EAT, TOTEM), true, InventoryActionScheduler.PRIORITY_LIFE, "auto_totem"));
    }

    private static InventoryActionChain chain(final String owner, final int priority) {
        return InventoryActionChain.single(owner, priority, 2, InventoryAction.selectHotbarSlot(0));
    }
}
