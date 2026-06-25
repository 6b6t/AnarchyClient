package net.blockhost.anarchyclient.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
