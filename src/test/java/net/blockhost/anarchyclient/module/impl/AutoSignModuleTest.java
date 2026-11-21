package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.SignText;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MinecraftBootstrapExtension.class)
class AutoSignModuleTest {

    @Test
    void writesOnlyBlankSignsRightAfterPlacing() {
        SignText written =
                SignText.EMPTY.asMutable().setLine(0, Component.literal("keep")).asImmutable();

        assertTrue(AutoSignModule.isFreshlyPlaced(true, SignText.EMPTY, false));
        assertFalse(AutoSignModule.isFreshlyPlaced(false, SignText.EMPTY, false));
        assertFalse(AutoSignModule.isFreshlyPlaced(true, written, false));
    }

    @Test
    void recognizesStandingAndHangingSignItems() {
        assertTrue(AutoSignModule.isSignItem(new ItemStack(Items.OAK_SIGN)));
        assertTrue(AutoSignModule.isSignItem(new ItemStack(Items.OAK_HANGING_SIGN)));
        assertFalse(AutoSignModule.isSignItem(new ItemStack(Items.OAK_PLANKS)));
    }
}
