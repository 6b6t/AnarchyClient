package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class AspectModule extends Module {

    private final NumberSetting fovScale = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov_scale")
            .name("FOV Scale")
            .defaultValue(1.0)
            .min(0.5)
            .max(1.8)
            .step(0.05)
            .build()));

    public AspectModule() {
        super("aspect", "Aspect", ModuleCategory.RENDER);
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        return (float) Math.max(1.0, Math.min(180.0, fov * this.fovScale.value()));
    }
}
