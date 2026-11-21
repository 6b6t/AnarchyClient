package net.blockhost.anarchyclient.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import net.blockhost.anarchyclient.config.ClientConfig;
import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.lenni0451.rivet.Rivet;
import net.lenni0451.rivet.layout.absolute.AbsoluteLayout;
import net.lenni0451.rivet.math.Size;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

@ExtendWith(MinecraftBootstrapExtension.class)
class ModulePanelTest {

    private static final Size SIZE = new Size(1280, 720);

    @TempDir
    Path tempDir;

    @Test
    void buildsEverySectionAndDrawer() {
        ModuleManager modules = new ModuleManager();
        ModuleRegistry.registerDefaults(modules);
        ModulePanel panel = new ModulePanel(modules, new ClientConfig(modules, this.tempDir.resolve("config.json")));
        new Rivet(new TestRivetBackend(), AbsoluteLayout.INSTANCE, SIZE).root().addChild(panel);

        Module module = modules.all().iterator().next();
        panel.inspectModule(module);
        panel.computeLayout(SIZE);
        assertTrue(panel.layoutDebugLabel().contains("module=" + module.id()));

        // MODULES is selected initially, and selecting the open tab again closes it, so visit it last.
        for (Tab tab : List.of(Tab.FRIENDS, Tab.PROFILES, Tab.THEME, Tab.MODULES)) {
            panel.selectTab(tab);
            panel.computeLayout(SIZE);
            assertTrue(
                    panel.layoutDebugLabel().startsWith("tab=" + tab.name().toLowerCase(Locale.ROOT) + " "),
                    tab.name());
        }
        for (Drawer drawer : List.of(Drawer.ROOT, Drawer.GENERAL, Drawer.MODULES, Drawer.SOUND, Drawer.NOTIFICATIONS)) {
            panel.openDrawer(drawer);
            panel.computeLayout(SIZE);
            assertTrue(
                    panel.layoutDebugLabel().endsWith("drawer=" + drawer.name().toLowerCase(Locale.ROOT)),
                    drawer.name());
        }
    }

    @Test
    void switchingTabsCancelsKeybindCapture() {
        ModuleManager modules = new ModuleManager();
        ModuleRegistry.registerDefaults(modules);
        ModulePanel panel = new ModulePanel(modules, new ClientConfig(modules, this.tempDir.resolve("config.json")));
        new Rivet(new TestRivetBackend(), AbsoluteLayout.INSTANCE, SIZE).root().addChild(panel);
        Module module = modules.all().iterator().next();
        panel.inspectModule(module);

        panel.startKeybindListening(module);
        assertTrue(panel.isCapturingKeybind());
        panel.selectTab(Tab.FRIENDS);

        assertFalse(panel.isCapturingKeybind());
    }
}
