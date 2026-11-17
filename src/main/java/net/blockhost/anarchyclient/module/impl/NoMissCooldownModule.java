package net.blockhost.anarchyclient.module.impl;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleCategory;

public final class NoMissCooldownModule extends Module {

    private static NoMissCooldownModule active;

    public NoMissCooldownModule() {
        super("no_miss_cooldown", "No Miss Cooldown", ModuleCategory.COMBAT,
                "Removes the short attack cooldown that vanilla adds after you click at air, so you can attack again right away.");
    }

    @Override
    protected void onEnable() {
        active = this;
    }

    @Override
    protected void onDisable() {
        if (active == this) {
            active = null;
        }
    }

    public static boolean removesMissCooldown() {
        return active != null;
    }
}
