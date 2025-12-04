package com.teleportapi.gametest;

import com.teleportapi.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Example GameTests for TeleportAPI demonstrating the framework usage.
 * These tests showcase basic testing patterns and serve as templates for
 * writing new tests.
 */
@GameTestHolder("teleportapi")
public class TeleportAPIGameTests {

    /**
     * Example test demonstrating basic Selection coordinate validation.
     * Tests that coordinates can be set and retrieved correctly.
     * 
     * Structure: teleportapi:teleportapigametests.example_test
     */
    @GameTest(template = "example_test")
    public static void exampleTest(GameTestHelper helper) {
        // Create a new Selection instance
        Selection selection = new Selection();

        // Set coordinates
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // Verify coordinates were set correctly
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getYMin(), "yMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getYMax(), "yMax");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getZMin(), "zMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getZMax(), "zMax");

        // Verify all 6 faces are set
        if (selection.getSetFacesCount() != 6) {
            helper.fail("Expected 6 faces to be set, but got: " + selection.getSetFacesCount());
        }

        GameTestTemplate.logDebug(helper, "Example test completed successfully!");

        // Mark test as successful
        helper.succeed();
    }

    /**
     * Test that demonstrates Selection.setFromCorners functionality.
     * Verifies that min/max coordinates are calculated correctly regardless of
     * corner order.
     * 
     * Structure: teleportapi:teleportapigametests.corners_test
     */
    @GameTest(template = "corners_test")
    public static void cornersTest(GameTestHelper helper) {
        Selection selection = new Selection();

        // Test with reversed corners (max before min)
        BlockPos corner1 = new BlockPos(10, 10, 10);
        BlockPos corner2 = new BlockPos(0, 0, 0);

        selection.setFromCorners(corner1, corner2);

        // Should correctly identify min and max regardless of order
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getXMin(), "xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getXMax(), "xMax");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getYMin(), "yMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getYMax(), "yMax");
        GameTestTemplate.assertCoordinateEquals(helper, 0, selection.getZMin(), "zMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, selection.getZMax(), "zMax");

        GameTestTemplate.logDebug(helper, "Corners test completed successfully!");
        helper.succeed();
    }

    /**
     * Test demonstrating delayed success with tick scheduling.
     * Shows how to test time-dependent behavior.
     * 
     * Structure: teleportapi:teleportapigametests.delayed_test
     */
    @GameTest(template = "delayed_test")
    public static void delayedTest(GameTestHelper helper) {
        Selection selection = new Selection();

        // Set coordinates immediately
        selection.setXMin(5);
        selection.setXMax(15);

        // Schedule verification after 10 ticks
        helper.runAfterDelay(10, () -> {
            GameTestTemplate.assertCoordinateEquals(helper, 5, selection.getXMin(), "xMin after delay");
            GameTestTemplate.assertCoordinateEquals(helper, 15, selection.getXMax(), "xMax after delay");

            GameTestTemplate.logDebug(helper, "Delayed test completed successfully!");
            helper.succeed();
        });
    }

    /**
     * Test demonstrating Selection.reset functionality.
     * Verifies that reset properly clears all coordinates.
     * 
     * Structure: teleportapi:teleportapigametests.reset_test
     */
    @GameTest(template = "reset_test")
    public static void resetTest(GameTestHelper helper) {
        Selection selection = new Selection();

        // Set all coordinates
        selection.setXMin(0);
        selection.setXMax(10);
        selection.setYMin(0);
        selection.setYMax(10);
        selection.setZMin(0);
        selection.setZMax(10);

        // Verify they're set
        if (selection.getSetFacesCount() != 6) {
            helper.fail("Expected 6 faces before reset, but got: " + selection.getSetFacesCount());
        }

        // Reset the selection
        selection.reset();

        // Verify everything is cleared
        if (selection.getSetFacesCount() != 0) {
            helper.fail("Expected 0 faces after reset, but got: " + selection.getSetFacesCount());
        }

        if (selection.getXMin() != null || selection.getXMax() != null) {
            helper.fail("Expected null coordinates after reset");
        }

        GameTestTemplate.logDebug(helper, "Reset test completed successfully!");
        helper.succeed();
    }
}
