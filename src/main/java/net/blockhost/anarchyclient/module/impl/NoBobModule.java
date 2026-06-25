package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.blockhost.anarchyclient.render.RenderSuppression;

public final class NoBobModule extends Module {

    public NoBobModule() {
        super("no_bob", "No Bob", ModuleCategory.RENDER);
    }

    @Override
    protected void onEnable() {
        RenderSuppression.enable(this.id(), RenderSuppression.Kind.VIEW_BOB);
    }

    @Override
    protected void onDisable() {
        RenderSuppression.disable(this.id());
    }
}
