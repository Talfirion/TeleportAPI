package com.teleportapi.gametest;

import com.teleportapi.FaceType;
import com.teleportapi.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Comprehensive GameTests for the Selection class.
 * Tests all major functionality including coordinate management, face points,
 * and validation.
 */
@GameTestHolder("teleportapi")
public class SelectionGameTests {

    /**
     * Tests basic coordinate setting and retrieval.
     * 
     * Structure: teleportapi:selectiongametests.test_coordinates
     */
    @GameTest(template = "empty3x3x3")
    public static void testCoordinates(GameTestHelper helper) {
        Selection selection = new Selection();

        // Test setting individual coordinates
        selection.setXMin(1);
        selection.setXMax(9);
        selection.setYMin(2);
        selection.setYMax(8);
        selection.setZMin(3);
        selection.setZMax(7);

        // Verify all coordinates
        GameTestTemplate.assertCoordinateEquals(helper, 1, selection.getXMin(), "xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 9, selection.getXMax(), "xMax");
        GameTestTemplate.assertCoordinateEquals(helper, 2, selection.getYMin(), "yMin");
        GameTestTemplate.assertCoordinateEquals(helper, 8, selection.getYMax(), "yMax");
        GameTestTemplate.assertCoordinateEquals(helper, 3, selection.getZMin(), "zMin");
        GameTestTemplate.assertCoordinateEquals(helper, 7, selection.getZMax(), "zMax");

        // Verify set faces count
        if (selection.getSetFacesCount() != 6) {
            helper.fail("Expected 6 set faces, got: " + selection.getSetFacesCount());
        }

        helper.succeed();
    }

    /**
     * Tests face point setting and retrieval.
     * 
     * Structure: teleportapi:selectiongametests.test_face_points
     */
    @GameTest(template = "empty3x3x3")
    public static void testFacePoints(GameTestHelper helper) {
        Selection selection = new Selection();

        // Set face points
        BlockPos xMinPos = new BlockPos(0, 5, 5);
        BlockPos xMaxPos = new BlockPos(10, 5, 5);
        BlockPos yMinPos = new BlockPos(5, 0, 5);
        BlockPos yMaxPos = new BlockPos(5, 10, 5);

        selection.setFacePoint(FaceType.X_MIN, xMinPos);
        selection.setFacePoint(FaceType.X_MAX, xMaxPos);
        selection.setFacePoint(FaceType.Y_MIN, yMinPos);
        selection.setFacePoint(FaceType.Y_MAX, yMaxPos);

        // Verify face points
        GameTestTemplate.assertBlockPosEquals(helper, xMinPos, selection.getFacePoint(FaceType.X_MIN),
                "X_MIN face point");
        GameTestTemplate.assertBlockPosEquals(helper, xMaxPos, selection.getFacePoint(FaceType.X_MAX),
                "X_MAX face point");
        GameTestTemplate.assertBlockPosEquals(helper, yMinPos, selection.getFacePoint(FaceType.Y_MIN),
                "Y_MIN face point");
        GameTestTemplate.assertBlockPosEquals(helper, yMaxPos, selection.getFacePoint(FaceType.Y_MAX),
                "Y_MAX face point");

        // Verify coordinates are extracted correctly
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin from face");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax from face");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getYMin(), "yMin from face");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getYMax(), "yMax from face");

        helper.succeed();
    }

    /**
     * Tests removing face points.
     * 
     * Structure: teleportapi:selectiongametests.test_remove_face
     */
    @GameTest(template = "empty3x3x3")
    public static void testRemoveFace(GameTestHelper helper) {
        Selection selection = new Selection();

        // Set a face point
        BlockPos testPos = new BlockPos(5, 5, 5);
        selection.setFacePoint(FaceType.X_MIN, testPos);

        if (selection.getFacePoint(FaceType.X_MIN) == null) {
            helper.fail("Face point should be set");
        }

        // Remove the face point
        selection.removeFacePoint(FaceType.X_MIN);

        if (selection.getFacePoint(FaceType.X_MIN) != null) {
            helper.fail("Face point should be null after removal");
        }

        if (selection.getXMin() != null) {
            helper.fail("xMin should be null after face removal");
        }

        helper.succeed();
    }

    /**
     * Tests setFromCorners with various corner configurations.
     * 
     * Structure: teleportapi:selectiongametests.test_corners
     */
    @GameTest(template = "empty3x3x3")
    public static void testSetFromCorners(GameTestHelper helper) {
        Selection selection = new Selection();

        // Test normal order
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 10, 10);
        selection.setFromCorners(p1, p2);

        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax");

        // Reset and test reversed order
        selection.reset();
        selection.setFromCorners(p2, p1);

        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin reversed");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax reversed");

        // Test mixed coordinates
        selection.reset();
        BlockPos p3 = new BlockPos(5, 0, 10);
        BlockPos p4 = new BlockPos(0, 8, 2);
        selection.setFromCorners(p3, p4);

        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin mixed");
        GameTestTemplate.assertCoordinateEquals(helper, 5, selection.getXMax(), "xMax mixed");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getYMin(), "yMin mixed");
        GameTestTemplate.assertCoordinateEquals(helper, 8, selection.getYMax(), "yMax mixed");
        GameTestTemplate.assertCoordinateEquals(helper, 2, selection.getZMin(), "zMin mixed");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getZMax(), "zMax mixed");

        helper.succeed();
    }

    /**
     * Tests setFromPoints with multiple positions.
     * 
     * Structure: teleportapi:selectiongametests.test_multiple_points
     */
    @GameTest(template = "empty3x3x3")
    public static void testSetFromPoints(GameTestHelper helper) {
        Selection selection = new Selection();

        BlockPos[] points = {
                new BlockPos(5, 5, 5),
                new BlockPos(10, 2, 8),
                new BlockPos(1, 9, 3),
                new BlockPos(7, 4, 10)
        };

        selection.setFromPoints(points);

        // Should find min and max from all points
        GameTestTemplate.assertCoordinateEquals(helper, 1, selection.getXMin(), "xMin from points");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax from points");
        GameTestTemplate.assertCoordinateEquals(helper, 2, selection.getYMin(), "yMin from points");
        GameTestTemplate.assertCoordinateEquals(helper, 9, selection.getYMax(), "yMax from points");
        GameTestTemplate.assertCoordinateEquals(helper, 3, selection.getZMin(), "zMin from points");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getZMax(), "zMax from points");

        helper.succeed();
    }

    /**
     * Tests isComplete validation logic.
     * 
     * Structure: teleportapi:selectiongametests.test_completion
     */
    @GameTest(template = "empty3x3x3")
    public static void testCompletion(GameTestHelper helper) {
        Selection selection = new Selection();

        // Should not be complete initially
        if (selection.isComplete()) {
            helper.fail("Selection should not be complete initially");
        }

        // Set only some coordinates
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);

        if (selection.isComplete()) {
            helper.fail("Selection should not be complete with partial coordinates");
        }

        // Complete all coordinates except world
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // Note: Still won't be complete without world, but tests coordinate
        // completeness
        int faceCount = selection.getSetFacesCount();
        if (faceCount != 6) {
            helper.fail("Expected 6 set faces, got: " + faceCount);
        }

        helper.succeed();
    }

    /**
     * Tests reset functionality.
     * 
     * Structure: teleportapi:selectiongametests.test_reset
     */
    @GameTest(template = "empty3x3x3")
    public static void testReset(GameTestHelper helper) {
        Selection selection = new Selection();

        // Set everything
        selection.setFromCorners(new BlockPos(0, 0, 0), new BlockPos(10, 10, 10));

        if (selection.getSetFacesCount() == 0) {
            helper.fail("Selection should have faces set before reset");
        }

        // Reset
        selection.reset();

        // Verify everything is cleared
        if (selection.getSetFacesCount() != 0) {
            helper.fail("Selection should have 0 faces after reset");
        }

        if (selection.getXMin() != null) {
            helper.fail("xMin should be null after reset");
        }

        if (selection.getWorld() != null) {
            helper.fail("World should be null after reset");
        }

        helper.succeed();
    }
}
