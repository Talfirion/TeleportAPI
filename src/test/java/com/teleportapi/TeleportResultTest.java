package com.teleportapi;

import net.minecraft.world.level.block.state.BlockState;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the TeleportResult class.
 * Tests all constructors, getters, immutability, and edge cases.
 */
class TeleportResultTest {

    // Use Object to avoid ClassCastException when casting String to BlockState
    // Helper to create a Set with distinct objects masked as Set<BlockState>
    @SuppressWarnings("unchecked")
    private Set<BlockState> createMockBlockStateSet(int count) {
        Set<Object> raw = new HashSet<>();
        for (int i = 0; i < count; i++) {
            raw.add(new String("Dummy-" + i + "-" + java.util.UUID.randomUUID()));
        }
        return (Set<BlockState>) (Set<?>) raw;
    }

    // Helper to create a Map with distinct objects masked as keys
    @SuppressWarnings("unchecked")
    private Map<BlockState, Integer> createMockBlockStateMap(int count) {
        Map<Object, Integer> raw = new HashMap<>();
        for (int i = 0; i < count; i++) {
            raw.put(new String("Dummy-" + i + "-" + java.util.UUID.randomUUID()), 10 + i);
        }
        return (Map<BlockState, Integer>) (Map<?, ?>) raw;
    }

    // Helper to get a single dummy object masked as BlockState (for adding to
    // immutable sets test)
    @SuppressWarnings("unused")
    private BlockState createDummyBlockState() {
        return (BlockState) (Object) new String("SingleDummy-" + java.util.UUID.randomUUID());
    }

    // ========== Constructor Tests ==========

    @Test
    void testSimpleConstructor() {
        Set<BlockState> excludedTypes = createMockBlockStateSet(1);

        TeleportResult result = new TeleportResult(
                true, 100, 5, excludedTypes, "Test message", true);

        assertTrue(result.isSuccess());
        assertEquals(100, result.getTotalBlocks());
        assertEquals(5, result.getExcludedBlocks());
        assertEquals(95, result.getTeleportedBlocks());
        assertEquals("Test message", result.getMessage());
        assertTrue(result.isTeleported());
        assertEquals(1, result.getExcludedBlockTypes().size());
    }

    @Test
    void testConstructorWithCounts() {
        Set<BlockState> excludedTypes = new HashSet<>();

        TeleportResult result = new TeleportResult(
                true, 200, 10, excludedTypes, "Success", true, 50, 30);

        assertEquals(200, result.getTotalBlocks());
        assertEquals(10, result.getExcludedBlocks());
        assertEquals(50, result.getReplacedBlockCount());
        assertEquals(30, result.getSkippedBlockCount());
    }

    @Test
    void testConstructorWithMaps() {
        Set<BlockState> excludedTypes = new HashSet<>();
        Map<BlockState, Integer> replacedMap = createMockBlockStateMap(1);
        Map<BlockState, Integer> skippedMap = createMockBlockStateMap(1);

        TeleportResult result = new TeleportResult(
                true, 100, 5, excludedTypes, "Test", true,
                10, 5, replacedMap, skippedMap);

        assertEquals(10, result.getReplacedBlockCount());
        assertEquals(5, result.getSkippedBlockCount());
        assertEquals(1, result.getReplacedBlocksMap().size());
        assertEquals(1, result.getSkippedBlocksMap().size());
    }

    @Test
    void testFullConstructor() {
        Set<BlockState> excludedTypes = new HashSet<>();
        Map<BlockState, Integer> replacedMap = new HashMap<>();
        Map<BlockState, Integer> skippedMap = new HashMap<>();
        List<String> players = Arrays.asList("Player1", "Player2");

        TeleportResult result = new TeleportResult(
                true, 150, 10, excludedTypes, "Full test", true,
                40, 20, 10, 30, 120, 15,
                replacedMap, skippedMap, 5, players);

        assertEquals(150, result.getTotalBlocks());
        assertEquals(10, result.getExcludedBlocks());
        assertEquals(40, result.getReplacedBlockCount());
        assertEquals(20, result.getSkippedBlockCount());
        assertEquals(10, result.getSkippedByLimitCount());
        assertEquals(30, result.getAirBlockCount());
        assertEquals(120, result.getSolidBlockCount());
        assertEquals(15, result.getDestinationSolidBlocksLost());
        assertEquals(5, result.getTeleportedEntitiesCount());
        assertEquals(2, result.getTeleportedPlayerNames().size());
    }

    // ========== Success/Failure States ==========

    @Test
    void testSuccessState() {
        TeleportResult result = new TeleportResult(
                true, 50, 0, new HashSet<>(), "Success", true);

        assertTrue(result.isSuccess());
        assertTrue(result.isTeleported());
    }

