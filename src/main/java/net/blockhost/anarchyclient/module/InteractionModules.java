package net.blockhost.anarchyclient.module;

import net.blockhost.anarchyclient.module.impl.AntiHungerModule;
import net.blockhost.anarchyclient.module.impl.AntiReducedDebugInfoModule;
import net.blockhost.anarchyclient.module.impl.AutoDropModule;
import net.blockhost.anarchyclient.module.impl.FakeLagModule;
import net.blockhost.anarchyclient.module.impl.GhostHandModule;
import net.blockhost.anarchyclient.module.impl.MoreCarryModule;
import net.blockhost.anarchyclient.module.impl.MultiActionsModule;
import net.blockhost.anarchyclient.module.impl.NameCollectorModule;
import net.blockhost.anarchyclient.module.impl.NoBlockInteractModule;
import net.blockhost.anarchyclient.module.impl.NoEntityInteractModule;
import net.blockhost.anarchyclient.module.impl.NoInteractModule;
import net.blockhost.anarchyclient.module.impl.NoMiningTraceModule;
import net.blockhost.anarchyclient.module.impl.NoRotateSetModule;
import net.blockhost.anarchyclient.module.impl.NoSlotSetModule;
import net.blockhost.anarchyclient.module.impl.PingSpoofModule;
import net.blockhost.anarchyclient.module.impl.PortalMenuModule;
import net.blockhost.anarchyclient.module.impl.ReachModule;
import net.blockhost.anarchyclient.module.impl.RotationModule;

/**
 * Registers the default interaction and connection tweaks modules.
 *
 * <p>Registration order is event dispatch order. See {@link ModuleRegistry}.
 */
final class InteractionModules {

    private InteractionModules() {}

    static void register(final ModuleManager modules) {
        modules.register(new NoMiningTraceModule());
        modules.register(new NoInteractModule());
        modules.register(new GhostHandModule());
        modules.register(new ReachModule());
        modules.register(new RotationModule());
        modules.register(new NoBlockInteractModule());
        modules.register(new NoEntityInteractModule());
        modules.register(new AutoDropModule());
        modules.register(new NoRotateSetModule());
        modules.register(new NoSlotSetModule());
        modules.register(new MoreCarryModule());
        modules.register(new AntiHungerModule());
        modules.register(new AntiReducedDebugInfoModule());
        modules.register(new PingSpoofModule());
        modules.register(new FakeLagModule());
        modules.register(new PortalMenuModule());
        modules.register(new MultiActionsModule());
        modules.register(new NameCollectorModule());
    }
}
