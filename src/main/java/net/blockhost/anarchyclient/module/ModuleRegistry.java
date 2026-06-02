package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.ActiveModulesHudModule;
import net.blockhost.anarchyclient.module.impl.AntiAfkModule;
import net.blockhost.anarchyclient.module.impl.AutoArmorModule;
import net.blockhost.anarchyclient.module.impl.AutoTotemModule;
import net.blockhost.anarchyclient.module.impl.AutoEatModule;
import net.blockhost.anarchyclient.module.impl.AutoFishModule;
import net.blockhost.anarchyclient.module.impl.AutoGgModule;
import net.blockhost.anarchyclient.module.impl.AutoRespawnModule;
import net.blockhost.anarchyclient.module.impl.AutoSprintModule;
import net.blockhost.anarchyclient.module.impl.AutoToolModule;
import net.blockhost.anarchyclient.module.impl.AutoWeaponModule;
import net.blockhost.anarchyclient.module.impl.BlockEspModule;
import net.blockhost.anarchyclient.module.impl.ChatSpammerModule;
import net.blockhost.anarchyclient.module.impl.CoordinatesHudModule;
import net.blockhost.anarchyclient.module.impl.EagleModule;
import net.blockhost.anarchyclient.module.impl.EspModule;
import net.blockhost.anarchyclient.module.impl.FullbrightModule;
import net.blockhost.anarchyclient.module.impl.ItemEspModule;
import net.blockhost.anarchyclient.module.impl.KillAuraModule;
import net.blockhost.anarchyclient.module.impl.MacroModule;
import net.blockhost.anarchyclient.module.impl.MiddleClickActionModule;
import net.blockhost.anarchyclient.module.impl.NametagsModule;
import net.blockhost.anarchyclient.module.impl.NoFallModule;
import net.blockhost.anarchyclient.module.impl.NyanCatGifSpammerModule;
import net.blockhost.anarchyclient.module.impl.ParkourModule;
import net.blockhost.anarchyclient.module.impl.SafeWalkModule;
import net.blockhost.anarchyclient.module.impl.StorageEspModule;
import net.blockhost.anarchyclient.module.impl.TrajectoriesModule;
import net.blockhost.anarchyclient.module.impl.TracersModule;

public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static void registerDefaults(final ModuleManager modules) {
        modules.register(new AutoTotemModule());
        modules.register(new KillAuraModule());
        modules.register(new AutoWeaponModule());
        modules.register(new AutoArmorModule());
        modules.register(new EspModule());
        modules.register(new TracersModule());
        modules.register(new ItemEspModule());
        modules.register(new StorageEspModule());
        modules.register(new BlockEspModule());
        modules.register(new NametagsModule());
        modules.register(new TrajectoriesModule());
        modules.register(new FullbrightModule());
        modules.register(new AutoSprintModule());
        modules.register(new NoFallModule());
        modules.register(new EagleModule());
        modules.register(new SafeWalkModule());
        modules.register(new ParkourModule());
        modules.register(new AutoEatModule());
        modules.register(new AutoRespawnModule());
        modules.register(new AutoFishModule());
        modules.register(new AntiAfkModule());
        modules.register(new AutoToolModule());
        modules.register(new CoordinatesHudModule());
        modules.register(new ActiveModulesHudModule(modules));
        modules.register(new MacroModule());
        modules.register(new MiddleClickActionModule());
        modules.register(new AutoGgModule());
        modules.register(new ChatSpammerModule());
        modules.register(new NyanCatGifSpammerModule());
    }
}
