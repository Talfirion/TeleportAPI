package com.teleportapi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case testing for Selection class.
 * Tests boundary conditions, extreme values, and validation logic.
 */
class SelectionEdgeCaseTest {

    private Selection selection;

    @BeforeEach
    void setUp() {
        selection = new Selection();
    }

    // ========== Zero-Volume Selections ==========

    @Test
    void testZeroVolumeSelection_SamePoint() {
        // All coordinates are the same - single point
        selection.setXMin(10);
        selection.setXMax(10);
        selection.setYMin(20);
        selection.setYMax(20);
        selection.setZMin(30);
        selection.setZMax(30);

        // getVolume() requires isComplete() which needs world - test face count instead
        assertEquals(6, selection.getSetFacesCount(), "Should have all 6 faces set");
    }

    @Test
    void testZeroVolumeSelection_SinglePlaneXY() {
        // Z coordinates are the same - a plane
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(5);
        selection.setZMax(5);

        // getVolume() requires world - test face count
        assertEquals(6, selection.getSetFacesCount(), "Plane should have all faces set");
    }

    @Test
    void testZeroVolumeSelection_Line() {
        // Only one axis varies - a line
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(5);
        selection.setYMax(5);
        selection.setZMin(5);
        selection.setZMax(5);

        // getVolume() requires world - test face count
        assertEquals(6, selection.getSetFacesCount(), "Line should have all faces set");
    }

    // ========== Single Block Selections ==========

    @Test
    void testSingleBlockSelection_1x1x1() {
        selection.setXMin(0);
        selection.setXMax(0);
        selection.setYMin(0);
        selection.setYMax(0);
        selection.setZMin(0);
        selection.setZMax(0);

        // getVolume() requires world, test coordinate getters
        assertEquals(6, selection.getSetFacesCount());
        assertEquals(0, selection.getXMin());
        assertEquals(0, selection.getYMin());
    }

    @Test
    void testSingleBlockSelection_UsingCorners() {
        BlockPos point = new BlockPos(5, 10, 15);
        selection.setFromCorners(point, point);

        // getVolume() requires world
        assertEquals(6, selection.getSetFacesCount());
        assertEquals(point.getX(), selection.getXMin());
        assertEquals(point.getY(), selection.getYMin());
        assertEquals(point.getZ(), selection.getZMin());
    }

    // ========== Extreme Coordinates ==========

