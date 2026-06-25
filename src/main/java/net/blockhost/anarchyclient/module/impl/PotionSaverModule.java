package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class PotionSaverModule extends Module {

    public PotionSaverModule() {
        super("potion_saver", "Potion Saver", ModuleCategory.PLAYER);
    }

    @Override
    public void tick(final Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.gui.screen() != null || !player.isUsingItem() || !isPotion(player.getUseItem())) {
            return;
        }
        if (player.getUseItemRemainingTicks() <= 1) {
            player.stopUsingItem();
        }
    }

    @Override
    public boolean itemUse(final Minecraft client, final InteractionHand hand) {
        return false;
    }

    static boolean isPotion(final ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }
}
