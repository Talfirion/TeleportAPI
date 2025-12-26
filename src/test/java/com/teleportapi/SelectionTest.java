package com.teleportapi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standard tests for the Selection class.
 * Tests basic functionality, setters, getters, and common workflows.
 */
class SelectionTest {

    private Selection selection;

    @BeforeEach
    void setUp() {
        selection = new Selection();
    }

    // ========== Individual Face Setters/Getters ==========

    @Test
    void testSetAndGetXMin() {
        selection.setXMin(10);
        assertEquals(10, selection.getXMin());
    }

    @Test
    void testSetAndGetXMax() {
        selection.setXMax(20);
        assertEquals(20, selection.getXMax());
    }

    @Test
    void testSetAndGetYMin() {
        selection.setYMin(5);
        assertEquals(5, selection.getYMin());
    }

    @Test
    void testSetAndGetYMax() {
        selection.setYMax(15);
        assertEquals(15, selection.getYMax());
    }

    @Test
    void testSetAndGetZMin() {
        selection.setZMin(30);
        assertEquals(30, selection.getZMin());
    }

    @Test
    void testSetAndGetZMax() {
        selection.setZMax(40);
        assertEquals(40, selection.getZMax());
    }

    // ========== FaceType Operations ==========

    @Test
    void testSetFacePoint() {
        BlockPos point = new BlockPos(100, 200, 300);
        selection.setFacePoint(FaceType.X_MIN, point);

        BlockPos retrieved = selection.getFacePoint(FaceType.X_MIN);
        assertNotNull(retrieved);
        assertEquals(100, retrieved.getX());
    }

    @Test
    void testSetAllFacePoints() {
        selection.setFacePoint(FaceType.X_MIN, new BlockPos(0, 0, 0));
        selection.setFacePoint(FaceType.X_MAX, new BlockPos(10, 0, 0));
        selection.setFacePoint(FaceType.Y_MIN, new BlockPos(0, 5, 0));
        selection.setFacePoint(FaceType.Y_MAX, new BlockPos(0, 15, 0));
        selection.setFacePoint(FaceType.Z_MIN, new BlockPos(0, 0, 20));
        selection.setFacePoint(FaceType.Z_MAX, new BlockPos(0, 0, 30));

        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testGetFacePoint_NotSet() {
        BlockPos point = selection.getFacePoint(FaceType.X_MIN);
        assertNull(point);
    }

    @Test
    void testRemoveFacePoint() {
        selection.setXMin(10);
        assertEquals(1, selection.getSetFacesCount());

        selection.removeFacePoint(FaceType.X_MIN);
        assertEquals(0, selection.getSetFacesCount());
    }

    // ========== isComplete() Tests ==========

    @Test
    void testIsComplete_EmptySelection() {
        assertFalse(selection.isComplete());
    }

    @Test
    void testIsComplete_PartialSelection() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        assertFalse(selection.isComplete());
    }

