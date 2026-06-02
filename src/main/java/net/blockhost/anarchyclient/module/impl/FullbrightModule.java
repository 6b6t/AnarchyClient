package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class FullbrightModule extends Module {

    private final NumberSetting brightness = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("brightness")
            .name("Brightness")
            .defaultValue(1.0)
            .min(0.0)
            .max(1.0)
            .step(0.05)
            .build()));
    private Double previousBrightness;

    public FullbrightModule() {
        super("fullbright", "Fullbright", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.previousBrightness == null) {
            this.previousBrightness = client.options.gamma().get();
        }
        client.options.gamma().set(this.brightness.value());
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.previousBrightness != null) {
            client.options.gamma().set(this.previousBrightness);
            this.previousBrightness = null;
        }
    }
}
