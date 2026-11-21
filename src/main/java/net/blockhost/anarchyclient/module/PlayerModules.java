package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AntiAfkModule;
import net.blockhost.anarchyclient.module.impl.AutoCraftModule;
import net.blockhost.anarchyclient.module.impl.AutoEnchantModule;
import net.blockhost.anarchyclient.module.impl.AutoExtinguishModule;
import net.blockhost.anarchyclient.module.impl.AutoFishModule;
import net.blockhost.anarchyclient.module.impl.AutoGrindModule;
import net.blockhost.anarchyclient.module.impl.AutoMendModule;
import net.blockhost.anarchyclient.module.impl.AutoQueueModule;
import net.blockhost.anarchyclient.module.impl.AutoRenameModule;
import net.blockhost.anarchyclient.module.impl.AutoReplenishModule;
import net.blockhost.anarchyclient.module.impl.AutoRespawnModule;
import net.blockhost.anarchyclient.module.impl.AutoShopModule;
import net.blockhost.anarchyclient.module.impl.EXPThrowerModule;
import net.blockhost.anarchyclient.module.impl.EndermanLookModule;
import net.blockhost.anarchyclient.module.impl.FastExpModule;
import net.blockhost.anarchyclient.module.impl.FastUseModule;
import net.blockhost.anarchyclient.module.impl.GhostModeModule;
import net.blockhost.anarchyclient.module.impl.ItemSaverModule;
import net.blockhost.anarchyclient.module.impl.NoJumpDelayModule;
import net.blockhost.anarchyclient.module.impl.PotionSaverModule;
import net.blockhost.anarchyclient.module.impl.SmartEatModule;

/**
 * Registers the default player automation modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class PlayerModules {

    private PlayerModules() {}

    static void register(final ModuleManager modules) {
        modules.register(new SmartEatModule());
        modules.register(new AutoReplenishModule());
        modules.register(new EXPThrowerModule());
        modules.register(new FastExpModule());
        modules.register(new AutoMendModule());
        modules.register(new AutoRespawnModule());
        modules.register(new GhostModeModule());
        modules.register(new AutoFishModule());
        modules.register(new AutoExtinguishModule());
        modules.register(new AutoCraftModule());
        modules.register(new AutoQueueModule());
        modules.register(new AutoShopModule());
        modules.register(new AutoRenameModule());
        modules.register(new AutoGrindModule());
        modules.register(new AutoEnchantModule());
        modules.register(new PotionSaverModule());
        modules.register(new FastUseModule());
        modules.register(new EndermanLookModule());
        modules.register(new AntiAfkModule());
        modules.register(new NoJumpDelayModule());
        modules.register(new ItemSaverModule());
    }
}