    @Test
    void testFailureState() {
        TeleportResult result = new TeleportResult(
                false, 0, 0, new HashSet<>(), "Failed", false);

        assertFalse(result.isSuccess());
        assertFalse(result.isTeleported());
    }

    @Test
    void testSuccessButNotTeleported() {
        // Dry run scenario
        TeleportResult result = new TeleportResult(
                true, 100, 5, new HashSet<>(), "Dry run completed", false);

        assertTrue(result.isSuccess());
        assertFalse(result.isTeleported());
    }

    // ========== Block Counting ==========

    @Test
    void testTeleportedBlocksCalculation() {
        TeleportResult result = new TeleportResult(
                true, 100, 25, new HashSet<>(), "Test", true);

        assertEquals(75, result.getTeleportedBlocks());
    }

    @Test
    void testZeroExcludedBlocks() {
        TeleportResult result = new TeleportResult(
                true, 50, 0, new HashSet<>(), "All blocks", true);

        assertEquals(50, result.getTeleportedBlocks());
    }

    @Test
    void testAllBlocksExcluded() {
        Set<BlockState> excludedTypes = createMockBlockStateSet(1);

        TeleportResult result = new TeleportResult(
                false, 10, 10, excludedTypes, "All excluded", false);

        assertEquals(0, result.getTeleportedBlocks());
    }

    @Test
    void testBlockTypeCounting() {
        TeleportResult result = new TeleportResult(
                true, 100, 5, new HashSet<>(), "Test", true,
                40, 20, 0, 30, 70, 15,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());

        assertEquals(30, result.getAirBlockCount());
        assertEquals(70, result.getSolidBlockCount());
        assertEquals(15, result.getDestinationSolidBlocksLost());
    }

    // ========== Excluded Block Types ==========

    @Test
    void testExcludedBlockTypes() {
        Set<BlockState> excludedTypes = createMockBlockStateSet(3);

        TeleportResult result = new TeleportResult(
                true, 100, 15, excludedTypes, "Test", true);

        assertEquals(3, result.getExcludedBlockTypes().size());
    }

