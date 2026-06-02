package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AutoTotemModule;
import net.blockhost.anarchyclient.module.impl.AutoEatModule;
import net.blockhost.anarchyclient.module.impl.AutoGgModule;
import net.blockhost.anarchyclient.module.impl.AutoSprintModule;
import net.blockhost.anarchyclient.module.impl.ChatSpammerModule;
import net.blockhost.anarchyclient.module.impl.CoordinatesHudModule;
import net.blockhost.anarchyclient.module.impl.EspModule;
import net.blockhost.anarchyclient.module.impl.FullbrightModule;
import net.blockhost.anarchyclient.module.impl.KillAuraModule;
import net.blockhost.anarchyclient.module.impl.NoFallModule;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;
import net.blockhost.anarchyclient.module.impl.TracersModule;

public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static void registerDefaults(final ModuleManager modules) {
        modules.register(new AutoTotemModule());
        modules.register(new KillAuraModule());
        modules.register(new EspModule());
        modules.register(new TracersModule());
        modules.register(new FullbrightModule());
        modules.register(new AutoSprintModule());
        modules.register(new NoFallModule());
        modules.register(new AutoEatModule());
        modules.register(new CoordinatesHudModule());
        modules.register(new AutoGgModule());
        modules.register(new ChatSpammerModule());
        modules.register(new NyanCatGifSpammerModule());
    }
}