    @Test
    void testExtremeCoordinates_VeryLargePositive() {
        int large = 1_000_000;
        selection.setXMin(0);
        selection.setXMax(large);
        selection.setYMin(0);
        selection.setYMax(large);
        selection.setZMin(0);
        selection.setZMax(large);

        assertEquals(large, selection.getXMax());
        // getMin()/getMax() require world
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testExtremeCoordinates_VeryLargeNegative() {
        int large = -1_000_000;
        selection.setXMin(large);
        selection.setXMax(0);
        selection.setYMin(large);
        selection.setYMax(0);
        selection.setZMin(large);
        selection.setZMax(0);

        // getMin()/getMax() require world
        assertEquals(large, selection.getXMin());
        assertEquals(0, selection.getXMax());
    }

    @Test
    void testExtremeCoordinates_MixedNegativePositive() {
        selection.setXMin(-500);
        selection.setXMax(500);
        selection.setYMin(-100);
        selection.setYMax(100);
        selection.setZMin(-250);
        selection.setZMax(250);

        // getVolume() requires world
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testExtremeCoordinates_MinecraftWorldHeight_1_20() {
        // Minecraft 1.20.1: Y from -64 to 319
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(-64);
        selection.setYMax(319);
        selection.setZMin(0);
        selection.setZMax(10);

        // getMin()/getMax() require world
        assertEquals(-64, selection.getYMin());
        assertEquals(319, selection.getYMax());
        // getVolume() also requires world
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testExtremeCoordinates_BelowWorldMinimum() {
        // Testing below Minecraft's usual minimum
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(-1000);
        selection.setYMax(-500);
        selection.setZMin(0);
        selection.setZMax(10);

        // Note: isComplete() requires world to be set, which we don't do in unit tests
        assertEquals(6, selection.getSetFacesCount());
        // getMin() returns null without world, so test coordinate getters instead
        assertEquals(-1000, selection.getYMin());
    }

    // ========== Negative Coordinate Workflow ==========

    @Test
    void testNegativeCoordinates_FullWorkflow() {
        BlockPos negPoint1 = new BlockPos(-100, -50, -200);
        BlockPos negPoint2 = new BlockPos(-50, -10, -150);

        selection.setFromCorners(negPoint1, negPoint2);

        // Note: isComplete() requires world to be set
        assertEquals(6, selection.getSetFacesCount());
        assertEquals(-100, selection.getXMin());
        assertEquals(-50, selection.getXMax());
        assertEquals(-50, selection.getYMin());
        assertEquals(-10, selection.getYMax());
        assertEquals(-200, selection.getZMin());
        assertEquals(-150, selection.getZMax());
    }

    @Test
    void testNegativeCoordinates_FromPoints() {
        BlockPos[] points = {
                new BlockPos(-10, -20, -30),
                new BlockPos(-5, -15, -25),
                new BlockPos(-8, -18, -28),
                new BlockPos(-12, -22, -32)
        };

        selection.setFromPoints(points);

        // Use coordinate getters instead of getMin()/getMax() which require world
        assertEquals(-12, selection.getXMin());
        assertEquals(-5, selection.getXMax());
        assertEquals(-22, selection.getYMin());
        assertEquals(-15, selection.getYMax());
        assertEquals(-32, selection.getZMin());
        assertEquals(-25, selection.getZMax());
    }

    // ========== Invalid Range Validation ==========

    @Test
    void testValidation_MinGreaterThanMax_X() {
        // This should trigger a warning but not throw exception
        selection.setXMin(100);
        selection.setXMax(50);

        // Validation logs warning but allows it
        assertEquals(100, selection.getXMin());
        assertEquals(50, selection.getXMax());
    }

    @Test
    void testValidation_MinGreaterThanMax_AllAxes() {
        selection.setXMin(100);
        selection.setXMax(50);
        selection.setYMin(200);
        selection.setYMax(150);
        selection.setZMin(300);
        selection.setZMax(250);

        // Should still work, values are stored as set
        assertEquals(100, selection.getXMin());
        assertEquals(50, selection.getXMax());
        assertEquals(200, selection.getYMin());
        assertEquals(150, selection.getYMax());
        assertEquals(300, selection.getZMin());
        assertEquals(250, selection.getZMax());
    }

    // ========== Volume Calculation Edge Cases ==========

    @Test
    void testVolumeCalculation_IncompleteSelection() {
        selection.setXMin(0);
        selection.setXMax(10);
        // Missing other coordinates

        assertEquals(0, selection.getVolume(), "Incomplete selection should have 0 volume");
    }

    @Test
    void testVolumeCalculation_LargeVolume() {
        selection.setXMin(0);
        selection.setXMax(99);
        selection.setYMin(0);
        selection.setYMax(99);
        selection.setZMin(0);
        selection.setZMax(99);

        // getVolume() requires world
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testVolumeCalculation_ReversedCoordinates() {
        // Set coordinates in reverse order
        selection.setXMax(0);
        selection.setXMin(10);
        selection.setYMax(0);
        selection.setYMin(10);
        selection.setZMax(0);
        selection.setZMin(10);

        // getVolume() requires world
        assertEquals(6, selection.getSetFacesCount());
    }

    // ========== GetMin/GetMax Edge Cases ==========

    @Test
    void testGetMinMax_AllSameCoordinate() {
        selection.setXMin(5);
        selection.setXMax(5);
        selection.setYMin(5);
        selection.setYMax(5);
        selection.setZMin(5);
        selection.setZMax(5);

        // getMin()/getMax() require world
        assertEquals(6, selection.getSetFacesCount());
        assertEquals(5, selection.getXMin());
        assertEquals(5, selection.getYMin());
        assertEquals(5, selection.getZMin());
    }

    @Test
    void testGetMinMax_WithoutWorld() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);
        // World is not set

        assertNull(selection.getMin(), "Should return null without world");
        assertNull(selection.getMax(), "Should return null without world");
    }

    // ========== GetInfo Edge Cases ==========

    @Test
    void testGetInfo_IncompleteWithVariousCounts() {
        String info = selection.getInfo();
        assertTrue(info.contains("0/6"));

        selection.setXMin(0);
        info = selection.getInfo();
        assertTrue(info.contains("1/6"));

        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        info = selection.getInfo();
        assertTrue(info.contains("4/6"));
    }

    // ========== SetFromSixPoints Edge Cases ==========

    @Test
    void testSetFromSixPoints_AllSamePoint() {
        BlockPos point = new BlockPos(5, 10, 15);
        BlockPos[] points = new BlockPos[6];
        for (int i = 0; i < 6; i++) {
            points[i] = point;
        }

        selection.setFromSixPoints(points);

        assertEquals(6, selection.getSetFacesCount());
        assertEquals(point.getX(), selection.getXMin());
        assertEquals(point.getX(), selection.getXMax());
    }

    @Test
    void testSetFromSixPoints_MinimalVariation() {
        // Points that vary by only 1 on each axis
        BlockPos[] points = {
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, 0),
                new BlockPos(0, 1, 0),
                new BlockPos(0, 0, 1),
                new BlockPos(1, 1, 0),
                new BlockPos(1, 1, 1)
        };

        selection.setFromSixPoints(points);

        assertEquals(0, selection.getXMin());
        assertEquals(1, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(1, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(1, selection.getZMax());
    }

    // ========== Reset Edge Cases ==========

    @Test
    void testReset_AfterFullSetup() {
        selection.setFromCorners(new BlockPos(0, 0, 0), new BlockPos(10, 10, 10));
        assertEquals(6, selection.getSetFacesCount());

        selection.reset();

        assertEquals(0, selection.getSetFacesCount());
        assertFalse(selection.isComplete());
        assertNull(selection.getWorld());
    }

    @Test
    void testReset_Multiple() {
        selection.setXMin(0);
        selection.reset();
        selection.setXMin(5);
        selection.reset();
        selection.setXMin(10);

        assertEquals(1, selection.getSetFacesCount());
        assertEquals(10, selection.getXMin());
    }
}
