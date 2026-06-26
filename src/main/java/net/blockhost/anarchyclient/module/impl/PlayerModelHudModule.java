package net.blockhost.anarchyclient.module.impl;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

import java.util.List;

public final class PlayerModelHudModule extends HudElementModule {

    public PlayerModelHudModule() {
        super("player_model_hud", "Player Model", "Bottom Left");
    }

    @Override
    protected void renderHudElement(final Minecraft client, final GuiGraphicsExtractor graphics) {
        int width = 62;
        int height = 86;
        HudPosition position = this.position(graphics, width, height);
        int x = position.x();
        int y = position.y();
        graphics.fill(x, y, x + width, y + height, 0x66000000);
        graphics.outline(x, y, width, height, 0x55FFFFFF);
        graphics.text(client.font, client.player.getName().getString(), x + 5, y + 5, this.color(), true);
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                x + 8,
                y + 16,
                x + width - 8,
                y + height - 6,
                28,
                0.0625F,
                x + width / 2.0F,
                y + height / 2.0F,
                client.player
        );
    }

    @Override
    protected List<String> lines(final Minecraft client) {
        return List.of(
                "Pose " + client.player.getPose().name().toLowerCase(java.util.Locale.ROOT),
                "Yaw " + Math.round(client.player.getYRot()),
                "Pitch " + Math.round(client.player.getXRot()),
                "Body " + Math.round(client.player.yBodyRot)
        );
    }
}