    @Test
    void testIsComplete_AllFacesSet() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // isComplete() also needs world, but without world it returns false
        assertFalse(selection.isComplete()); // World not set
    }

    // ========== getSetFacesCount() Tests ==========

    @Test
    void testGetSetFacesCount_Zero() {
        assertEquals(0, selection.getSetFacesCount());
    }

    @Test
    void testGetSetFacesCount_Incremental() {
        assertEquals(0, selection.getSetFacesCount());

        selection.setXMin(0);
        assertEquals(1, selection.getSetFacesCount());

        selection.setXMax(10);
        assertEquals(2, selection.getSetFacesCount());

        selection.setYMin(0);
        assertEquals(3, selection.getSetFacesCount());
    }

    @Test
    void testGetSetFacesCount_AllSet() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        assertEquals(6, selection.getSetFacesCount());
    }

    // ========== setFromCorners() Tests ==========

    @Test
    void testSetFromCorners_BasicCuboid() {
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 20, 30);

        selection.setFromCorners(p1, p2);

        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(20, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(30, selection.getZMax());
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testSetFromCorners_ReversedOrder() {
        BlockPos p1 = new BlockPos(10, 20, 30);
        BlockPos p2 = new BlockPos(0, 0, 0);

        selection.setFromCorners(p1, p2);

        // Should auto-sort min/max
        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(20, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(30, selection.getZMax());
    }

    @Test
    void testSetFromCorners_SamePoint() {
        BlockPos point = new BlockPos(5, 10, 15);

        selection.setFromCorners(point, point);

        assertEquals(5, selection.getXMin());
        assertEquals(5, selection.getXMax());
        assertEquals(10, selection.getYMin());
        assertEquals(10, selection.getYMax());
        assertEquals(15, selection.getZMin());
        assertEquals(15, selection.getZMax());
    }

    @Test
    void testSetFromCorners_NegativeCoordinates() {
        BlockPos p1 = new BlockPos(-10, -20, -30);
        BlockPos p2 = new BlockPos(-5, -10, -15);

        selection.setFromCorners(p1, p2);

        assertEquals(-10, selection.getXMin());
        assertEquals(-5, selection.getXMax());
        assertEquals(-20, selection.getYMin());
        assertEquals(-10, selection.getYMax());
        assertEquals(-30, selection.getZMin());
        assertEquals(-15, selection.getZMax());
    }

    // ========== setFromPoints() Tests ==========

    @Test
    void testSetFromPoints_TwoPoints() {
        BlockPos[] points = {
                new BlockPos(0, 0, 0),
                new BlockPos(10, 10, 10)
        };

        selection.setFromPoints(points);

        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(10, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(10, selection.getZMax());
    }

    @Test
    void testSetFromPoints_MultiplePoints() {
        BlockPos[] points = {
                new BlockPos(5, 10, 15),
                new BlockPos(0, 5, 10),
                new BlockPos(15, 20, 25),
                new BlockPos(3, 7, 12)
        };

        selection.setFromPoints(points);

        assertEquals(0, selection.getXMin());
        assertEquals(15, selection.getXMax());
        assertEquals(5, selection.getYMin());
        assertEquals(20, selection.getYMax());
        assertEquals(10, selection.getZMin());
        assertEquals(25, selection.getZMax());
    }

    @Test
    void testSetFromPoints_SinglePoint() {
        BlockPos[] points = { new BlockPos(7, 14, 21) };

        selection.setFromPoints(points);

        assertEquals(7, selection.getXMin());
        assertEquals(7, selection.getXMax());
        assertEquals(14, selection.getYMin());
        assertEquals(14, selection.getYMax());
        assertEquals(21, selection.getZMin());
        assertEquals(21, selection.getZMax());
    }

    @Test
    void testSetFromPoints_EmptyArray() {
        BlockPos[] points = {};

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromPoints(points);
        });
    }

    @Test
    void testSetFromPoints_NullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromPoints((BlockPos[]) null);
        });
    }

    // ========== setFromSixPoints() Tests ==========

    @Test
    void testSetFromSixPoints_Valid() {
        BlockPos[] points = {
                new BlockPos(0, 5, 10),
                new BlockPos(10, 15, 20),
                new BlockPos(5, 10, 15),
                new BlockPos(3, 7, 12),
                new BlockPos(8, 12, 18),
                new BlockPos(2, 6, 11)
        };

        selection.setFromSixPoints(points);

        assertEquals(6, selection.getSetFacesCount());
        // Should calculate min/max from all points
        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
    }

    @Test
    void testSetFromSixPoints_TooFewPoints() {
        BlockPos[] points = {
                new BlockPos(0, 0, 0),
                new BlockPos(10, 10, 10)
        };

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromSixPoints(points);
        });
    }

    @Test
    void testSetFromSixPoints_TooManyPoints() {
        BlockPos[] points = new BlockPos[10];
        for (int i = 0; i < 10; i++) {
            points[i] = new BlockPos(i, i, i);
        }

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromSixPoints(points);
        });
    }

    @Test
    void testSetFromSixPoints_ExactlySix() {
        BlockPos[] points = new BlockPos[6];
        for (int i = 0; i < 6; i++) {
            points[i] = new BlockPos(i, i * 2, i * 3);
        }

        selection.setFromSixPoints(points);
        assertEquals(6, selection.getSetFacesCount());
    }

    // ========== reset() Tests ==========

    @Test
    void testReset_EmptySelection() {
        selection.reset();
        assertEquals(0, selection.getSetFacesCount());
    }

    @Test
    void testReset_PartiallyFilledSelection() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(5);

        selection.reset();

        assertEquals(0, selection.getSetFacesCount());
        assertFalse(selection.isComplete());
    }

    @Test
    void testReset_FullSelection() {
        selection.setFromCorners(new BlockPos(0, 0, 0), new BlockPos(10, 10, 10));
        assertEquals(6, selection.getSetFacesCount());

        selection.reset();

        assertEquals(0, selection.getSetFacesCount());
        assertNull(selection.getWorld());
    }

    // ========== getInfo() Tests ==========

    @Test
    void testGetInfo_EmptySelection() {
        String info = selection.getInfo();
        assertNotNull(info);
        assertTrue(info.contains("0/6"));
    }

    @Test
    void testGetInfo_PartialSelection() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(5);

        String info = selection.getInfo();
        assertTrue(info.contains("3/6"));
    }

    @Test
    void testGetInfo_CompleteSelection() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        String info = selection.getInfo();
        assertTrue(info.contains("6/6"));
    }

    // ========== validateCoordinates() Tests (REMOVED) ==========
    // Note: validateCoordinates() is not a public method in Selection class
    // These tests have been removed as the method doesn't exist

    // ========== getMin() and getMax() Tests ==========

    @Test
    void testGetMin_WithoutWorld() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // getMin() requires world to be set
        assertNull(selection.getMin());
    }

    @Test
    void testGetMax_WithoutWorld() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // getMax() requires world to be set
        assertNull(selection.getMax());
    }

    // ========== getVolume() Tests ==========

    @Test
    void testGetVolume_IncompleteSelection() {
        selection.setXMin(0);
        selection.setXMax(10);

        assertEquals(0, selection.getVolume());
    }

    @Test
    void testGetVolume_CompleteWithoutWorld() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // getVolume() requires world to be set
        assertEquals(0, selection.getVolume());
    }

    // ========== Multiple set Operations ==========

    @Test
    void testMultipleSetOperations() {
        selection.setXMin(0);
        assertEquals(1, selection.getSetFacesCount());

        selection.setXMin(5); // Update same face
        assertEquals(1, selection.getSetFacesCount());
        assertEquals(5, selection.getXMin());
    }

    @Test
    void testOverwriteWithSetFromCorners() {
        selection.setXMin(100);
        selection.setYMin(200);

        selection.setFromCorners(new BlockPos(0, 0, 0), new BlockPos(10, 10, 10));

        assertEquals(0, selection.getXMin());
        assertEquals(0, selection.getYMin());
        assertEquals(6, selection.getSetFacesCount());
    }

    // ========== World Management ==========

    @Test
    void testSetAndGetWorld() {
        assertNull(selection.getWorld());
        // Can't create Level in unit tests, just test null state
    }

    @Test
    void testResetClearsWorld() {
        // World is initially null
        selection.setXMin(0);
        selection.reset();

        assertNull(selection.getWorld());
    }

    // ========== Extreme Values ==========

    @Test
    void testVeryLargeCoordinates() {
        int large = 1_000_000;
        selection.setXMin(0);
        selection.setXMax(large);
        selection.setYMin(0);
        selection.setYMax(large);
        selection.setZMin(0);
        selection.setZMax(large);

        assertEquals(large, selection.getXMax());
        assertEquals(large, selection.getYMax());
        assertEquals(large, selection.getZMax());
    }

    @Test
    void testNegativeCoordinates() {
        selection.setXMin(-100);
        selection.setXMax(-50);
        selection.setYMin(-200);
        selection.setYMax(-150);
        selection.setZMin(-300);
        selection.setZMax(-250);

        assertEquals(-100, selection.getXMin());
        assertEquals(-50, selection.getXMax());
        assertEquals(-200, selection.getYMin());
        assertEquals(-150, selection.getYMax());
        assertEquals(-300, selection.getZMin());
        assertEquals(-250, selection.getZMax());
    }
}
