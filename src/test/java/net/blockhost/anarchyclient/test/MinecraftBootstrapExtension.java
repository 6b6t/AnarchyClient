package net.blockhost.anarchyclient.test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MinecraftBootstrapExtension implements BeforeAllCallback {

    private static final AtomicBoolean BOOTSTRAPPED = new AtomicBoolean();

    @Override
    public void beforeAll(final ExtensionContext context) {
        if (BOOTSTRAPPED.compareAndSet(false, true)) {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}
