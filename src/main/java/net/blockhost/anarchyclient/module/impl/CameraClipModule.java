package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;

public final class CameraClipModule extends Module {

    public CameraClipModule() {
        super("camera_clip", "Camera Clip", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        RenderSuppression.enable(this.id(), RenderSuppression.Kind.CAMERA_CLIP);
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }
}
