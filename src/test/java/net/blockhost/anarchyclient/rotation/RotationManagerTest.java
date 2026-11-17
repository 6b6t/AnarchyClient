package net.blockhost.anarchyclient.rotation;

import net.blockhost.anarchyclient.test.MinecraftBootstrapExtension;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MinecraftBootstrapExtension.class)
class RotationManagerTest {

    private static final Vec3 EYE = new Vec3(0.5, 1.62, -2.5);
    private static final AABB BLOCK = new AABB(0.0, 1.0, 0.0, 1.0, 2.0, 1.0);

    @Test
    void lookRayHitsBlockInFrontWithinRange() {
        assertTrue(RotationManager.lookHits(EYE, new Rotation(0.0F, 0.0F), BLOCK, 4.5));
    }

    @Test
    void lookRayMissesWhenFacingAwayOrOutOfRange() {
        assertFalse(RotationManager.lookHits(EYE, new Rotation(180.0F, 0.0F), BLOCK, 4.5));
        assertFalse(RotationManager.lookHits(EYE, new Rotation(0.0F, 0.0F), BLOCK, 2.0));
    }

    @Test
    void reportsActiveOwner() {
        RotationManager.clearAll();
        RotationManager.request(new RotationRequest("scaffold", new Rotation(0.0F, 80.0F), 10, 90.0F, 5));
        RotationManager.request(new RotationRequest("kill_aura", new Rotation(90.0F, 0.0F), 50, 90.0F, 5));

        assertTrue(RotationManager.isActiveOwner("kill_aura"));
        assertFalse(RotationManager.isActiveOwner("scaffold"));
        RotationManager.clearAll();
    }
}
