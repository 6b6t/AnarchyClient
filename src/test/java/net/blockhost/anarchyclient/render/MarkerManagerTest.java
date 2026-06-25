package net.blockhost.anarchyclient.render;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerManagerTest {

    @BeforeEach
    @AfterEach
    void clearMarkers() {
        MarkerManager.clear();
    }

    @Test
    void rejectsBlankMarkersAndReturnsSnapshot() {
        MarkerManager.put(null);
        MarkerManager.put(new TestMarker("", 0, 10));
        MarkerManager.put(new TestMarker("one", 0, 10));

        assertEquals(1, MarkerManager.markers().size());
        assertEquals("one", MarkerManager.markers().iterator().next().id());
    }

    @Test
    void ticksMarkersAndExpiresByLifetime() {
        MarkerManager.put(new TestMarker("timed", 0, 2));

        MarkerManager.tick();
        Marker marker = MarkerManager.markers().iterator().next();

        assertEquals(1, marker.ageTicks());
        assertTrue(MarkerManager.remove("timed"));
        assertEquals(0, MarkerManager.markers().size());

        MarkerManager.put(new TestMarker("timed", 0, 2));
        MarkerManager.tick();
        MarkerManager.tick();

        assertEquals(0, MarkerManager.markers().size());
    }

    private record TestMarker(String id, int ageTicks, int lifetimeTicks) implements Marker {

        @Override
        public Marker ticked() {
            return new TestMarker(this.id, this.ageTicks + 1, this.lifetimeTicks);
        }

        @Override
        public void render(final LevelRenderContext context, final Vec3 camera) {
        }
    }
}
