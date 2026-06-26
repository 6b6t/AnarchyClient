package net.blockhost.anarchyclient.ui;

import net.blockhost.anarchyclient.module.Module;
import net.blockhost.anarchyclient.module.ModuleManager;
import net.blockhost.anarchyclient.module.ModuleRegistry;
import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class LucideIconsTest {

    @Test
    void parsesHexCodepointsIntoGlyphs() throws Exception {
        InputStream input = new ByteArrayInputStream("""
                search=f2c5
                chevron-down=f16d
                """.getBytes(StandardCharsets.UTF_8));

        Map<String, String> glyphs = LucideIcons.parse(input);

        assertEquals("\uf2c5", glyphs.get("search"));
        assertEquals("\uf16d", glyphs.get("chevron-down"));
    }

    @Test
    void generatedResourceContainsUsedIcons() throws Exception {
        try (InputStream input = LucideIcons.class.getResourceAsStream("/assets/anarchyclient/font/lucide-icons.properties")) {
            assertNotNull(input);

            Map<String, String> glyphs = LucideIcons.parse(input);

            assertEquals("\uf16d", glyphs.get("chevron-down"));
            assertEquals("\uf171", glyphs.get("chevron-right"));
            assertEquals("\uf2c5", glyphs.get("search"));
            assertEquals("\uf2cc", glyphs.get("settings"));
            assertEquals("\uf26e", glyphs.get("more-vertical"));
        }
    }

    @Test
    void generatedResourceContainsModuleIcons() throws Exception {
        ModuleManager modules = new ModuleManager();
        ModuleRegistry.registerDefaults(modules);

        try (InputStream input = LucideIcons.class.getResourceAsStream("/assets/anarchyclient/font/lucide-icons.properties")) {
            assertNotNull(input);

            Map<String, String> glyphs = LucideIcons.parse(input);

            for (Module module : modules.all()) {
                assertTrue(glyphs.containsKey(module.icon()), module.id() + " uses missing icon " + module.icon());
            }
        }
    }
}
