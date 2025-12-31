package com.teleportapi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HeightLimitTest {

    @Test
    void testIsOutsideHeightLimits() {
        // Limits: [0, 256)
        int min = 0;
        int max = 256;

        // Inside
        assertFalse(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 0, 0), min, max));
        assertFalse(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 100, 0), min, max));
        assertFalse(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 255, 0), min, max));

        // Outside (Max)
        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 256, 0), min, max));
        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 300, 0), min, max));

        // Outside (Min)
        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, -1, 0), min, max));
        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, -100, 0), min, max));
    }

    @Test
    void testCustomLimits() {
        // Limits: [-64, 320) (Nether/Overworld 1.18+)
        int min = -64;
        int max = 320;

        assertFalse(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, -64, 0), min, max));
        assertFalse(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 319, 0), min, max));

        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, -65, 0), min, max));
        assertTrue(StructureTeleporter.isOutsideHeightLimits(new BlockPos(0, 320, 0), min, max));
    }
}
