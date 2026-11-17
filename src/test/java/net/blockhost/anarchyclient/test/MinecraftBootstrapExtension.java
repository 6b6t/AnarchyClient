package net.blockhost.anarchyclient.test;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bootstraps the vanilla registries once per test JVM.
 *
 * <p>Since 26.x, item components are bound only when a world's registries load. Without that step,
 * creating an {@code ItemStack} fails with "Components not bound yet". This extension builds the
 * vanilla world registries the same way the data generator does and binds the components, so tests
 * can create real item stacks.</p>
 */
public final class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (BOOTSTRAPPED.compareAndSet(false, true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createWorldLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        }
    }
}
