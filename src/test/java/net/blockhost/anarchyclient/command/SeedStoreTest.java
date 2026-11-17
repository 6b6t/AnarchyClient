package net.blockhost.anarchyclient.command;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesSeedsByWorldKey() throws Exception {
        Path path = this.tempDir.resolve("seeds.json");
        String world = SeedStore.worldKey("example.org:25565");

        assertEquals("server:example.org:25565", world);
        assertEquals("12345", SeedStore.normalizeSeed(" 12345 "));
        assertThrows(IllegalArgumentException.class, () -> SeedStore.normalizeSeed("   "));
        assertNull(SeedStore.get(path, world));

        SeedStore.SeedRecord saved = SeedStore.put(path, world, " 12345 ",
                Instant.parse("2026-06-25T00:00:00Z"), "26.2", "minecraft:overworld", "manual");
        assertEquals(world, saved.world());
        assertEquals("12345", saved.seed());
        assertEquals("2026-06-25T00:00:00Z", saved.savedAt());
        assertEquals("26.2", saved.version());
        assertEquals("minecraft:overworld", saved.dimension());
        assertEquals("manual", saved.source());
        assertEquals(1, SeedStore.list(path).size());
        assertEquals("12345", SeedStore.get(path, world).seed());
        assertTrue(SeedStore.delete(path, world));
        assertFalse(SeedStore.delete(path, world));
    }

    @Test
    void skipsMalformedEntries() throws Exception {
        Path path = this.tempDir.resolve("seeds.json");
        Files.writeString(path, """
                {
                  "server:broken": "not an object",
                  "server:object_seed": {"seed": {"value": 1}},
                  "server:good": {"seed": "42", "version": ["bad"]}
                }
                """);

        assertNull(SeedStore.get(path, "server:broken"));
        assertNull(SeedStore.get(path, "server:object_seed"));
        SeedStore.SeedRecord good = SeedStore.get(path, "server:good");
        assertEquals("42", good.seed());
        assertEquals("", good.version());
        assertEquals(1, SeedStore.list(path).size());
    }
}
