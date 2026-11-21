package net.blockhost.anarchyclient.module.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joml.Vector3fc;
import org.junit.jupiter.api.Test;

class RenderEffectsModulesTest {

    @Test
    void heavenRiseProgressClampsAndEasesOut() {
        assertEquals(0.0F, HeavenModule.progress(0L, 8.0));
        assertEquals(0.5F, HeavenModule.progress(4_000L, 8.0));
        assertEquals(1.0F, HeavenModule.progress(20_000L, 8.0));
        assertEquals(0.75F, HeavenModule.easeOut(0.5F));
        assertEquals(1.0F, HeavenModule.easeOut(1.0F));
    }

    @Test
    void hitmarkerFadesOutOverItsDuration() {
        assertEquals(255, HitmarkerModule.alpha(0L, 300L));
        assertEquals(128, HitmarkerModule.alpha(150L, 300L));
        assertEquals(0, HitmarkerModule.alpha(300L, 300L));
        assertEquals(0, HitmarkerModule.alpha(10L, 0L));
    }

    @Test
    void bordersFindTheChunkAndMapCellContainingAPosition() {
        assertEquals(0, BordersModule.cellStart(15.9, BordersModule.CHUNK_SIZE, 0));
        assertEquals(-16, BordersModule.cellStart(-0.1, BordersModule.CHUNK_SIZE, 0));
        // Map areas span -64..63, 64..191, and so on.
        assertEquals(-64, BordersModule.cellStart(63.5, BordersModule.MAP_SIZE, BordersModule.MAP_OFFSET));
        assertEquals(64, BordersModule.cellStart(64.0, BordersModule.MAP_SIZE, BordersModule.MAP_OFFSET));
        assertEquals(-192, BordersModule.cellStart(-65.0, BordersModule.MAP_SIZE, BordersModule.MAP_OFFSET));
    }

    @Test
    void skyboxRainbowStaysInRangeAndChangesOverTime() {
        Vector3fc start = SkyboxModule.rainbow(0L, 20.0);
        Vector3fc later = SkyboxModule.rainbow(7_000L, 20.0);

        for (Vector3fc color : new Vector3fc[] {start, later}) {
            assertTrue(color.x() >= 0.0F && color.x() <= 1.0F);
            assertTrue(color.y() >= 0.0F && color.y() <= 1.0F);
            assertTrue(color.z() >= 0.0F && color.z() <= 1.0F);
        }
        assertNotEquals(start, later);
    }

    @Test
    void skinFlickerCanLeaveTheCapeAlone() {
        assertTrue(SkinFlickerModule.parts(true).size()
                > SkinFlickerModule.parts(false).size());
    }
}
