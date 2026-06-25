package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class SmoothCameraModule extends Module {

    private Boolean previous;

    public SmoothCameraModule() {
        super("smooth_camera", "Smooth Camera", ModuleCategory.RENDER);
    }

    @Override
    public void tick(final Minecraft client) {
        if (this.previous == null) {
            this.previous = client.options.smoothCamera;
        }
        client.options.smoothCamera = true;
    }

    @Override
    protected void onDisable() {
        Minecraft client = Minecraft.getInstance();
        if (this.previous != null) {
            client.options.smoothCamera = this.previous;
            this.previous = null;
        }
    }
}
