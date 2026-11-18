package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoEnchantModuleTest {

    @Test
    void aBlankListMatchesEveryEnchantableItem() {
        assertTrue(AutoEnchantModule.shouldEnchantCandidate(new ItemStack(Items.DIAMOND_SWORD), Set.of(), false));
    }

    @Test
    void aListWithOnlyUnknownIdsMatchesNothing() {
        // "diamond_swrod" resolves to nothing, which must not turn into "enchant everything".
        Set<Item> parsed = ItemScan.parseItems("minecraft:diamond_swrod");

        assertFalse(AutoEnchantModule.shouldEnchantCandidate(new ItemStack(Items.DIAMOND_SWORD), parsed, true));
        assertTrue(AutoEnchantModule.shouldEnchantCandidate(new ItemStack(Items.DIAMOND_SWORD),
                ItemScan.parseItems("minecraft:diamond_sword"), true));
    }
}
