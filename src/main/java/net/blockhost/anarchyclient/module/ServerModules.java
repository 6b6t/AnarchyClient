package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AntiCheatDetectModule;
import net.blockhost.anarchyclient.module.impl.AntiExploitModule;
import net.blockhost.anarchyclient.module.impl.AutoConfigModule;
import net.blockhost.anarchyclient.module.impl.FlagCheckModule;
import net.blockhost.anarchyclient.module.impl.PayloadInspectorModule;
import net.blockhost.anarchyclient.module.impl.PluginScannerModule;
import net.blockhost.anarchyclient.module.impl.ServerObserverModule;
import net.blockhost.anarchyclient.module.impl.StaffAlertModule;
import net.blockhost.anarchyclient.module.impl.YggdrasilSignatureFixModule;

/**
 * Registers the default server detection, anti-cheat awareness, and exploit protection modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class ServerModules {

    private ServerModules() {
    }

    static void register(final ModuleManager modules) {
        modules.register(new ServerObserverModule());
        modules.register(new AutoConfigModule(modules));
        modules.register(new AntiCheatDetectModule());
        modules.register(new PluginScannerModule());
        modules.register(new PayloadInspectorModule());
        modules.register(new FlagCheckModule());
        modules.register(new StaffAlertModule(modules));
        modules.register(new AntiExploitModule());
        modules.register(new YggdrasilSignatureFixModule());
    }
}
