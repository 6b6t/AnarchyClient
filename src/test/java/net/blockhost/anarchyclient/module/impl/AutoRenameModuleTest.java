package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoRenameModuleTest {

    private static final Set<Item> TARGETS = Set.of(Items.DIAMOND_SWORD);

    @Test
    void renamesTargetItemsThatDoNotHaveTheNameYet() {
        ItemStack named = new ItemStack(Items.DIAMOND_SWORD);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Slayer"));

        assertTrue(AutoRenameModule.shouldRename(new ItemStack(Items.DIAMOND_SWORD), TARGETS, "Slayer", false, false));
        assertFalse(AutoRenameModule.shouldRename(named, TARGETS, "Slayer", false, false));
        assertFalse(AutoRenameModule.shouldRename(new ItemStack(Items.IRON_SWORD), TARGETS, "Slayer", false, false));
        assertFalse(AutoRenameModule.shouldRename(new ItemStack(Items.DIAMOND_SWORD), TARGETS, " ", false, false));
    }

    @Test
    void labelsShulkersAfterTheirFirstItem() {
        ItemStack shulker = new ItemStack(Items.SHULKER_BOX);
        shulker.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(new ItemStack(Items.OBSIDIAN))));

        assertEquals("Obsidian", AutoRenameModule.targetName(shulker, "Fallback", true, false));
        assertEquals("Fallback", AutoRenameModule.targetName(shulker, "Fallback", false, false));
        assertTrue(AutoRenameModule.shouldRename(shulker, Set.of(), "", true, false));
    }

    @Test
    void takesTheResultOnlyWhenThePlayerCanPay() {
        assertTrue(AutoRenameModule.canTakeResult(false, 5, 5));
        assertFalse(AutoRenameModule.canTakeResult(false, 3, 5));
        assertTrue(AutoRenameModule.canTakeResult(true, 0, 5));
        assertFalse(AutoRenameModule.canTakeResult(true, 30, 0));
    }
}
