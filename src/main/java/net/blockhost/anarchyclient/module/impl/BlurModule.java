package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.ui.AnarchyClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class BlurModule extends Module {

    private final NumberSetting opacity = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("opacity")
            .name("Opacity")
            .defaultValue(90.0)
            .min(10.0)
            .max(180.0)
            .step(5.0)
            .build()));

    public BlurModule() {
        super("blur", "Blur", ModuleCategory.RENDER);
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        if (client.gui.screen() == null || client.gui.screen() instanceof AnarchyClientScreen) {
            return;
        }
        int alpha = Math.max(0, Math.min(255, this.opacity.value().intValue()));
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), alpha << 24);
    }
}
