package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.ActiveModulesHudModule;
import net.blockhost.anarchyclient.module.impl.ArmorHudModule;
import net.blockhost.anarchyclient.module.impl.CombatHudModule;
import net.blockhost.anarchyclient.module.impl.CompassHudModule;
import net.blockhost.anarchyclient.module.impl.CoordinatesHudModule;
import net.blockhost.anarchyclient.module.impl.EntityListHudModule;
import net.blockhost.anarchyclient.module.impl.FlagTimelineHudModule;
import net.blockhost.anarchyclient.module.impl.HoleHudModule;
import net.blockhost.anarchyclient.module.impl.InventoryHudModule;
import net.blockhost.anarchyclient.module.impl.ItemHudModule;
import net.blockhost.anarchyclient.module.impl.LagNotifierHudModule;
import net.blockhost.anarchyclient.module.impl.MapHudModule;
import net.blockhost.anarchyclient.module.impl.ModuleInfoHudModule;
import net.blockhost.anarchyclient.module.impl.PacketDebugHudModule;
import net.blockhost.anarchyclient.module.impl.PlayerModelHudModule;
import net.blockhost.anarchyclient.module.impl.PlayerRadarHudModule;
import net.blockhost.anarchyclient.module.impl.PotionTimersHudModule;
import net.blockhost.anarchyclient.module.impl.RadarHudModule;
import net.blockhost.anarchyclient.module.impl.ServerFingerprintHudModule;
import net.blockhost.anarchyclient.module.impl.SilentHotbarModule;
import net.blockhost.anarchyclient.module.impl.TextHudModule;
import net.blockhost.anarchyclient.module.impl.WelcomerHudModule;

/**
 * Registers the default HUD element modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class HudModules {

    private HudModules() {}

    static void register(final ModuleManager modules) {
        modules.register(new CoordinatesHudModule());
        modules.register(new TextHudModule());
        modules.register(new ItemHudModule());
        modules.register(new MapHudModule());
        modules.register(new PlayerModelHudModule());
        modules.register(new HoleHudModule());
        modules.register(new RadarHudModule());
        modules.register(new ArmorHudModule());
        modules.register(new InventoryHudModule());
        modules.register(new PotionTimersHudModule());
        modules.register(new CombatHudModule());
        modules.register(new LagNotifierHudModule());
        modules.register(new CompassHudModule());
        modules.register(new ModuleInfoHudModule(modules));
        modules.register(new PlayerRadarHudModule());
        modules.register(new ActiveModulesHudModule(modules));
        modules.register(new ServerFingerprintHudModule());
        modules.register(new FlagTimelineHudModule());
        modules.register(new PacketDebugHudModule());
        modules.register(new SilentHotbarModule());
        modules.register(new EntityListHudModule());
        modules.register(new WelcomerHudModule());
    }
}
