package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class StructureTeleporter {

    // Class for storing block information
    public static class BlockData {
        public BlockPos relativePos; // Position relative to starting point
        public BlockState blockState; // Block state (block type + its data)
        public CompoundTag nbt; // BlockEntity data (chests, etc.)

        public BlockData(BlockPos relativePos, BlockState blockState, CompoundTag nbt) {
            this.relativePos = relativePos;
            this.blockState = blockState;
            this.nbt = nbt;
        }
    }

    // Method for copying structure to memory

    public static List<BlockData> copyStructure(Selection selection) {
        if (!selection.isComplete()) {
            return null;
        }

        List<BlockData> blocks = new ArrayList<>();

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        Level world = selection.getWorld();

        // Iterate through all blocks in the selected region
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    // Get NBT data if available (for chests, etc.)
                    CompoundTag nbt = null;
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        nbt = blockEntity.saveWithFullMetadata();
                        // Remove coordinates from NBT to avoid conflicts during pasting
                        nbt.remove("x");
                        nbt.remove("y");
                        nbt.remove("z");
                    }

                    // Calculate relative position (relative to minimum point)
                    BlockPos relativePos = pos.subtract(min);

                    // Save block
                    blocks.add(new BlockData(relativePos, state, nbt));
                }
            }
        }

        TeleportAPI.LOGGER.info("Blocks copied: " + blocks.size());
        return blocks;
    }

    // Method for pasting structure to a new location
    @SuppressWarnings("null")
    public static void pasteStructure(List<BlockData> blocks, BlockPos targetPos, Level world) {
        if (blocks == null || blocks.isEmpty()) {
            TeleportAPI.LOGGER.warn("No blocks to paste!");
            return;
        }

        // Paste each block
        for (BlockData blockData : blocks) {
            // Calculate absolute position (target position + relative)
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            // Set block
            world.setBlock(absolutePos, blockData.blockState, 3);

            // Restore NBT data (if available)
            if (blockData.nbt != null) {

                BlockEntity blockEntity = world.getBlockEntity(absolutePos);
                if (blockEntity != null) {
                    // Create NBT copy and update coordinates
                    CompoundTag tag = blockData.nbt.copy();
                    tag.putInt("x", absolutePos.getX());
                    tag.putInt("y", absolutePos.getY());
                    tag.putInt("z", absolutePos.getZ());

                    blockEntity.load(tag);
                }
            }
        }

        TeleportAPI.LOGGER.info("Blocks pasted: " + blocks.size() + " at position " + targetPos);
    }

    // Method for teleporting structure (remove from old location and paste in
    // new one)
    public static void teleportStructure(Selection selection, BlockPos targetPos) {
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("Selection not complete!");
            return;
        }

        Level world = selection.getWorld();

        // 1. Copy structure
        List<BlockData> blocks = copyStructure(selection);

        if (blocks == null || blocks.isEmpty()) {
            return;
        }

        // 2. Remove blocks from old location (replace with air)
        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    // Set air (remove block)
                    world.removeBlock(pos, false);
                }
            }
        }

        // 3. Paste in new location
        pasteStructure(blocks, targetPos, world);

        TeleportAPI.LOGGER.info("Structure teleported to " + targetPos);
    }

    /**
     * Teleport structure defined by two corners.
     */
    public static void teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos) {
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromCorners(p1, p2);
        teleportStructure(selection, targetPos);
    }

    /**
     * Teleport structure defined by 6 points.
     */
    public static void teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos) {
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromSixPoints(points);
        teleportStructure(selection, targetPos);
    }
}
