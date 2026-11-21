package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;

public final class FullbrightModule extends Module {

    private static FullbrightModule active;

    private final NumberSetting brightness = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("brightness")
            .name("Brightness")
            .defaultValue(10.0)
            .min(0.0)
            .max(10.0)
            .step(0.5)
            .build()));

    public FullbrightModule() {
        super(
                "fullbright",
                "Fullbright",
                ModuleCategory.RENDER,
                "Raises scene brightness so caves and night areas stay readable without torches.");
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    /** Lightmap brightness to use instead of the gamma option, or -1 while the module is off. */
    public static float brightness() {
        FullbrightModule module = active;
        return module == null ? -1.0F : module.brightness.value().floatValue();
    }
}
