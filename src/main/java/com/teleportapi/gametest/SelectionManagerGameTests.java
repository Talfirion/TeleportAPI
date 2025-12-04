package com.teleportapi.gametest;

import com.teleportapi.Selection;
import com.teleportapi.SelectionManager;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * GameTests for SelectionManager focusing on player-specific functionality.
 * These tests validate player selection management that cannot be tested in
 * standard JUnit without Minecraft's Bootstrap initialization.
 * 
 * Tests covered:
 * - getSelection() creates new Selection for new players
 * - getSelection() returns same Selection for same player
 * - Different players get different Selections
 * - clearSelection() removes player's selection
 * - Selection state persists across getSelection() calls
 */
@GameTestHolder("teleportapi")
public class SelectionManagerGameTests {

    /**
     * Test that getSelection creates a new Selection for a new player.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testGetSelectionCreatesNewSelection(GameTestHelper helper) {
        // Spawn a mock/fake player in the test area
        Player player = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();
        Selection selection = manager.getSelection(player);

        // Verify selection was created
        if (selection == null) {
            helper.fail("getSelection should create a new Selection for a new player");
        }

        GameTestTemplate.logDebug(helper,
                "Successfully created new selection for player: " + player.getName().getString());
        helper.succeed();
    }

    /**
     * Test that getSelection returns the same Selection instance for the same
     * player.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testGetSelectionReturnsSameInstance(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();
        Selection selection1 = manager.getSelection(player);
        Selection selection2 = manager.getSelection(player);

        // Verify both calls return the exact same instance
        if (selection1 != selection2) {
            helper.fail("getSelection should return the same Selection instance for the same player");
        }

        GameTestTemplate.logDebug(helper, "Verified same selection instance returned for player");
        helper.succeed();
    }

    /**
     * Test that different players get different Selection instances.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testDifferentPlayersGetDifferentSelections(GameTestHelper helper) {
        Player player1 = helper.makeMockPlayer();
        Player player2 = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();
        Selection selection1 = manager.getSelection(player1);
        Selection selection2 = manager.getSelection(player2);

        // Verify different players have different selections
        if (selection1 == selection2) {
            helper.fail("Different players should have different Selection instances");
        }

        GameTestTemplate.logDebug(helper, "Verified different players have different selections");
        helper.succeed();
    }

    /**
     * Test that clearSelection removes a player's selection.
     * After clearing, a new Selection should be created on next getSelection call.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testClearSelectionRemovesSelection(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();

        // Get initial selection
        Selection selection1 = manager.getSelection(player);

        // Clear the selection
        manager.clearSelection(player);

        // Get selection again - should be a new instance
        Selection selection2 = manager.getSelection(player);

        // Verify it's a different instance (new selection created)
        if (selection1 == selection2) {
            helper.fail("After clearSelection, a new Selection instance should be created");
        }

        GameTestTemplate.logDebug(helper, "Verified clearSelection removes and recreates selection");
        helper.succeed();
    }

    /**
     * Test that Selection modifications persist across getSelection calls.
     * This verifies that the SelectionManager properly maintains player state.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testSelectionStatePersistence(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();

        // Get selection and modify it
        Selection selection = manager.getSelection(player);
        selection.setXMin(10);
        selection.setYMax(100);
        selection.setZMax(50);

        // Get selection again
        Selection retrievedSelection = manager.getSelection(player);

        // Verify modifications persisted
        GameTestTemplate.assertCoordinateEquals(helper, 10, retrievedSelection.getXMin(), "xMin persistence");
        GameTestTemplate.assertCoordinateEquals(helper, 100, retrievedSelection.getYMax(), "yMax persistence");
        GameTestTemplate.assertCoordinateEquals(helper, 50, retrievedSelection.getZMax(), "zMax persistence");

        GameTestTemplate.logDebug(helper, "Verified selection state persists across getSelection calls");
        helper.succeed();
    }

    /**
     * Test that multiple players can have independent selections with different
     * values.
     * This is a comprehensive test combining player isolation and state
     * persistence.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testMultiplePlayersIndependentSelections(GameTestHelper helper) {
        Player player1 = helper.makeMockPlayer();
        Player player2 = helper.makeMockPlayer();
        Player player3 = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();

        // Get selections and set different values
        Selection sel1 = manager.getSelection(player1);
        Selection sel2 = manager.getSelection(player2);
        Selection sel3 = manager.getSelection(player3);

        sel1.setXMin(1);
        sel1.setYMin(10);
        sel2.setXMin(2);
        sel2.setYMin(20);
        sel3.setXMin(3);
        sel3.setYMin(30);

        // Retrieve again and verify each player's selection is independent
        GameTestTemplate.assertCoordinateEquals(helper, 1, manager.getSelection(player1).getXMin(), "Player 1 xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 10, manager.getSelection(player1).getYMin(), "Player 1 yMin");

        GameTestTemplate.assertCoordinateEquals(helper, 2, manager.getSelection(player2).getXMin(), "Player 2 xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 20, manager.getSelection(player2).getYMin(), "Player 2 yMin");

        GameTestTemplate.assertCoordinateEquals(helper, 3, manager.getSelection(player3).getXMin(), "Player 3 xMin");
        GameTestTemplate.assertCoordinateEquals(helper, 30, manager.getSelection(player3).getYMin(), "Player 3 yMin");

        GameTestTemplate.logDebug(helper, "Verified 3 players have independent selections");
        helper.succeed();
    }

    /**
     * Test that clearSelection gracefully handles non-existent player selections.
     * Should not throw an exception.
     * 
     * Structure: selectiongametests.empty3x3x3
     */
    @GameTest(template = "empty3x3x3")
    public static void testClearNonExistentSelection(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();

        SelectionManager manager = SelectionManager.getInstance();

        // Clear without ever getting a selection first - should not crash
        try {
            manager.clearSelection(player);
            GameTestTemplate.logDebug(helper, "Verified clearSelection handles non-existent player gracefully");
            helper.succeed();
        } catch (Exception e) {
            helper.fail("clearSelection should not throw exception for non-existent player: " + e.getMessage());
        }
    }
}
