package net.blockhost.anarchyclient.command;

import net.blockhost.anarchyclient.setting.BooleanSetting;
import net.blockhost.anarchyclient.setting.NumberSetting;
import net.blockhost.anarchyclient.setting.SelectSetting;
import net.blockhost.anarchyclient.setting.StringSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnarchyClientCommandsTest {

    @TempDir
    Path tempDir;

    @Test
    void parsesBooleanSettingValues() {
        BooleanSetting setting = BooleanSetting.from(BooleanSetting.builder()
                .id("enabled")
                .name("Enabled")
                .defaultValue(false)
                .build());

        AnarchyClientCommands.setSettingValue(setting, "true");

        assertEquals(true, setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "yes"));
    }

    @Test
    void parsesNumberSettingValuesThroughSettingSanitizer() {
        NumberSetting setting = NumberSetting.from(NumberSetting.builder()
                .id("range")
                .name("Range")
                .defaultValue(1.0)
                .min(0.0)
                .max(6.0)
                .step(0.5)
                .build());

        AnarchyClientCommands.setSettingValue(setting, "9");

        assertEquals(6.0, setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "far"));
    }

    @Test
    void validatesSelectSettingValues() {
        SelectSetting setting = SelectSetting.from(SelectSetting.builder()
                .id("mode")
                .name("Mode")
                .defaultValue("First")
                .addAllOptions(List.of("First", "Second"))
                .build());

        AnarchyClientCommands.setSettingValue(setting, "Second");

        assertEquals("Second", setting.value());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.setSettingValue(setting, "Third"));
    }

    @Test
    void acceptsStringSettingValues() {
        StringSetting setting = StringSetting.from(StringSetting.builder()
                .id("message")
                .name("Message")
                .defaultValue("one")
                .build());

        AnarchyClientCommands.setSettingValue(setting, "two words");

        assertEquals("two words", setting.value());
    }

    @Test
    void centerModesSnapCoordinatesPredictably() {
        assertEquals(12.5, AnarchyClientCommands.CenterMode.MIDDLE.coordinate(12.2));
        assertEquals(-4.5, AnarchyClientCommands.CenterMode.MIDDLE.coordinate(-4.8));
        assertEquals(12.0, AnarchyClientCommands.CenterMode.CORNER.coordinate(12.2));
        assertEquals(-5.0, AnarchyClientCommands.CenterMode.CORNER.coordinate(-4.8));
    }

    @Test
    void parsesCommandCoordinatesAndRelativeBlockPositions() {
        assertEquals(12.0, AnarchyClientCommands.parseCoordinate("12", 5.0));
        assertEquals(5.0, AnarchyClientCommands.parseCoordinate("~", 5.0));
        assertEquals(7.5, AnarchyClientCommands.parseCoordinate("~2.5", 5.0));
        assertEquals(new BlockPos(12, 64, -5),
                AnarchyClientCommands.parseBlockPosition("~2", "64", "~-1", new Vec3(10.2, 70.0, -3.4)));
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.parseCoordinate("~bad", 0.0));
    }

    @Test
    void fillAreasAreNormalizedAndBounded() {
        AnarchyClientCommands.BlockArea area = AnarchyClientCommands.blockArea(
                new BlockPos(3, 5, 7),
                new BlockPos(1, 4, 6),
                12
        );

        assertEquals(1, area.minX());
        assertEquals(4, area.minY());
        assertEquals(6, area.minZ());
        assertEquals(3, area.maxX());
        assertEquals(5, area.maxY());
        assertEquals(7, area.maxZ());
        assertEquals(12, area.count());
        assertThrows(IllegalArgumentException.class, () -> AnarchyClientCommands.blockArea(
                new BlockPos(0, 0, 0),
                new BlockPos(3, 3, 3),
                8
        ));
    }

    @Test
    void extractsSkinUrlFromMojangProfilePayload() {
        String texturePayload = "{\"textures\":{\"SKIN\":{\"url\":\"https://textures.minecraft.net/texture/example\"}}}";
        String encoded = Base64.getEncoder().encodeToString(texturePayload.getBytes(StandardCharsets.UTF_8));
        String profile = "{\"properties\":[{\"name\":\"textures\",\"value\":\"" + encoded + "\"}]}";

        assertEquals("https://textures.minecraft.net/texture/example", AnarchyClientCommands.skinUrl(profile));
    }

    @Test
    void sanitizesSkinFileNames() {
        assertEquals("Bad_Name__", AnarchyClientCommands.sanitizeFileName("Bad/Name:*"));
        assertEquals("skin", AnarchyClientCommands.sanitizeFileName("   "));
    }

    @Test
    void validatesPortScanRanges() {
        ServerPortScanner.PortRange range = ServerPortScanner.range(25570, 25565);

        assertEquals(25565, range.min());
        assertEquals(25570, range.max());
        assertEquals(6, range.checks().size());
        assertThrows(IllegalArgumentException.class, () -> ServerPortScanner.range(1, ServerPortScanner.MAX_RANGE_SIZE + 2));
    }

    @Test
    void formatsOpenPortScanResults() {
        assertEquals("No open ports found.", ServerPortScanner.format(List.of(
                new ServerPortScanner.PortResult(25565, "Minecraft", false)
        )));
        assertEquals("Open ports: 25565 (Minecraft), 8123 (Dynmap)", ServerPortScanner.format(List.of(
                new ServerPortScanner.PortResult(25565, "Minecraft", true),
                new ServerPortScanner.PortResult(22, "SSH", false),
                new ServerPortScanner.PortResult(8123, "Dynmap", true)
        )));
    }

    @Test
    void storesSeedsByWorldKey() throws Exception {
        Path path = this.tempDir.resolve("seeds.json");
        String world = SeedStore.worldKey("example.org:25565");

        assertEquals("server:example.org:25565", world);
        assertEquals("12345", SeedStore.normalizeSeed(" 12345 "));
        assertThrows(IllegalArgumentException.class, () -> SeedStore.normalizeSeed("   "));
        assertNull(SeedStore.get(path, world));

        SeedStore.SeedRecord saved = SeedStore.put(path, world, " 12345 ", Instant.parse("2026-06-25T00:00:00Z"));
        assertEquals(world, saved.world());
        assertEquals("12345", saved.seed());
        assertEquals("2026-06-25T00:00:00Z", saved.savedAt());
        assertEquals(1, SeedStore.list(path).size());
        assertEquals("12345", SeedStore.get(path, world).seed());
        assertTrue(SeedStore.delete(path, world));
        assertFalse(SeedStore.delete(path, world));
    }
}
