package com.teleportapi.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Utility class for creating and managing GameTest structures programmatically.
 * Provides helper methods for common test patterns and debug visualization.
 */
public class GameTestTemplate {

    /**
     * Fills a rectangular region with the specified block.
     * 
     * @param helper The GameTestHelper instance
     * @param from   Starting position (relative to structure)
     * @param to     Ending position (relative to structure)
     * @param block  Block to fill with
     */
    @SuppressWarnings("null")
    public static void fillRegion(GameTestHelper helper, BlockPos from, BlockPos to, BlockState block) {
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    helper.setBlock(new BlockPos(x, y, z), block);
                }
            }
        }
    }

    /**
     * Creates a simple hollow box structure for testing.
     * Useful for testing selection and teleportation boundaries.
     * 
     * @param helper    The GameTestHelper instance
     * @param corner1   First corner of the box
     * @param corner2   Opposite corner of the box
     * @param wallBlock Block to use for walls
     */
    @SuppressWarnings("null")
    public static void createHollowBox(GameTestHelper helper, BlockPos corner1, BlockPos corner2,
            BlockState wallBlock) {
        int minX = Math.min(corner1.getX(), corner2.getX());
        int maxX = Math.max(corner1.getX(), corner2.getX());
        int minY = Math.min(corner1.getY(), corner2.getY());
        int maxY = Math.max(corner1.getY(), corner2.getY());
        int minZ = Math.min(corner1.getZ(), corner2.getZ());
        int maxZ = Math.max(corner1.getZ(), corner2.getZ());

        // Create walls
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Only place blocks on the edges
                    if (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ) {
                        helper.setBlock(new BlockPos(x, y, z), wallBlock);
                    }
                }
            }
        }
    }

    /**
     * Places visual markers at specific coordinates for debugging.
     * Uses colored concrete blocks to mark important positions.
     * 
     * @param helper     The GameTestHelper instance
     * @param pos        Position to mark
     * @param markerType Type of marker (0=red, 1=green, 2=blue, 3=yellow)
     */
    @SuppressWarnings("null")
    public static void placeDebugMarker(GameTestHelper helper, BlockPos pos, int markerType) {
        BlockState marker;
        switch (markerType) {
            case 1: // Green - Success/Start
                marker = Blocks.GREEN_CONCRETE.defaultBlockState();
                break;
            case 2: // Blue - Info/Mid
                marker = Blocks.BLUE_CONCRETE.defaultBlockState();
                break;
            case 3: // Yellow - Warning
                marker = Blocks.YELLOW_CONCRETE.defaultBlockState();
                break;
            default: // Red - Error/End
                marker = Blocks.RED_CONCRETE.defaultBlockState();
                break;
        }
        helper.setBlock(pos, marker);
    }

    /**
     * Clears a rectangular region by setting all blocks to air.
     * 
     * @param helper The GameTestHelper instance
     * @param from   Starting position
     * @param to     Ending position
     */
    public static void clearRegion(GameTestHelper helper, BlockPos from, BlockPos to) {
        fillRegion(helper, from, to, Blocks.AIR.defaultBlockState());
    }

    /**
     * Creates a checkerboard pattern for visual testing.
     * 
     * @param helper The GameTestHelper instance
     * @param from   Starting position
     * @param to     Ending position
     * @param block1 First block type
     * @param block2 Second block type
     */
    @SuppressWarnings("null")
    public static void createCheckerboard(GameTestHelper helper, BlockPos from, BlockPos to,
            BlockState block1, BlockState block2) {
        int minX = Math.min(from.getX(), to.getX());
        int maxX = Math.max(from.getX(), to.getX());
        int minY = Math.min(from.getY(), to.getY());
        int maxY = Math.max(from.getY(), to.getY());
        int minZ = Math.min(from.getZ(), to.getZ());
        int maxZ = Math.max(from.getZ(), to.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    // Checkerboard pattern based on sum of coordinates
                    boolean useFirst = ((x + y + z) % 2) == 0;
                    helper.setBlock(new BlockPos(x, y, z), useFirst ? block1 : block2);
                }
            }
        }
    }

    /**
     * Logs a formatted debug message to the game test output.
     * 
     * @param helper  The GameTestHelper instance
     * @param message Debug message to log
     */
    public static void logDebug(GameTestHelper helper, String message) {
        com.mojang.logging.LogUtils.getLogger().info("[GameTest Debug] " + message);
    }

    /**
     * Asserts that two BlockPos are equal, providing detailed error message on
     * failure.
     * 
     * @param helper   The GameTestHelper instance
     * @param expected Expected position
     * @param actual   Actual position
     * @param message  Error message prefix
     */
    public static void assertBlockPosEquals(GameTestHelper helper, BlockPos expected, BlockPos actual, String message) {
        if (!expected.equals(actual)) {
            helper.fail(message + " - Expected: " + expected + ", but got: " + actual);
        }
    }

    /**
     * Asserts that a coordinate value matches expected value.
     * 
     * @param helper         The GameTestHelper instance
     * @param expected       Expected coordinate
     * @param actual         Actual coordinate
     * @param coordinateName Name of the coordinate (e.g., "xMin", "yMax")
     */
    public static void assertCoordinateEquals(GameTestHelper helper, int expected, int actual, String coordinateName) {
        if (expected != actual) {
            helper.fail(coordinateName + " mismatch - Expected: " + expected + ", but got: " + actual);
        }
    }
}
