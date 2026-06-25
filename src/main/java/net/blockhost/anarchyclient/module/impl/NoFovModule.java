package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class NoFovModule extends Module {

    private final NumberSetting fov = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("fov")
            .name("FOV")
            .defaultValue(90.0)
            .min(30.0)
            .max(120.0)
            .step(1.0)
            .build()));

    public NoFovModule() {
        super("no_fov", "No FOV", ModuleCategory.RENDER);
    }

    @Override
    public float fov(final Minecraft client, final float fov) {
        return this.fov.value().floatValue();
    }
}