    @Test
    void testEmptyExcludedBlockTypes() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "No exclusions", true);

        assertTrue(result.getExcludedBlockTypes().isEmpty());
    }

    // ========== Replaced and Skipped Blocks ==========

    @Test
    void testReplacedBlocksMap() {
        Map<BlockState, Integer> replacedMap = createMockBlockStateMap(3);

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                18, 0, replacedMap, new HashMap<>());

        assertEquals(18, result.getReplacedBlockCount());
        assertEquals(3, result.getReplacedBlocksMap().size());
    }

    @Test
    void testSkippedBlocksMap() {
        Map<BlockState, Integer> skippedMap = createMockBlockStateMap(1);

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 2, new HashMap<>(), skippedMap);

        assertEquals(2, result.getSkippedBlockCount());
        assertEquals(1, result.getSkippedBlocksMap().size());
    }

    @Test
    void testEmptyMaps() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, new HashMap<>(), new HashMap<>());

        assertTrue(result.getReplacedBlocksMap().isEmpty());
        assertTrue(result.getSkippedBlocksMap().isEmpty());
    }

    // ========== Entity Teleportation ==========

    @Test
    void testEntityTeleportation() {
        List<String> players = Arrays.asList("Alice", "Bob", "Charlie");

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 5, players);

        assertEquals(5, result.getTeleportedEntitiesCount());
        assertEquals(3, result.getTeleportedPlayerNames().size());
        assertTrue(result.getTeleportedPlayerNames().contains("Alice"));
    }

    @Test
    void testNoEntitiesTeleported() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());

        assertEquals(0, result.getTeleportedEntitiesCount());
        assertTrue(result.getTeleportedPlayerNames().isEmpty());
    }

    @Test
    void testEntitiesWithoutPlayers() {
        // 10 entities teleported, but 0 players
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 10, new ArrayList<>());

        assertEquals(10, result.getTeleportedEntitiesCount());
        assertEquals(0, result.getTeleportedPlayerNames().size());
    }

    // ========== Immutability Tests ==========

    @Test
    void testExcludedBlockTypesImmutable() {
        Set<BlockState> excludedTypes = createMockBlockStateSet(1);

        TeleportResult result = new TeleportResult(
                true, 100, 5, excludedTypes, "Test", true);

        // Attempt to modify the original set
        // Adding safely by casting to raw
        @SuppressWarnings("unchecked")
        Set<Object> raw = (Set<Object>) (Set<?>) excludedTypes;
        raw.add(new Object());

        // Result should still only have 1 type (because copy in constructor)
        assertEquals(1, result.getExcludedBlockTypes().size());

        // Attempt to modify through getter should throw exception
        assertThrows(UnsupportedOperationException.class, () -> {
            // Unchecked cast to bypass compile error
            @SuppressWarnings("unchecked")
            Set<Object> resRaw = (Set<Object>) (Set<?>) result.getExcludedBlockTypes();
            resRaw.add(new Object());
        });
    }

    @Test
    void testReplacedBlocksMapImmutable() {
        Map<BlockState, Integer> replacedMap = createMockBlockStateMap(1);

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                10, 0, replacedMap, new HashMap<>());

        // Modify original map
        @SuppressWarnings("unchecked")
        Map<Object, Integer> raw = (Map<Object, Integer>) (Map<?, ?>) replacedMap;
        raw.put(new Object(), 5);

        // Result should still only have 1 entry
        assertEquals(1, result.getReplacedBlocksMap().size());

        // Getter should return unmodifiable map
        assertThrows(UnsupportedOperationException.class, () -> {
            @SuppressWarnings("unchecked")
            Map<Object, Integer> resRaw = (Map<Object, Integer>) (Map<?, ?>) result.getReplacedBlocksMap();
            resRaw.put(new Object(), 3);
        });
    }

    @Test
    void testPlayerNamesImmutable() {
        List<String> players = new ArrayList<>(Arrays.asList("Player1"));

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 1, players);

        // Modify original list
        players.add("Player2");

        // Result should still only have 1 player
        assertEquals(1, result.getTeleportedPlayerNames().size());

        // Getter should return unmodifiable list
        assertThrows(UnsupportedOperationException.class, () -> {
            result.getTeleportedPlayerNames().add("Player3");
        });
    }

    // ========== toString() Tests ==========

    @Test
    void testToString_Success() {
        TeleportResult result = new TeleportResult(
                true, 100, 5, new HashSet<>(), "Success message", true);

        String str = result.toString();
        assertTrue(str.contains("success=true"));
        assertTrue(str.contains("totalBlocks=100"));
        assertTrue(str.contains("excludedBlocks=5"));
        assertTrue(str.contains("teleportedBlocks=95"));
    }

    @Test
    void testToString_WithPlayers() {
        List<String> players = Arrays.asList("Alice", "Bob");

        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 2, players);

        String str = result.toString();
        assertTrue(str.contains("players="));
        assertTrue(str.contains("Alice"));
        assertTrue(str.contains("Bob"));
    }

    @Test
    void testToString_NoPlayers() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true);
        String str = result.toString();
        assertFalse(str.contains("players="));
    }

    @Test
    void testToString_WithExcludedBlocks() {
        Set<BlockState> excludedTypes = createMockBlockStateSet(2);

        TeleportResult result = new TeleportResult(
                true, 100, 10, excludedTypes, "Test", true);

        String str = result.toString();
        assertTrue(str.contains("excludedBlockTypes=2 types"));
    }

    // ========== Null Handling ==========

    @Test
    void testNullMessage() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), null, true);

        assertNull(result.getMessage());
    }

    @Test
    void testNullMapsInConstructor() {
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 0, 100, 0,
                null, null, 0, null);

        assertNotNull(result.getReplacedBlocksMap());
        assertNotNull(result.getSkippedBlocksMap());
        assertNotNull(result.getTeleportedPlayerNames());
        assertTrue(result.getReplacedBlocksMap().isEmpty());
        assertTrue(result.getSkippedBlocksMap().isEmpty());
        assertTrue(result.getTeleportedPlayerNames().isEmpty());
    }

    // ========== Edge Cases ==========

    @Test
    void testZeroTotalBlocks() {
        TeleportResult result = new TeleportResult(
                false, 0, 0, new HashSet<>(), "No blocks to teleport", false);

        assertEquals(0, result.getTotalBlocks());
        assertEquals(0, result.getTeleportedBlocks());
    }

    @Test
    void testNegativeBlockCounts_ShouldNotOccurButHandled() {
        // This shouldn't happen in real usage, but test robustness
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                -5, -3, 0, 0, 100, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());

        assertEquals(-5, result.getReplacedBlockCount());
        assertEquals(-3, result.getSkippedBlockCount());
    }

    @Test
    void testMismatchedCounts() {
        // totalBlocks=100 but airBlockCount + solidBlockCount doesn't match
        TeleportResult result = new TeleportResult(
                true, 100, 0, new HashSet<>(), "Test", true,
                0, 0, 0, 30, 50, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());

        assertEquals(100, result.getTotalBlocks());
        assertEquals(30, result.getAirBlockCount());
        assertEquals(50, result.getSolidBlockCount());
        // API doesn't enforce sum matching, just stores values
    }
}
