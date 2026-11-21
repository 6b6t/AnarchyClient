package net.blockhost.anarchyclient.waypoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WaypointStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void persistsWaypointsByWorldAndName() {
        Path path = this.tempDir.resolve("waypoints.json");
        WaypointStore store = new WaypointStore(path);

        store.add(new Waypoint("server:example|overworld", "home", new BlockPos(1, 65, -3), 123));

        WaypointStore reloaded = new WaypointStore(path);
        reloaded.load();
        assertEquals(1, reloaded.byWorld("server:example|overworld").size());
        assertTrue(reloaded.find("server:example|overworld", "home").isPresent());
        assertTrue(reloaded.remove("server:example|overworld", "home"));
    }

    @Test
    void parsesLegacyWaypointSettingIntoCurrentWorld() {
        assertEquals(
                new Waypoint("world", "home", new BlockPos(0, 64, 0), WaypointStore.DEFAULT_COLOR),
                WaypointStore.parseLegacy("world", "home:0:64:0").getFirst());
    }

    @Test
    void keepsUnreadableFileAsBackupInsteadOfOverwritingIt() throws Exception {
        Path path = this.tempDir.resolve("waypoints.json");
        String broken = "{ \"waypoints\": [ {\"world\": \"w\", ";
        Files.writeString(path, broken);

        WaypointStore store = new WaypointStore(path);
        store.load();
        store.add(new Waypoint("w", "new", new BlockPos(0, 0, 0), WaypointStore.DEFAULT_COLOR));

        try (Stream<Path> files = Files.list(this.tempDir)) {
            Path backup = files.filter(file -> file.getFileName().toString().endsWith(".backup.json"))
                    .findFirst()
                    .orElseThrow();
            assertEquals(broken, Files.readString(backup));
        }
        WaypointStore reloaded = new WaypointStore(path);
        reloaded.load();
        assertTrue(reloaded.find("w", "new").isPresent());
    }
}
