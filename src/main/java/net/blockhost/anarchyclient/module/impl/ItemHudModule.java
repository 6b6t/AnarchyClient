package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class ItemHudModule extends HudElementModule {

    public ItemHudModule() {
        super("item_hud", "Item HUD", "Bottom Right");
    }

    @Override
    protected int color() {
        return 0xFF8EEAD5;
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        ItemStack main = client.player.getMainHandItem();
        ItemStack offhand = client.player.getOffhandItem();
        return List.of(
                "Main " + describe(main),
                "Offhand " + describe(offhand)
        );
    }

    static String describe(final ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "empty";
        }
        return stack.getCount() + "x " + stack.getHoverName().getString();
    }
}
