package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AutoBreedModule;
import net.blockhost.anarchyclient.module.impl.AutoBrewerModule;
import net.blockhost.anarchyclient.module.impl.AutoFarmModule;
import net.blockhost.anarchyclient.module.impl.AutoMountModule;
import net.blockhost.anarchyclient.module.impl.AutoNametagModule;
import net.blockhost.anarchyclient.module.impl.AutoShearerModule;
import net.blockhost.anarchyclient.module.impl.AutoSmelterModule;
import net.blockhost.anarchyclient.module.impl.AutoSmithModule;
import net.blockhost.anarchyclient.module.impl.AutoTameModule;
import net.blockhost.anarchyclient.module.impl.AutoTradeModule;
import net.blockhost.anarchyclient.module.impl.ChestAuraModule;
import net.blockhost.anarchyclient.module.impl.ChestCleanerModule;
import net.blockhost.anarchyclient.module.impl.ChestStealerModule;
import net.blockhost.anarchyclient.module.impl.ChestSwapModule;
import net.blockhost.anarchyclient.module.impl.EasyPearlModule;
import net.blockhost.anarchyclient.module.impl.ElytraSwapModule;
import net.blockhost.anarchyclient.module.impl.InventoryCleanerModule;
import net.blockhost.anarchyclient.module.impl.InventorySorterModule;
import net.blockhost.anarchyclient.module.impl.LawnBotModule;
import net.blockhost.anarchyclient.module.impl.MossBotModule;
import net.blockhost.anarchyclient.module.impl.SpawnProoferModule;
import net.blockhost.anarchyclient.module.impl.StashFinderModule;
import net.blockhost.anarchyclient.module.impl.StrongholdFinderModule;
import net.blockhost.anarchyclient.module.impl.TreeAuraModule;

/**
 * Registers the default inventory and farming automation modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class AutomationModules {

    private AutomationModules() {
    }

    static void register(final ModuleManager modules) {
        modules.register(new ChestSwapModule());
        modules.register(new ChestStealerModule());
        modules.register(new ChestCleanerModule());
        modules.register(new InventoryCleanerModule());
        modules.register(new ChestAuraModule());
        modules.register(new ElytraSwapModule());
        modules.register(new EasyPearlModule());
        modules.register(new AutoFarmModule());
        modules.register(new AutoShearerModule());
        modules.register(new AutoBreedModule());
        modules.register(new AutoMountModule());
        modules.register(new SpawnProoferModule());
        modules.register(new StashFinderModule());
        modules.register(new StrongholdFinderModule());
        modules.register(new AutoSmelterModule());
        modules.register(new AutoBrewerModule());
        modules.register(new AutoNametagModule());
        modules.register(new TreeAuraModule());
        modules.register(new MossBotModule());
        modules.register(new LawnBotModule());
        modules.register(new AutoTameModule());
        modules.register(new InventorySorterModule());
        modules.register(new AutoTradeModule());
        modules.register(new AutoSmithModule());
    }
}
