package net.blockhost.anarchyclient.module.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEspModuleTest {

    @Test
    void mergesFilledStorageRectangles() {
        List<AABB> boxes = StorageEspModule.mergePositions(List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(1, 64, 0),
                new BlockPos(0, 64, 1),
                new BlockPos(1, 64, 1)
        ));

        assertEquals(List.of(new AABB(0, 64, 0, 2, 65, 2)), boxes);
    }

    @Test
    void doesNotIncludeMissingPositionsInsideMergedBoxes() {
        List<AABB> boxes = StorageEspModule.mergePositions(List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(1, 64, 0),
                new BlockPos(1, 64, 1)
        ));

        assertEquals(2, boxes.size());
        assertTrue(boxes.contains(new AABB(0, 64, 0, 2, 65, 1)));
        assertTrue(boxes.contains(new AABB(1, 64, 1, 2, 65, 2)));
    }
}
