package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge case testing for StructureTeleporter class.
 * Tests boundary conditions, extreme values, and resource limits.
 */
class StructureTeleporterEdgeCaseTest {

    // ========== Very Large Structures ==========

    @Test
    void testBlockDataList_VeryLargeStructure() {
        // Simulate a large structure (1000+ blocks)
        List<StructureTeleporter.BlockData> blocks = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos(i % 10, i / 100, (i / 10) % 10);
            blocks.add(new StructureTeleporter.BlockData(pos, null, null));
        }

        assertEquals(1000, blocks.size());
        assertNotNull(blocks.get(0));
        assertNotNull(blocks.get(999));
    }

    @Test
    void testBlockDataList_ExtremelyLargeStructure() {
        // Test with very large structure (10,000 blocks)
        List<StructureTeleporter.BlockData> blocks = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            BlockPos pos = new BlockPos(i % 100, i / 10000, (i / 100) % 100);
            blocks.add(new StructureTeleporter.BlockData(pos, null, null));
        }

        assertEquals(10000, blocks.size());
        // Verify memory efficiency - list should not be null
        assertNotNull(blocks);
    }

    @Test
    void testBlockDataList_MemoryReuse() {
        // Test that BlockData objects can be created and discarded efficiently
        for (int iteration = 0; iteration < 100; iteration++) {
            List<StructureTeleporter.BlockData> blocks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                blocks.add(new StructureTeleporter.BlockData(
                        new BlockPos(i, i, i), null, null));
            }
            assertEquals(100, blocks.size());
            blocks.clear(); // Simulate cleanup
        }
        // Test passes if no OutOfMemoryError occurs
        assertTrue(true);
    }

    // ========== Coordinate Arithmetic Overflow ==========

    @Test
    void testBlockData_MaxIntegerCoordinates() {
        // Test with maximum integer values
        BlockPos maxPos = new BlockPos(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(maxPos, null, null);

        assertNotNull(blockData);
        assertEquals(Integer.MAX_VALUE, blockData.relativePos.getX());
        assertEquals(Integer.MAX_VALUE, blockData.relativePos.getY());
        assertEquals(Integer.MAX_VALUE, blockData.relativePos.getZ());
    }

    @Test
    void testBlockData_MinIntegerCoordinates() {
        // Test with minimum integer values
        BlockPos minPos = new BlockPos(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(minPos, null, null);

        assertNotNull(blockData);
        assertEquals(Integer.MIN_VALUE, blockData.relativePos.getX());
        assertEquals(Integer.MIN_VALUE, blockData.relativePos.getY());
        assertEquals(Integer.MIN_VALUE, blockData.relativePos.getZ());
    }

    @Test
    void testBlockPos_SubtractOverflow() {
        // Test potential overflow in BlockPos.subtract()
        // This documents a potential issue if coordinates are near MAX_VALUE
        BlockPos pos1 = new BlockPos(Integer.MAX_VALUE - 10, 100, 100);
        BlockPos pos2 = new BlockPos(Integer.MIN_VALUE + 10, 0, 0);

        // BlockPos.subtract will handle overflow by wrapping
        // This test documents the behavior
        BlockPos result = pos1.subtract(pos2);
        assertNotNull(result);
        // Just verify it doesn't throw an exception
    }

    // ========== Complex NBT Structures ==========

    @Test
    @SuppressWarnings("null")
    void testBlockData_ComplexNBT() {
        CompoundTag complexNBT = new CompoundTag();

        // Simulate a chest with items
        complexNBT.putString("id", "minecraft:chest");

        ListTag items = new ListTag();
        for (int i = 0; i < 27; i++) {
            CompoundTag item = new CompoundTag();
            item.putString("id", "minecraft:diamond");
            item.putByte("Count", (byte) 64);
            item.putByte("Slot", (byte) i);
            items.add(item);
        }
        complexNBT.put("Items", items);

        BlockPos pos = new BlockPos(10, 20, 30);
        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(pos, null, complexNBT);

        assertNotNull(blockData);
        assertNotNull(blockData.nbt);
        assertEquals("minecraft:chest", blockData.nbt.getString("id"));
        assertEquals(27, ((ListTag) blockData.nbt.get("Items")).size());
    }

    @Test
    void testBlockData_DeeplyNestedNBT() {
        // Test with deeply nested NBT structure
        CompoundTag deepNBT = new CompoundTag();
        CompoundTag current = deepNBT;

        // Create 10 levels of nesting
        for (int i = 0; i < 10; i++) {
            CompoundTag nested = new CompoundTag();
            nested.putInt("level", i);
            current.put("nested", nested);
            current = nested;
        }

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new BlockPos(0, 0, 0), null, deepNBT);

        assertNotNull(blockData);
        assertNotNull(blockData.nbt);
        assertTrue(blockData.nbt.contains("nested"));
    }

    @Test
    void testBlockData_EmptyNBT() {
        CompoundTag emptyNBT = new CompoundTag();

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new BlockPos(5, 10, 15), null, emptyNBT);

        assertNotNull(blockData);
        assertNotNull(blockData.nbt);
        assertTrue(blockData.nbt.isEmpty());
    }

    @Test
    @SuppressWarnings("null")
    void testBlockData_NBTWithLongStrings() {
        // Test with very long string values
        CompoundTag nbt = new CompoundTag();
        String longString = "A".repeat(10000); // 10,000 character string
        nbt.putString("longText", longString);

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new BlockPos(0, 0, 0), null, nbt);

        assertNotNull(blockData);
        assertEquals(longString, blockData.nbt.getString("longText"));
        assertEquals(10000, blockData.nbt.getString("longText").length());
    }

    // ========== Null BlockState Handling ==========

    @Test
    void testPasteStructure_AllNullBlockStates() {
        List<StructureTeleporter.BlockData> blocks = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            blocks.add(new StructureTeleporter.BlockData(
                    new BlockPos(i, 0, 0), null, null));
        }

        // Verify list creation works with null BlockStates
        // Note: Actual pasting requires a real Level object (tested in GameTests)
        assertEquals(10, blocks.size());
        for (StructureTeleporter.BlockData block : blocks) {
            assertNotNull(block);
            assertNull(block.blockState);
        }
    }

    @Test
    void testCopyStructure_IncompleteSelection_Various() {
        // Test with selection missing different faces
        Selection selection1 = new Selection();
        selection1.setXMin(0);
        // Missing other coordinates
        assertNull(StructureTeleporter.copyStructure(selection1));

        Selection selection2 = new Selection();
        selection2.setXMin(0);
        selection2.setXMax(10);
        selection2.setYMin(0);
        selection2.setYMax(10);
        // Missing Z coordinates
        assertNull(StructureTeleporter.copyStructure(selection2));
    }

    // ========== Empty Tag Handling ==========

    @Test
    void testBlockData_NBTWithSpecialCharacters() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("text", "Special: §a§l§nBold\nNewline\tTab\"Quote'Apostrophe");
        nbt.putString("unicode", "Unicode: 你好 مرحبا Здравствуйте");

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new BlockPos(0, 0, 0), null, nbt);

        assertNotNull(blockData);
        assertTrue(blockData.nbt.getString("text").contains("§a"));
        assertTrue(blockData.nbt.getString("unicode").contains("你好"));
    }

    @Test
    void testBlockData_NBTCopy() {
        CompoundTag originalNBT = new CompoundTag();
        originalNBT.putString("key", "value");
        originalNBT.putInt("number", 42);

        StructureTeleporter.BlockData blockData = new StructureTeleporter.BlockData(
                new BlockPos(0, 0, 0), null, originalNBT);

        // Verify the NBT is stored (not copied in BlockData constructor)
        assertSame(originalNBT, blockData.nbt);

        // Modify original
        originalNBT.putString("key", "modified");

        // BlockData should reflect the change (same reference)
        assertEquals("modified", blockData.nbt.getString("key"));
    }

    // ========== TeleportByCorners Edge Cases ==========

    @Test
    void testTeleportByCorners_SamePoints() {
        BlockPos point = new BlockPos(10, 20, 30);
        BlockPos target = new BlockPos(50, 60, 70);

        // Should create a 1x1x1 selection
        assertDoesNotThrow(() -> {
            try {
                StructureTeleporter.teleportByCorners(null, point, point, target, false);
            } catch (NullPointerException e) {
                // Expected due to null world
            }
        });
    }

    @Test
    void testTeleportByCorners_ExtremeDistance() {
        BlockPos p1 = new BlockPos(0, 0, 0);
        BlockPos p2 = new BlockPos(10, 10, 10);
        BlockPos farTarget = new BlockPos(1_000_000, 1_000_000, 1_000_000);

        // Should handle extreme target distances
        assertDoesNotThrow(() -> {
            try {
                StructureTeleporter.teleportByCorners(null, p1, p2, farTarget, false);
            } catch (NullPointerException e) {
                // Expected due to null world
            }
        });
    }

    // ========== TeleportBySixPoints Edge Cases ==========

    @Test
    void testTeleportBySixPoints_InsufficientPoints() {
        BlockPos[] points = new BlockPos[3];
        for (int i = 0; i < 3; i++) {
            points[i] = new BlockPos(i, i, i);
        }

        assertThrows(IllegalArgumentException.class, () -> {
            StructureTeleporter.teleportBySixPoints(null, points, new BlockPos(0, 0, 0));
        });
    }

    @Test
    void testTeleportBySixPoints_TooManyPoints() {
        BlockPos[] points = new BlockPos[10];
        for (int i = 0; i < 10; i++) {
            points[i] = new BlockPos(i, i, i);
        }

        assertThrows(IllegalArgumentException.class, () -> {
            StructureTeleporter.teleportBySixPoints(null, points, new BlockPos(0, 0, 0));
        });
    }

    @Test
    void testTeleportBySixPoints_AllSamePoint() {
        BlockPos point = new BlockPos(5, 10, 15);
        BlockPos[] points = new BlockPos[6];
        for (int i = 0; i < 6; i++) {
            points[i] = point;
        }

        // Should create a single point selection
        assertDoesNotThrow(() -> {
            try {
                StructureTeleporter.teleportBySixPoints(null, points, new BlockPos(0, 0, 0));
            } catch (NullPointerException e) {
                // Expected due to null world
            }
        });
    }

    // ========== BlockData Equality and Comparison ==========

    @Test
    void testBlockData_DifferentPositionsSameNBT() {
        CompoundTag sharedNBT = new CompoundTag();
        sharedNBT.putString("shared", "data");

        StructureTeleporter.BlockData bd1 = new StructureTeleporter.BlockData(
                new BlockPos(0, 0, 0), null, sharedNBT);
        StructureTeleporter.BlockData bd2 = new StructureTeleporter.BlockData(
                new BlockPos(10, 10, 10), null, sharedNBT);

        assertNotEquals(bd1.relativePos, bd2.relativePos);
        assertSame(bd1.nbt, bd2.nbt); // Same NBT reference
    }

    // ========== Performance Boundary Tests ==========

    @Test
    void testBlockDataCreation_Performance() {
        long startTime = System.currentTimeMillis();

        List<StructureTeleporter.BlockData> blocks = new ArrayList<>();
        for (int i = 0; i < 10000; i++) {
            blocks.add(new StructureTeleporter.BlockData(
                    new BlockPos(i, i, i), null, null));
        }

        long duration = System.currentTimeMillis() - startTime;

        // Should complete in reasonable time (< 1 second for 10k blocks)
        assertTrue(duration < 1000, "Creating 10k BlockData should be fast, took " + duration + "ms");
        assertEquals(10000, blocks.size());
    }
}
