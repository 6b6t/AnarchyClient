package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class InventoryCleanerModuleTest {

    @Test
    void strictModeKeepsAnAxeAsAToolAlongsideASword() {
        List<ItemStack> stacks = List.of(
                new ItemStack(Items.NETHERITE_SWORD),
                new ItemStack(Items.WOODEN_AXE),
                new ItemStack(Items.ROTTEN_FLESH));
        InventoryCleanerModule.CleanerOptions options = new InventoryCleanerModule.CleanerOptions(
                true, Set.of(), true, true, false, false, false, false, false, false, false, false);

        assertEquals(2, InventoryCleanerModule.findDropSlot(stacks, -1, options));
    }
}
