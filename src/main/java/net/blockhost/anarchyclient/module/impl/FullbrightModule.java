package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends Module {

    private final NumberSetting brightness = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("brightness")
            .name("Brightness")
            .defaultValue(10.0)
            .min(0.0)
            .max(10.0)
            .step(0.5)
            .build()));
    private Double previousBrightness;

    public FullbrightModule() {
        super("fullbright", "Fullbright", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.previousBrightness == null) {
            this.previousBrightness = client.options.gamma().value;
        }
        client.options.gamma().value = this.brightness.value();
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.previousBrightness != null) {
            client.options.gamma().value = this.previousBrightness;
            this.previousBrightness = null;
        }
    }
}
