package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;

public final class PortalMenuModule extends Module {

    private static boolean active;

    public PortalMenuModule() {
        super("portal_menu", "Portal Menu", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static boolean active() {
        return active;
    }
}
