package com.teleportapi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Selection class.
 * Tests coordinate management, validation, and selection methods.
 * Note: Some tests are limited because we cannot easily mock Minecraft's Level
 * class.
 */
class SelectionTest {

    private Selection selection;

    @BeforeEach
    void setUp() {
        selection = new Selection();
    }

    @Test
    void testSetAndGetFacePoint() {
        BlockPos testPos = new BlockPos(10, 20, 30);
        selection.setFacePoint(FaceType.X_MIN, testPos);

        assertEquals(testPos, selection.getFacePoint(FaceType.X_MIN));
    }

    @Test
    void testRemoveFacePoint() {
        BlockPos testPos = new BlockPos(10, 20, 30);
        selection.setFacePoint(FaceType.X_MIN, testPos);

        assertNotNull(selection.getFacePoint(FaceType.X_MIN));

        selection.removeFacePoint(FaceType.X_MIN);

        assertNull(selection.getFacePoint(FaceType.X_MIN));
    }

    @Test
    void testCoordinateSetters() {
        selection.setXMin(10);
        selection.setXMax(20);
        selection.setYMin(5);
        selection.setYMax(15);
        selection.setZMin(0);
        selection.setZMax(10);

        assertEquals(10, selection.getXMin());
        assertEquals(20, selection.getXMax());
        assertEquals(5, selection.getYMin());
        assertEquals(15, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(10, selection.getZMax());
    }

    @Test
    void testCoordinateGettersReturnNullWhenNotSet() {
        assertNull(selection.getXMin());
        assertNull(selection.getXMax());
        assertNull(selection.getYMin());
        assertNull(selection.getYMax());
        assertNull(selection.getZMin());
        assertNull(selection.getZMax());
    }

    @Test
    void testIsCompleteReturnsFalseWhenNotAllFacesSet() {
        assertFalse(selection.isComplete());

        selection.setXMin(0);
        assertFalse(selection.isComplete());

        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        assertFalse(selection.isComplete()); // Still missing Z_MAX and world
    }

    @Test
    void testGetSetFacesCount() {
        assertEquals(0, selection.getSetFacesCount());

        selection.setXMin(0);
        assertEquals(1, selection.getSetFacesCount());

        selection.setXMax(10);
        selection.setYMin(0);
        assertEquals(3, selection.getSetFacesCount());

        selection.removeFacePoint(FaceType.X_MIN);
        assertEquals(2, selection.getSetFacesCount());
    }

    @Test
    void testGetMinAndMaxReturnNullWhenIncomplete() {
        selection.setXMin(0);
        selection.setXMax(10);

        assertNull(selection.getMin());
        assertNull(selection.getMax());
    }

    @Test
    void testSetFromCornersWithValidPoints() {
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 10, 10);

        selection.setFromCorners(p1, p2);

        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(10, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(10, selection.getZMax());
        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testSetFromCornersWithReversedPoints() {
        BlockPos p1 = new BlockPos(10, 10, 10);
        BlockPos p2 = new BlockPos(0, 0, 0);

        selection.setFromCorners(p1, p2);

        // Should still correctly identify min and max
        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(10, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(10, selection.getZMax());
    }

    @Test
    void testSetFromCornersWithNullPointsThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromCorners(null, new BlockPos(0, 0, 0));
        });

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromCorners(new BlockPos(0, 0, 0), null);
        });
    }

    @Test
    void testSetFromPointsWithValidPoints() {
        BlockPos[] points = {
                new BlockPos(0, 0, 0),
                new BlockPos(10, 5, 3),
                new BlockPos(2, 10, 8)
        };

        selection.setFromPoints(points);

        assertEquals(0, selection.getXMin());
        assertEquals(10, selection.getXMax());
        assertEquals(0, selection.getYMin());
        assertEquals(10, selection.getYMax());
        assertEquals(0, selection.getZMin());
        assertEquals(8, selection.getZMax());
    }

    @Test
    void testSetFromPointsWithEmptyArrayThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromPoints(new BlockPos[0]);
        });
    }

    @Test
    void testSetFromPointsWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromPoints((BlockPos[]) null);
        });
    }

    @Test
    void testSetFromSixPointsWithValidPoints() {
        BlockPos[] points = {
                new BlockPos(0, 0, 0),
                new BlockPos(10, 0, 0),
                new BlockPos(5, 5, 0),
                new BlockPos(5, 10, 0),
                new BlockPos(0, 0, 8),
                new BlockPos(10, 10, 8)
        };

        selection.setFromSixPoints(points);

        assertEquals(6, selection.getSetFacesCount());
    }

    @Test
    void testSetFromSixPointsWithWrongCountThrowsException() {
        BlockPos[] tooFew = {
                new BlockPos(0, 0, 0),
                new BlockPos(10, 10, 10)
        };

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromSixPoints(tooFew);
        });

        BlockPos[] tooMany = new BlockPos[7];
        for (int i = 0; i < 7; i++) {
            tooMany[i] = new BlockPos(i, i, i);
        }

        assertThrows(IllegalArgumentException.class, () -> {
            selection.setFromSixPoints(tooMany);
        });
    }

    @Test
    void testGetInfoWhenIncomplete() {
        String info = selection.getInfo();
        assertTrue(info.contains("incomplete"));
        assertTrue(info.contains("0/6"));
    }

    @Test
    void testReset() {
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        assertEquals(6, selection.getSetFacesCount());

        selection.reset();

        assertEquals(0, selection.getSetFacesCount());
        assertNull(selection.getWorld());
        assertNull(selection.getXMin());
        assertNull(selection.getXMax());
    }
}
