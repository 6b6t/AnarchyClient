package net.blockhost.anarchyclient.test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.ServerPacksSource;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.tags.TagLoader;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Bootstraps the vanilla registries once per test JVM.
 *
 * <p>Since 26.x, item components and tags are bound only when a world's registries and data packs
 * load. Without that step, creating an {@code ItemStack} fails with "Components not bound yet" and tag
 * checks fail with "Tags not bound". This extension finishes the registry bootstrap that Fabric API
 * delays until client startup, loads tags from the vanilla data pack, then binds item components from
 * the vanilla world registries the same way the data generator does.</p>
 */
public final class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (BOOTSTRAPPED.compareAndSet(false, true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            // Fabric API delays the built-in registry freeze until the client starts and then calls this.
            BuiltInRegistries.bootStrap();
            bindVanillaTags();
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                    .build(VanillaRegistries.createWorldLookup())
                    .forEach(DataComponentInitializers.PendingComponents::apply);
        }
    }

    private static void bindVanillaTags() {
        try (MultiPackResourceManager resources = new MultiPackResourceManager(
                PackType.SERVER_DATA,
                List.of(ServerPacksSource.createVanillaPackSource().fullResources()))) {
            TagLoader.loadTagsForExistingRegistries(
                            resources, RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY))
                    .forEach(Registry.PendingTags::apply);
        }
    }
}
