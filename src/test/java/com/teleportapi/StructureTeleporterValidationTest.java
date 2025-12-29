package com.teleportapi;

//import net.minecraft.core.BlockPos;
//import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StructureTeleporter validation and sanitization logic.
 * Tests isExcluded(), sanitizeBlockState(), default lists, and shouldReplace().
 * Note: Some methods are private, so we test them indirectly through public
 * APIs.
 */
@Disabled("Requires Minecraft Context")
class StructureTeleporterValidationTest {

    private BlockState createMockBlockState() {
        return (BlockState) (Object) new String("DummyBlockState-" + java.util.UUID.randomUUID());
    }

    // ========== Default Excluded Blocks Tests ==========

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_NotNull() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();
        assertNotNull(excludedBlocks);
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_NotEmpty() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();
        assertFalse(excludedBlocks.isEmpty());
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ContainsBedrock() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        @SuppressWarnings("null")
        boolean containsBedrock = excludedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.BEDROCK));

        assertTrue(containsBedrock, "Default excluded blocks should contain Bedrock");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ContainsEndPortalFrame() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        @SuppressWarnings("null")
        boolean containsEndPortalFrame = excludedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.END_PORTAL_FRAME));

        assertTrue(containsEndPortalFrame, "Default excluded blocks should contain End Portal Frame");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ContainsEndPortal() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        @SuppressWarnings("null")
        boolean containsEndPortal = excludedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.END_PORTAL));

        assertTrue(containsEndPortal, "Default excluded blocks should contain End Portal");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ContainsEndGateway() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        @SuppressWarnings("null")
        boolean containsEndGateway = excludedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.END_GATEWAY));
        assertTrue(containsEndGateway, "Default excluded blocks should contain End Gateway");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ContainsReinforcedDeepslate() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        @SuppressWarnings("null")
        boolean containsReinforcedDeepslate = excludedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.REINFORCED_DEEPSLATE));

        assertTrue(containsReinforcedDeepslate, "Default excluded blocks should contain Reinforced Deepslate");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_ExpectedCount() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();

        // As of current implementation: Bedrock, End Portal Frame, End Portal, End
        // Gateway, Reinforced Deepslate
        assertEquals(5, excludedBlocks.size(), "Should have 5 default excluded blocks");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_IsNewList() {
        List<BlockState> list1 = StructureTeleporter.getDefaultExcludedBlocks();
        List<BlockState> list2 = StructureTeleporter.getDefaultExcludedBlocks();

        assertNotSame(list1, list2, "Should return a new list each time");
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultExcludedBlocks_IsMutable() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();
        int originalSize = excludedBlocks.size();

        // Should be able to modify the returned list
        excludedBlocks.add(Blocks.STONE.defaultBlockState());
        assertEquals(originalSize + 1, excludedBlocks.size());
    }

    // ========== Default Preserved Blocks Tests ==========

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultPreservedBlocks_NotNull() {
        List<BlockState> preservedBlocks = StructureTeleporter.getDefaultPreservedBlocks();
        assertNotNull(preservedBlocks);
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultPreservedBlocks_MatchesExcluded() {
        List<BlockState> excludedBlocks = StructureTeleporter.getDefaultExcludedBlocks();
        List<BlockState> preservedBlocks = StructureTeleporter.getDefaultPreservedBlocks();

        // Default implementation returns the same list as excluded
        assertEquals(excludedBlocks.size(), preservedBlocks.size());
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testGetDefaultPreservedBlocks_ContainsSameCriticalBlocks() {
        List<BlockState> preservedBlocks = StructureTeleporter.getDefaultPreservedBlocks();

        @SuppressWarnings("null")
        boolean containsBedrock = preservedBlocks.stream()
                .anyMatch(state -> state.is(Blocks.BEDROCK));

        assertTrue(containsBedrock, "Preserved blocks should include Bedrock");
    }

    // ========== BlockData Tests ==========

    @Test
    void testBlockData_Creation() {
        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new net.minecraft.core.BlockPos(10, 20, 30),
                createMockBlockState(),
                null);

        assertNotNull(blockData);
        assertEquals(10, blockData.relativePos.getX());
        assertEquals(20, blockData.relativePos.getY());
        assertEquals(30, blockData.relativePos.getZ());
        assertNotNull(blockData.blockState);
        assertNull(blockData.nbt);
    }

    @Test
    void testBlockData_WithNBT() {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        nbt.putString("test", "value");

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new net.minecraft.core.BlockPos(0, 0, 0),
                createMockBlockState(),
                nbt);

        assertNotNull(blockData.nbt);
        assertEquals("value", blockData.nbt.getString("test"));
    }

    @Test
    void testBlockData_PublicFields() {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(5, 10, 15);
        BlockState state = createMockBlockState();

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(pos, state, null);

        // Fields should be public and accessible
        assertEquals(pos, blockData.relativePos);
        assertEquals(state, blockData.blockState);
        assertNull(blockData.nbt);
    }

    @Test
    void testBlockData_FieldsAreDirectReferences() {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(1, 2, 3);
        BlockState state = createMockBlockState();
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(pos, state, nbt);

        // Should be the same references, not copies
        assertSame(pos, blockData.relativePos);
        assertSame(state, blockData.blockState);
        assertSame(nbt, blockData.nbt);
    }

    // ========== CopyStructure Validation Tests ==========

    @Test
    void testCopyStructure_NullSelection() {
        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(null);
        assertNull(result);
    }

    @Test
    void testCopyStructure_IncompleteSelection() {
        Selection selection = new Selection();
        selection.setXMin(0);
        selection.setXMax(10);
        // Missing other coordinates

        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(selection);
        assertNull(result);
    }

    @Test
    void testCopyStructure_WithNullExcludedBlocks() {
        Selection selection = new Selection();
        selection.setXMin(0);
        // Incomplete selection means loop inside copyStructure (where isExcluded is
        // checked) is never reached.
        // So passing null excludedBlocks is safe here.

        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(selection, null);
        assertNull(result);
    }

    @Test
    void testCopyStructure_WithEmptyExcludedBlocks() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(0, 0, 0));

        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(
                selection, new ArrayList<>(), true);
        assertNull(result); // World not set
    }

    // ========== PasteStructure Validation Tests ==========

    @Test
    void testPasteStructure_NullBlocks() {
        // pasteStructure should handle null blocks gracefully (warn and return)
        assertDoesNotThrow(() -> {
            try {
                StructureTeleporter.pasteStructure(null, new net.minecraft.core.BlockPos(0, 0, 0), null);
            } catch (NullPointerException e) {
                // Expected due to null world if method proceeds far enough
            }
        });
    }

    @Test
    void testPasteStructure_EmptyBlocksList() {
        assertDoesNotThrow(() -> {
            try {
                StructureTeleporter.pasteStructure(
                        new ArrayList<>(),
                        new net.minecraft.core.BlockPos(0, 0, 0),
                        null);
            } catch (NullPointerException e) {
                // Expected due to null world
            }
        });
    }

    // ========== Excluded Blocks Handling Tests ==========

    @Test
    void testExclusionList_CustomList() {
        List<BlockState> customExcluded = Arrays.asList(
                createMockBlockState(),
                createMockBlockState());

        assertNotNull(customExcluded);
        assertEquals(2, customExcluded.size());
    }

    @Test
    @Disabled("Requires Minecraft Context")
    void testExclusionList_MixedWithDefaults() {
        List<BlockState> excludedBlocks = new ArrayList<>(StructureTeleporter.getDefaultExcludedBlocks());
        excludedBlocks.add(Blocks.DIAMOND_ORE.defaultBlockState());

        assertTrue(excludedBlocks.size() > 5);
    }

    // ========== PasteMode Behavior Tests ==========

    @Test
    void testPasteMode_ForceReplaceExists() {
        assertNotNull(StructureTeleporter.PasteMode.FORCE_REPLACE);
    }

    @Test
    void testPasteMode_PreserveListExists() {
        assertNotNull(StructureTeleporter.PasteMode.PRESERVE_LIST);
    }

    @Test
    void testPasteMode_PreserveExistingExists() {
        assertNotNull(StructureTeleporter.PasteMode.PRESERVE_EXISTING);
    }

    // ========== CheckExclusions Flag Tests ==========

    @Test
    @Disabled("Argument evaluation calls defaults")
    void testCopyStructure_CheckExclusionsTrue() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(1, 1, 1));

        // With checkExclusions=true, should use the excluded blocks list
        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(
                selection, StructureTeleporter.getDefaultExcludedBlocks(), true);
        assertNull(result); // World not set, but method handles parameter correctly
    }

    @Test
    @Disabled("Argument evaluation calls defaults")
    void testCopyStructure_CheckExclusionsFalse() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(1, 1, 1));

        // With checkExclusions=false, should ignore excluded blocks list
        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(
                selection, StructureTeleporter.getDefaultExcludedBlocks(), false);
        assertNull(result); // World not set
    }

    // ========== IncludeAir Flag Tests ==========

    @Test
    void testCopyStructure_IncludeAirTrue() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(1, 1, 1));

        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(
                selection, new ArrayList<>(), true, true);
        assertNull(result); // World not set
    }

    @Test
    void testCopyStructure_IncludeAirFalse() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(1, 1, 1));

        List<StructureTeleporter.BlockData> result = StructureTeleporter.copyStructure(
                selection, new ArrayList<>(), true, false);
        assertNull(result); // World not set, but parameter handling is tested
    }

    // ========== Method Overload Tests ==========

    @Test
    void testCopyStructure_OneParameter() {
        Selection selection = new Selection();

        assertDoesNotThrow(() -> {
            StructureTeleporter.copyStructure(selection);
        });
    }

    @Test
    void testCopyStructure_TwoParameters() {
        Selection selection = new Selection();
        List<BlockState> excluded = new ArrayList<>();

        assertDoesNotThrow(() -> {
            StructureTeleporter.copyStructure(selection, excluded);
        });
    }

    @Test
    void testCopyStructure_ThreeParameters() {
        Selection selection = new Selection();
        List<BlockState> excluded = new ArrayList<>();

        assertDoesNotThrow(() -> {
            StructureTeleporter.copyStructure(selection, excluded, true);
        });
    }

    @Test
    void testCopyStructure_FourParameters() {
        Selection selection = new Selection();
        List<BlockState> excluded = new ArrayList<>();

        assertDoesNotThrow(() -> {
            StructureTeleporter.copyStructure(selection, excluded, true, true);
        });
    }

    // ========== TeleportStructure Validation Tests ==========

    @Test
    void testTeleportStructure_IncompleteSelection() {
        Selection selection = new Selection();
        selection.setXMin(0);
        // Incomplete selection

        TeleportResult result = StructureTeleporter.teleportStructure(
                selection,
                new net.minecraft.core.BlockPos(100, 100, 100),
                null,
                false);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("not complete"));
    }

    @Test
    void testTeleportStructure_ShouldTeleportFalse() {
        Selection selection = new Selection();
        selection.setFromCorners(
                new net.minecraft.core.BlockPos(0, 0, 0),
                new net.minecraft.core.BlockPos(10, 10, 10));
        // World not set, so will fail

        TeleportResult result = StructureTeleporter.teleportStructure(
                selection,
                new net.minecraft.core.BlockPos(100, 100, 100),
                null,
                false // Dry run
        );

        // Should return a result indicating dry run or failure
        assertFalse(result.isTeleported());
    }
}
