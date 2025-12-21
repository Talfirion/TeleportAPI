package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StructureTeleporter {

    public enum PasteMode {
        FORCE_REPLACE, // Replace all blocks at destination (default)
        PRESERVE_LIST, // Replace all except blocks in the preserved list
        PRESERVE_EXISTING // Do not replace any existing non-air blocks
    }

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

    /**
     * Sanitizes a block state by resetting "volatile" properties like POWERED or
     * TRIGGERED.
     * This prevents redstone buttons or pressure plates from being stuck in the ON
     * state
     * after teleportation.
     */
    private static BlockState sanitizeBlockState(BlockState state) {
        if (state.hasProperty(BlockStateProperties.POWERED)) {
            state = state.setValue(BlockStateProperties.POWERED, false);
        }
        if (state.hasProperty(BlockStateProperties.TRIGGERED)) {
            state = state.setValue(BlockStateProperties.TRIGGERED, false);
        }
        if (state.hasProperty(BlockStateProperties.LIT)) {
            // Usually we want LIT to stay (like lanterns), but for redstone torches/lamps
            // we might want to reset them to default so they re-calculate.
            // However, resetting LIT for everything might turn off all lanterns.
            // Let's be selective if possible, or just skip LIT for now and see.
            // state = state.setValue(BlockStateProperties.LIT, false);
        }
        return state;
    }

    /**
     * Check if a block at the target should be replaced by the incoming block based
     * on the mode.
     */
    @SuppressWarnings("null")
    private static boolean shouldReplace(BlockState existing, BlockState incoming, PasteMode mode,
            List<BlockState> preservedBlocks) {
        // If incoming is air, usually we might still want to replace if we are moving
        // (clearing space),
        // but typically structure paste might want to avoid pasting air over blocks
        // unless intentional.
        // However, based on "teleport", we are moving the *source* to the *target*.
        // If source is air, we generally don't copy air blocks in copyStructure unless
        // specifically handled?
        // Wait, copyStructure iterates all blocks.
        // Let's assume we want to apply the mode logic.

        if (mode == PasteMode.FORCE_REPLACE) {
            return true;
        }

        if (mode == PasteMode.PRESERVE_EXISTING) {
            // Don't replace if existing is not air
            return existing.isAir();
        }

        if (mode == PasteMode.PRESERVE_LIST) {
            if (preservedBlocks != null) {
                for (BlockState preserved : preservedBlocks) {
                    if (existing.is(preserved.getBlock())) {
                        return false;
                    }
                }
            }
            return true;
        }

        return true;
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
        return copyStructure(selection, excludedBlocks, checkExclusions, true);
    }

    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks,
            boolean checkExclusions, boolean includeAir) {
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

                    // Skip air if not included
                    if (!includeAir && state.isAir()) {
                        continue;
                    }

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

                    // Save block (sanitized)
                    blocks.add(new BlockData(relativePos, sanitizeBlockState(state), nbt));
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
        pasteStructure(blocks, targetPos, world, PasteMode.FORCE_REPLACE, null);
    }

    @SuppressWarnings("null")
    public static void pasteStructure(List<BlockData> blocks, BlockPos targetPos, Level world,
            PasteMode mode, List<BlockState> preservedBlocks) {
        if (blocks == null || blocks.isEmpty()) {
            TeleportAPI.LOGGER.warn("No blocks to paste!");
            return;
        }

        // Pass 1: Set blocks with minimal updates (flag 2 = UPDATE_CLIENTS)
        // This ensures all blocks are in place before neighbor updates are triggered.
        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);
            BlockState existingState = world.getBlockState(absolutePos);

            if (shouldReplace(existingState, blockData.blockState, mode, preservedBlocks)) {
                // Set block without neighbor updates (flag 2 | 16)
                // 16 is UPDATE_KNOWN_SHAPE, used to avoid some side effects.
                world.setBlock(absolutePos, blockData.blockState, 18);
            }
        }

        // Pass 2: Restore NBT and Block Entity data
        for (BlockData blockData : blocks) {
            if (blockData.nbt != null) {
                BlockPos absolutePos = targetPos.offset(blockData.relativePos);
                BlockEntity blockEntity = world.getBlockEntity(absolutePos);
                if (blockEntity != null) {
                    CompoundTag tag = blockData.nbt.copy();
                    tag.putInt("x", absolutePos.getX());
                    tag.putInt("y", absolutePos.getY());
                    tag.putInt("z", absolutePos.getZ());
                    blockEntity.load(tag);
                }
            }
        }

        // Pass 3: Trigger neighbor updates to ensure logic (like grass support or
        // redstone) runs correctly
        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);
            BlockState state = world.getBlockState(absolutePos);

            // Notify neighbors and update block logic (flag 3 = UPDATE_CLIENTS |
            // UPDATE_NEIGHBORS)
            world.sendBlockUpdated(absolutePos, state, state, 3);
            world.updateNeighborsAt(absolutePos, state.getBlock());
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
                checkExclusions, PasteMode.FORCE_REPLACE, null, true, true, true);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport,
            boolean checkExclusions) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                PasteMode.FORCE_REPLACE, null, true, true, true);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, true, true, true);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, true, true);
    }

    @SuppressWarnings("null")
    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir,
            boolean teleportPlayers, boolean teleportEntities) {
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
        int replacedCount = 0;
        int skippedCount = 0;
        Set<BlockState> excludedTypes = new HashSet<>();
        Map<BlockState, Integer> replacedBlocksMap = new HashMap<>();
        Map<BlockState, Integer> skippedBlocksMap = new HashMap<>();

        // Helper to format block counts
        // (Defined inline or as private method? Private method is cleaner but I can't
        // easily add one at bottom without context.
        // Let's just append logic at the end.)

        // First pass: count blocks and excluded blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = sourceWorld.getBlockState(pos);

                    if (state.isAir()) {
                        continue;
                    }
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

        // Calculate potential replacements
        // Note: Re-calculating this during the copy phase or doing a pre-scan of target
        // area?
        // Since we need to report it before teleporting if shouldTeleport is false
        // (feedback).

        // For the purpose of feedback, we need to iterate the target area corresponding
        // to source blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos srcPos = new BlockPos(x, y, z);
                    BlockState srcState = sourceWorld.getBlockState(srcPos);

                    if (srcState.isAir() && !includeAir) {
                        continue;
                    }

                    if (!isExcluded(srcState, excludedBlocks, checkExclusions)) {
                        // Check target
                        BlockPos relPos = srcPos.subtract(min);
                        @SuppressWarnings("null")
                        BlockPos dstPos = targetPos.offset(relPos);
                        @SuppressWarnings("null")
                        BlockState dstState = targetLevel.getBlockState(dstPos);

                        if (shouldReplace(dstState, srcState, pasteMode, preservedBlocks)) {
                            replacedCount++;
                            replacedBlocksMap.merge(dstState, 1, (a, b) -> a + b);
                        } else {
                            skippedCount++;
                            skippedBlocksMap.merge(srcState, 1, (a, b) -> a + b);
                        }
                    }
                }
            }
        }

        // Detect entities in the selection area
        AABB selectionBox = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1,
                max.getZ() + 1);
        List<Entity> entities = sourceWorld.getEntitiesOfClass(Entity.class, selectionBox);
        List<EntityData> entitiesToTeleport = new ArrayList<>();

        for (Entity entity : entities) {
            // Skip players if teleportPlayers is false, skip other entities if
            // teleportEntities is false
            boolean isPlayer = entity instanceof Player;
            if (isPlayer && !teleportPlayers)
                continue;
            if (!isPlayer && !teleportEntities)
                continue;

            // Store player name specifically for reporting
            String playerName = null;
            if (entity instanceof Player player) {
                playerName = player.getName().getString();
            }

            // Calculate relative offset from selection min
            double relX = entity.getX() - min.getX();
            double relY = entity.getY() - min.getY();
            double relZ = entity.getZ() - min.getZ();

            entitiesToTeleport.add(new EntityData(entity, relX, relY, relZ, playerName));
        }

        // If shouldTeleport is false, just return the scan results
        if (!shouldTeleport) {
            message.append("Teleportation was not performed (shouldTeleport=false).");
            TeleportAPI.LOGGER.info(message.toString());
            return new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                    message.toString(), false, replacedCount, skippedCount, replacedBlocksMap, skippedBlocksMap,
                    entitiesToTeleport.size(), entitiesToTeleport.stream()
                            .filter(e -> e.playerName != null)
                            .map(e -> e.playerName)
                            .collect(Collectors.toList()));
        }

        // 1. Copy structure (this will skip excluded blocks)
        List<BlockData> blocks = copyStructure(selection, excludedBlocks, checkExclusions, includeAir);

        if (blocks == null || blocks.isEmpty()) {
            return new TeleportResult(false, totalBlocks, excludedCount, excludedTypes,
                    "No blocks to teleport after exclusions", false, 0, 0);
        }

        // 2. Remove blocks from old location (but keep excluded blocks)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = sourceWorld.getBlockState(pos);

                    // Remove block with neighbor updates (flag true)
                    if (!isExcluded(state, excludedBlocks, checkExclusions)) {
                        sourceWorld.removeBlock(pos, true);
                    }
                }
            }
        }

        // 3. Paste in new location
        pasteStructure(blocks, targetPos, targetLevel, pasteMode, preservedBlocks);

        // 4. Teleport entities
        List<String> teleportedPlayers = new ArrayList<>();
        for (EntityData info : entitiesToTeleport) {
            double targetX = targetPos.getX() + info.relX;
            double targetY = targetPos.getY() + info.relY;
            double targetZ = targetPos.getZ() + info.relZ;

            if (targetLevel != sourceWorld && info.entity instanceof ServerPlayer serverPlayer) {
                // Cross-dimensional player teleportation
                if (targetLevel instanceof ServerLevel serverLevel) {
                    serverPlayer.teleportTo(serverLevel, targetX, targetY, targetZ, serverPlayer.getYRot(),
                            serverPlayer.getXRot());
                }
            } else {
                // Regular teleportation or dimension change for non-players
                info.entity.teleportTo(targetX, targetY, targetZ);
                if (targetLevel != sourceWorld) {
                    info.entity.changeDimension((ServerLevel) targetLevel);
                }
            }

            if (info.playerName != null) {
                teleportedPlayers.add(info.playerName);
            }
        }

        message.append("Structure teleported successfully. ")
                .append(totalBlocks - excludedCount).append(" block(s) moved to ").append(targetPos).append(".");

        if (entitiesToTeleport.size() > 0) {
            message.append("\nEntities Teleported: ").append(entitiesToTeleport.size());
            for (String pName : teleportedPlayers) {
                message.append("\n  - teleported player \"").append(pName).append("\"");
            }
            int nonPlayers = entitiesToTeleport.size() - teleportedPlayers.size();
            if (nonPlayers > 0) {
                message.append("\n  - teleported ").append(nonPlayers).append(" other entities");
            }
        }

        if (!replacedBlocksMap.isEmpty()) {
            message.append("\nReplaced Blocks:");
            replacedBlocksMap.forEach((state, count) -> message.append("\n  - ")
                    .append(state.getBlock().getName().getString()).append(": ").append(count));
        }

        if (!skippedBlocksMap.isEmpty()) {
            message.append("\nSkipped (Lost) Blocks:");
            skippedBlocksMap.forEach((state, count) -> message.append("\n  - ")
                    .append(state.getBlock().getName().getString()).append(": ").append(count));
        }

        TeleportAPI.LOGGER.info(message.toString());

        return new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                message.toString(), true, replacedCount, skippedCount, replacedBlocksMap, skippedBlocksMap,
                entitiesToTeleport.size(), teleportedPlayers);
    }

    private static class EntityData {
        final Entity entity;
        final double relX;
        final double relY;
        final double relZ;
        final String playerName;

        EntityData(Entity entity, double relX, double relY, double relZ, String playerName) {
            this.entity = entity;
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
            this.playerName = playerName;
        }
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
            teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, true, true, true, true, true);
        } else {
            teleportByCorners(world, p1, p2, world, targetPos, null, true, false, true, true, true);
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
            teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, true, true, true, true,
                    true);
        } else {
            teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, null, true, false, true, true, true);
        }
    }

    public static TeleportResult teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                true, true, true);
    }

    public static TeleportResult teleportByCorners(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions, boolean includeAir) {
        return teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                includeAir, true, true);
    }

    public static TeleportResult teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, true, true, true);
    }

    public static TeleportResult teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromCorners(p1, p2);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                PasteMode.FORCE_REPLACE, null, includeAir, teleportPlayers, teleportEntities);
    }

    /**
     * Teleport structure defined by 6 points.
     */
    public static void teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos) {
        teleportBySixPoints(world, points, world, targetPos, null, true, true, true, true, true);
    }

    public static void teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos) {
        teleportBySixPoints(sourceLevel, points, targetLevel, targetPos, null, true, true, true, true, true);
    }

    public static TeleportResult teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport) {
        return teleportBySixPoints(world, points, world, targetPos, excludedBlocks, shouldTeleport, true, true, true,
                true);
    }

    public static TeleportResult teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportBySixPoints(world, points, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                true, true, true);
    }

    public static TeleportResult teleportBySixPoints(Level world, BlockPos[] points, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions, boolean includeAir) {
        return teleportBySixPoints(world, points, world, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                includeAir, true, true);
    }

    public static TeleportResult teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportBySixPoints(sourceLevel, points, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, true, true, true);
    }

    public static TeleportResult teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromSixPoints(points);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                PasteMode.FORCE_REPLACE, null, includeAir, teleportPlayers, teleportEntities);
    }
}
