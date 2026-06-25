package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class AmbienceModule extends Module {

    private final NumberSetting red = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("red")
            .name("Red")
            .defaultValue(20.0)
            .min(0.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final NumberSetting green = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("green")
            .name("Green")
            .defaultValue(30.0)
            .min(0.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final NumberSetting blue = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("blue")
            .name("Blue")
            .defaultValue(45.0)
            .min(0.0)
            .max(255.0)
            .step(5.0)
            .build()));
    private final NumberSetting alpha = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("alpha")
            .name("Alpha")
            .defaultValue(20.0)
            .min(0.0)
            .max(160.0)
            .step(5.0)
            .build()));

    public AmbienceModule() {
        super("ambience", "Ambience", ModuleCategory.RENDER);
    }

    @Override
    public void renderHud(final Minecraft client, final GuiGraphicsExtractor graphics) {
        int color = argb(this.alpha.value().intValue(), this.red.value().intValue(),
                this.green.value().intValue(), this.blue.value().intValue());
        if ((color >>> 24) > 0) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);
        }
    }

    static int argb(final int alpha, final int red, final int green, final int blue) {
        return (clamp(alpha) << 24) | (clamp(red) << 16) | (clamp(green) << 8) | clamp(blue);
    }

    private static int clamp(final int value) {
        return Math.max(0, Math.min(255, value));
    }
}
