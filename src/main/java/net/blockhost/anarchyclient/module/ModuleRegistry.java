package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AutoTotemModule;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;

public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static void registerDefaults(final ModuleManager modules) {
        modules.register(new AutoTotemModule());
        modules.register(new NyanCatGifSpammerModule());
    }
}
