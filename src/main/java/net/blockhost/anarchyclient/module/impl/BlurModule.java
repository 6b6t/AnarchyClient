package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

public final class BlurModule extends Module {

    public BlurModule() {
        super("blur", "Blur", ModuleCategory.RENDER,
                "Blurs the world behind inventories, containers, and other in-game screens using the vanilla Menu Background Blur strength.");
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (!shouldBlur(client.gui.screen()) || client.options.getMenuBackgroundBlurriness() < 1) {
            return;
        }
        // Vanilla only blurs once per frame and never does so for in-game UI screens, so this cannot collide with it.
        graphics.nextStratum();
        graphics.blurBeforeThisStratum();
    }

    static boolean shouldBlur(final Screen screen) {
        return screen != null && !(screen instanceof AnarchyClientScreen) && screen.isInGameUi();
    }
}
