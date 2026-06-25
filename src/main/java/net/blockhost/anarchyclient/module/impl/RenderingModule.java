package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class RenderingModule extends Module {

    private final NumberSetting fovScale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov_scale")
            .name("FOV Scale")
            .defaultValue(1.0)
            .min(0.5)
            .max(1.75)
            .step(0.05)
            .build()));
    private final NumberSetting minimumFov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("min_fov")
            .name("Min FOV")
            .defaultValue(30.0)
            .min(1.0)
            .max(160.0)
            .step(1.0)
            .build()));
    private final NumberSetting maximumFov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("max_fov")
            .name("Max FOV")
            .defaultValue(140.0)
            .min(1.0)
            .max(180.0)
            .step(1.0)
            .build()));

    public RenderingModule() {
        super("rendering", "Rendering", ModuleCategory.RENDER);
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        return clamp((float) (fov * this.fovScale.value()), this.minimumFov.value().floatValue(),
                this.maximumFov.value().floatValue());
    }

    static float clamp(final float value, final float min, final float max) {
        return Math.max(min, Math.min(max, value));
    }
}
