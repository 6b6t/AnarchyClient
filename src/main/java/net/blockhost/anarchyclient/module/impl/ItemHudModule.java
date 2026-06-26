package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
    protected void renderHudElement(final Minecraft client, final GuiGraphicsExtractor graphics) {
        ItemStack main = client.player.getMainHandItem();
        ItemStack offhand = client.player.getOffhandItem();
        int width = 58;
        int height = 30;
        HudPosition position = this.position(graphics, width, height);
        int x = position.x();
        int y = position.y();
        graphics.fill(x, y, x + width, y + height, 0xAA101418);
        graphics.outline(x, y, width, height, 0x55FFFFFF);
        drawSlot(client, graphics, main, x + 6, y + 7);
        drawSlot(client, graphics, offhand, x + 32, y + 7);
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

    private static void drawSlot(final Minecraft client, final GuiGraphicsExtractor graphics, final ItemStack stack,
                                 final int x, final int y) {
        graphics.fill(x - 2, y - 2, x + 18, y + 18, 0x6621262D);
        if (!stack.isEmpty()) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(client.font, stack, x, y);
        }
    }
}
