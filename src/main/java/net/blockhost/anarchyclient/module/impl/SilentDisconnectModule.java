package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;
import net.minecraft.client.Minecraft;

public final class SilentDisconnectModule extends Module {

    private static boolean active;

    public SilentDisconnectModule() {
        super("silent_disconnect", "Silent Disconnect", ModuleCategory.MISC);
    }

    @Override
    protected void onEnable() {
        active = true;
    }

    @Override
    protected void onDisable() {
        active = false;
    }

    public static boolean shouldSuppress(final Minecraft client) {
        return active && client != null && client.level != null && client.player != null;
    }
}
