package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;

public final class PortalsModule extends Module {

    private static boolean active;

    public PortalsModule() {
        super("portals", "Portals", ModuleCategory.PLAYER, java.util.List.of("portal_menu"));
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
