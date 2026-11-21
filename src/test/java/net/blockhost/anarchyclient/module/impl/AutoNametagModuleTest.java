package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoNametagModuleTest {

    @Test
    void usesOnlyRenamedNameTags() {
        ItemStack renamed = new ItemStack(Items.NAME_TAG);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Bob"));

        assertTrue(AutoNametagModule.isUsableTag(renamed));
        assertFalse(AutoNametagModule.isUsableTag(new ItemStack(Items.NAME_TAG)));
    }
}
