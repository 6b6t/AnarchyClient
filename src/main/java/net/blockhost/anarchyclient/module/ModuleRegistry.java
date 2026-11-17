package net.blockhost.anarchyclient.module;

/**
 * Registers the default module set.
 *
 * <p>Modules receive events in registration order, and the event bus has no priorities. Some modules
 * rely on that order. For example, Freecam must handle input after AutoWalk, Sneak, and NoSlow, and
 * AntiCrash must filter packets before the render and movement modules read them. Keep new modules
 * at the end of their group unless they need to run earlier.
 */
public final class ModuleRegistry {

    private ModuleRegistry() {
    }

    public static void registerDefaults(final ModuleManager modules) {
        ServerModules.register(modules);
        CombatModules.register(modules);
        RenderModules.register(modules);
        MovementModules.register(modules);
        PlayerModules.register(modules);
        WorldModules.register(modules);
        InteractionModules.register(modules);
        AutomationModules.register(modules);
        HudModules.register(modules);
        MiscModules.register(modules);
    }
}
