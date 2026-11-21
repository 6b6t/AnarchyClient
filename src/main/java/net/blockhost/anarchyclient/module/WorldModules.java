package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AirPlaceModule;
import net.blockhost.anarchyclient.module.impl.AutoSignModule;
import net.blockhost.anarchyclient.module.impl.AutoToolModule;
import net.blockhost.anarchyclient.module.impl.BreakDelayModule;
import net.blockhost.anarchyclient.module.impl.BuildHeightModule;
import net.blockhost.anarchyclient.module.impl.ClickTpModule;
import net.blockhost.anarchyclient.module.impl.EChestFarmerModule;
import net.blockhost.anarchyclient.module.impl.ExcavatorModule;
import net.blockhost.anarchyclient.module.impl.FastPlaceModule;
import net.blockhost.anarchyclient.module.impl.FlamethrowerModule;
import net.blockhost.anarchyclient.module.impl.HighwayBuilderModule;
import net.blockhost.anarchyclient.module.impl.InfinityMinerModule;
import net.blockhost.anarchyclient.module.impl.InstantRebreakModule;
import net.blockhost.anarchyclient.module.impl.LavacastModule;
import net.blockhost.anarchyclient.module.impl.LiquidFillerModule;
import net.blockhost.anarchyclient.module.impl.LiquidInteractModule;
import net.blockhost.anarchyclient.module.impl.NoGhostBlocksModule;
import net.blockhost.anarchyclient.module.impl.NoSlowBreakModule;
import net.blockhost.anarchyclient.module.impl.NukerModule;
import net.blockhost.anarchyclient.module.impl.ObsidianFarmModule;
import net.blockhost.anarchyclient.module.impl.PacketMineModule;
import net.blockhost.anarchyclient.module.impl.PainterModule;
import net.blockhost.anarchyclient.module.impl.ScaffoldModule;
import net.blockhost.anarchyclient.module.impl.SpeedMineModule;
import net.blockhost.anarchyclient.module.impl.VeinMinerModule;

/**
 * Registers the default world interaction modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class WorldModules {

    private WorldModules() {}

    static void register(final ModuleManager modules) {
        modules.register(new AutoToolModule());
        modules.register(new FastPlaceModule());
        modules.register(new ScaffoldModule());
        modules.register(new PacketMineModule());
        modules.register(new BreakDelayModule());
        modules.register(new SpeedMineModule());
        modules.register(new NoSlowBreakModule());
        modules.register(new InstantRebreakModule());
        modules.register(new NoGhostBlocksModule());
        modules.register(new VeinMinerModule());
        modules.register(new EChestFarmerModule());
        modules.register(new BuildHeightModule());
        modules.register(new AutoSignModule());
        modules.register(new ExcavatorModule());
        modules.register(new HighwayBuilderModule());
        modules.register(new InfinityMinerModule());
        modules.register(new FlamethrowerModule());
        modules.register(new NukerModule());
        modules.register(new LiquidFillerModule());
        modules.register(new LavacastModule());
        modules.register(new PainterModule());
        modules.register(new ObsidianFarmModule());
        modules.register(new AirPlaceModule());
        modules.register(new LiquidInteractModule());
        modules.register(new ClickTpModule());
    }
}
