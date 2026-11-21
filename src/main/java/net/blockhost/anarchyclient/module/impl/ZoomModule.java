package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class ZoomModule extends Module {

    private static final float MIN_FOV = 1.0F;

    private final NumberSetting factor = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("factor")
            .name("Factor")
            .defaultValue(3.0)
            .min(1.5)
            .max(10.0)
            .step(0.5)
            .build()));

    public ZoomModule() {
        super(
                "zoom",
                "Zoom",
                ModuleCategory.RENDER,
                "Narrows your rendered field of view by a set factor to zoom in. Your FOV option is not changed.");
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        return zoomedFov(fov, this.factor.value());
    }

    static float zoomedFov(final float fov, final double factor) {
        return Math.max(MIN_FOV, (float) (fov / Math.max(1.0, factor)));
    }
}
