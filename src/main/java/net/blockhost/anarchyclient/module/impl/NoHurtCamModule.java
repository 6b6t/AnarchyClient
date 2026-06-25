package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;

public final class NoHurtCamModule extends Module {

    public NoHurtCamModule() {
        super("no_hurt_cam", "No Hurt Cam", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        RenderSuppression.enable(this.id(), RenderSuppression.Kind.HURT_CAMERA);
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }
}
