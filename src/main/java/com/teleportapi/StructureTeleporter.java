package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * Get default list of blocks that should not be teleported.
     * Includes: Bedrock, End Portal Frame, End Portal, End Gateway, Reinforced
     * Deepslate
     */
    public static List<BlockState> getDefaultExcludedBlocks() {
        List<BlockState> excludedBlocks = new ArrayList<>();
        excludedBlocks.add(Blocks.BEDROCK.defaultBlockState());
        excludedBlocks.add(Blocks.END_PORTAL_FRAME.defaultBlockState());
        excludedBlocks.add(Blocks.END_PORTAL.defaultBlockState());
        excludedBlocks.add(Blocks.END_GATEWAY.defaultBlockState());
        excludedBlocks.add(Blocks.REINFORCED_DEEPSLATE.defaultBlockState());
        return excludedBlocks;
    }

    /**
     * Check if a block state should be excluded from teleportation.
     * 
     * @param state           Block state to check
     * @param excludedBlocks  List of excluded block states
     * @param checkExclusions Whether to actually check exclusions (if false, always
     *                        returns false)
     */
    private static boolean isExcluded(BlockState state, List<BlockState> excludedBlocks, boolean checkExclusions) {
        if (!checkExclusions || excludedBlocks == null || excludedBlocks.isEmpty()) {
            return false;
        }

        for (BlockState excluded : excludedBlocks) {
            if (state.getBlock() == excluded.getBlock()) {
                return true;
            }
        }
        return false;
    }

    // Method for copying structure to memory

    public static List<BlockData> copyStructure(Selection selection) {
        return copyStructure(selection, null, true);
    }

    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks) {
        return copyStructure(selection, excludedBlocks, true);
    }

    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks,
            boolean checkExclusions) {
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

                    // Skip excluded blocks
                    if (isExcluded(state, excludedBlocks, checkExclusions)) {
                        continue;
                    }

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
        pasteStructure(blocks, targetPos, world, null);
    }

    @SuppressWarnings("null")
    public static void pasteStructure(List<BlockData> blocks, BlockPos targetPos, Level world,
            List<BlockState> excludedBlocks) {
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
        teleportStructure(selection, selection.getWorld(), targetPos, null, true, true);
    }

    public static TeleportResult teleportStructure(Selection selection, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport) {
        return teleportStructure(selection, selection.getWorld(), targetPos, excludedBlocks, shouldTeleport, true);
    }

    public static TeleportResult teleportStructure(Selection selection, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportStructure(selection, selection.getWorld(), targetPos, excludedBlocks, shouldTeleport,
                checkExclusions);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("Selection not complete!");
            return new TeleportResult(false, 0, 0, new HashSet<>(),
                    "Selection not complete", false);
        }

        Level sourceWorld = selection.getWorld();
        if (targetLevel == null) {
            targetLevel = sourceWorld;
        }
        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        // Count total blocks in selection
        int totalBlocks = 0;
        int excludedCount = 0;
        Set<BlockState> excludedTypes = new HashSet<>();

        // First pass: count blocks and excluded blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = sourceWorld.getBlockState(pos);
                    totalBlocks++;

                    if (isExcluded(state, excludedBlocks, checkExclusions)) {
                        excludedCount++;
                        excludedTypes.add(state);
                    }
                }
            }
        }

        // Build message about excluded blocks
        StringBuilder message = new StringBuilder();
        if (excludedCount > 0) {
            message.append("Found ").append(excludedCount).append(" excluded block(s) of ")
                    .append(excludedTypes.size()).append(" type(s). ");
        }

        // If shouldTeleport is false, just return the scan results
        if (!shouldTeleport) {
            message.append("Teleportation was not performed (shouldTeleport=false).");
            TeleportAPI.LOGGER.info(message.toString());
            return new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                    message.toString(), false);
        }

        // 1. Copy structure (this will skip excluded blocks)
        List<BlockData> blocks = copyStructure(selection, excludedBlocks, checkExclusions);

        if (blocks == null || blocks.isEmpty()) {
            return new TeleportResult(false, totalBlocks, excludedCount, excludedTypes,
                    "No blocks to teleport after exclusions", false);
        }

        // 2. Remove blocks from old location (but keep excluded blocks)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = sourceWorld.getBlockState(pos);

                    // Don't remove excluded blocks
                    if (!isExcluded(state, excludedBlocks, checkExclusions)) {
                        sourceWorld.removeBlock(pos, false);
                    }
                }
            }
        }

        // 3. Paste in new location
        pasteStructure(blocks, targetPos, targetLevel, excludedBlocks);

        message.append("Structure teleported successfully. ")
                .append(blocks.size()).append(" block(s) moved to ").append(targetPos).append(".");

        TeleportAPI.LOGGER.info(message.toString());

        return new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                message.toString(), true);
    }

    /**
     * Teleport structure defined by two corners.
     */
    public static void teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos) {
        teleportByCorners(world, p1, p2, targetPos, true);
    }

    public static void teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos,
            boolean checkExclusions) {
        if (checkExclusions) {
            // get list blocks for exclusions
            List<BlockState> excludedBlocks = getDefaultExcludedBlocks();
            teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, true, true);
        } else {
            teleportByCorners(world, p1, p2, world, targetPos, null, true, false);
        }
    }

    public static void teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos) {
        teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, true);
    }

    public static void teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, boolean checkExclusions) {
        if (checkExclusions) {
            // get list blocks for exclusions
            List<BlockState> excludedBlocks = getDefaultExcludedBlocks();
            teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, true, true);
        } else {
            teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, null, true, false);
        }
    }

    public static TeleportResult teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions);
    }

    public static TeleportResult teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromCorners(p1, p2);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions);
    }

    /**
     * Teleport structure defined by 6 points.
     */
    public static void teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos) {
        teleportBySixPoints(world, points, world, targetPos, null, true, true);
    }

    public static void teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos) {
        teleportBySixPoints(sourceLevel, points, targetLevel, targetPos, null, true, true);
    }

    public static TeleportResult teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport) {
        return teleportBySixPoints(world, points, world, targetPos, excludedBlocks, shouldTeleport, true);
    }

    public static TeleportResult teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportBySixPoints(world, points, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions);
    }

    public static TeleportResult teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromSixPoints(points);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions);
    }
}
