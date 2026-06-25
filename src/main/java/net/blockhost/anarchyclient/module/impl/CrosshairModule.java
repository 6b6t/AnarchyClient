package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CrosshairModule extends Module {

    private final NumberSetting size = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("size")
            .name("Size")
            .defaultValue(5.0)
            .min(2.0)
            .max(24.0)
            .step(1.0)
            .build()));
    private final NumberSetting gap = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("gap")
            .name("Gap")
            .defaultValue(3.0)
            .min(0.0)
            .max(16.0)
            .step(1.0)
            .build()));

    public CrosshairModule() {
        super("crosshair", "Crosshair", ModuleCategory.RENDER);
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int size = this.size.value().intValue();
        int gap = this.gap.value().intValue();
        int color = 0xE6FFFFFF;
        graphics.fill(centerX - gap - size, centerY, centerX - gap, centerY + 1, color);
        graphics.fill(centerX + gap, centerY, centerX + gap + size, centerY + 1, color);
        graphics.fill(centerX, centerY - gap - size, centerX + 1, centerY - gap, color);
        graphics.fill(centerX, centerY + gap, centerX + 1, centerY + gap + size, color);
    }
}
