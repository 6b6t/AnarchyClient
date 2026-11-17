package net.blockhost.anarchyclient.command;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerCommandsTest {

    @Test
    void centerModesSnapCoordinatesPredictably() {
        assertEquals(12.5, PlayerCommands.CenterMode.MIDDLE.coordinate(12.2));
        assertEquals(-4.5, PlayerCommands.CenterMode.MIDDLE.coordinate(-4.8));
        assertEquals(12.0, PlayerCommands.CenterMode.CORNER.coordinate(12.2));
        assertEquals(-5.0, PlayerCommands.CenterMode.CORNER.coordinate(-4.8));
    }

    @Test
    void extractsSkinUrlFromMojangProfilePayload() {
        String texturePayload = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/example\"}}}";
        String encoded = Base64.getEncoder().encodeToString(texturePayload.getBytes(StandardCharsets.UTF_8));
        String profile = "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + encoded + "\"}]}";

        assertEquals("https://textures.minecraft.net/texture/example", PlayerCommands.skinUrl(profile));
    }
}
