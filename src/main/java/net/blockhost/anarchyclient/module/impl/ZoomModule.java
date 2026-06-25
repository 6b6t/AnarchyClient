package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.minecraft.client.Minecraft;

public final class ZoomModule extends Module {

    private final NumberSetting factor = this.setting(NumberSetting.from(NumberSetting.builder()
            .id("factor")
            .name("Factor")
            .defaultValue(3.0)
            .min(1.5)
            .max(10.0)
            .step(0.5)
            .build()));
    private Integer previousFov;

    public ZoomModule() {
        super("zoom", "Zoom", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (client.options == null) {
            return;
        }
        if (this.previousFov == null) {
            this.previousFov = client.options.fov().get();
        }
        int zoomed = zoomedFov(this.previousFov, this.factor.value());
        if (client.options.fov().get() != zoomed) {
            client.options.fov().set(zoomed);
        }
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.previousFov != null && client.options != null) {
            client.options.fov().set(this.previousFov);
        }
        this.previousFov = null;
    }

    static int zoomedFov(final int fov, final double factor) {
        return Math.max(10, (int) Math.round(fov / Math.max(1.0, factor)));
    }
}
