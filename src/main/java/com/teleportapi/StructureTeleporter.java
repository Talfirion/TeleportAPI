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
import net.minecraft.server.level.TicketType;
import com.teleportapi.permissions.PermissionHelper;
import com.teleportapi.permissions.PermissionHelper.CheckResult;
import net.minecraft.world.level.ChunkPos;

import com.teleportapi.event.StructureTeleportEvent;
import net.minecraftforge.common.MinecraftForge;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StructureTeleporter {

    /**
     * List of NBT tags that are removed during teleportation to prevent
     * conflicts.
     * These usually include coordinate data and multiblock-specific internal links.
     * Removing these forces mods to re-calculate their multiblock structures or
     * connections at the destination.
     */
    public static final List<String> DEFAULT_CLEANED_TAGS = List.of(
            "x", "y", "z", "id", // Coordinate and ID data
            "master", "slave", "part", // Common multiblock references
            "multiblock_data", "connections" // Specific multiblock states
    );

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
     * Get default list of blocks that should be preserved at the destination if
     * encountered.
     * Usually matches the excluded blocks list to ensure critical blocks are never
     * overwritten.
     */
    public static List<BlockState> getDefaultPreservedBlocks() {
        return getDefaultExcludedBlocks();
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
        if (!checkExclusions) {
            return false;
        }

        // If list is null, fallback to API defaults
        if (excludedBlocks == null) {
            excludedBlocks = getDefaultExcludedBlocks();
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
    @SuppressWarnings("null")
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
     * Identify all positions within the selection that are "enclosed" by the
     * coverage blocks.
     * Uses a flood-fill algorithm starting from the boundaries.
     * 
     * @param selection      The selection area.
     * @param coverageBlocks List of block states that form the "hull".
     * @return Set of BlockPos that are part of the hull or enclosed by it.
     */
    @SuppressWarnings("null")
    public static Set<BlockPos> getEnclosedPositions(Selection selection, List<BlockState> coverageBlocks) {
        return getEnclosedPositionsGeneric(selection.getMin(), selection.getMax(), coverageBlocks,
                selection.getWorld()::getBlockState, StructureTeleporter::isBlockInList);
    }

    /**
     * Generic version of the enclosed positions filter.
     */
    public static <T> Set<BlockPos> getEnclosedPositionsGeneric(BlockPos min, BlockPos max, List<T> allowedStates,
            StateProvider<T> provider, StateMatcher<T> matcher) {
        Set<BlockPos> outside = new java.util.HashSet<>();
        java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();

        // 1. Queue up all border positions that are NOT coverage blocks
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                addIfOutsideGeneric(new BlockPos(x, y, min.getZ()), allowedStates, provider, matcher, outside, queue);
                if (max.getZ() > min.getZ())
                    addIfOutsideGeneric(new BlockPos(x, y, max.getZ()), allowedStates, provider, matcher, outside,
                            queue);
            }
        }
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                addIfOutsideGeneric(new BlockPos(x, min.getY(), z), allowedStates, provider, matcher, outside, queue);
                if (max.getY() > min.getY())
                    addIfOutsideGeneric(new BlockPos(x, max.getY(), z), allowedStates, provider, matcher, outside,
                            queue);
            }
        }
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                addIfOutsideGeneric(new BlockPos(min.getX(), y, z), allowedStates, provider, matcher, outside, queue);
                if (max.getX() > min.getX())
                    addIfOutsideGeneric(new BlockPos(max.getX(), y, z), allowedStates, provider, matcher, outside,
                            queue);
            }
        }

        // 2. Flood fill to find all reachable "open" space
        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();

            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                @SuppressWarnings("null")
                BlockPos neighbor = current.relative(dir);
                if (isWithinBounds(neighbor, min, max) && !outside.contains(neighbor)) {
                    if (!matcher.matches(provider.getState(neighbor), allowedStates)) {
                        outside.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }

        // 3. Everything NOT in 'outside' is 'inside' (hull or enclosed)
        Set<BlockPos> enclosed = new java.util.HashSet<>();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!outside.contains(pos)) {
                        enclosed.add(pos);
                    }
                }
            }
        }

        return enclosed;
    }

    private static <T> void addIfOutsideGeneric(BlockPos pos, List<T> allowedStates, StateProvider<T> provider,
            StateMatcher<T> matcher, Set<BlockPos> outside, java.util.Queue<BlockPos> queue) {
        if (!outside.contains(pos) && !matcher.matches(provider.getState(pos), allowedStates)) {
            outside.add(pos);
            queue.add(pos);
        }
    }

    @SuppressWarnings("null")

    private static boolean isWithinBounds(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() >= min.getX() && pos.getX() <= max.getX() &&
                pos.getY() >= min.getY() && pos.getY() <= max.getY() &&
                pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
    }

    /**
     * Check if all blocks on the faces of the selection are of the specified types.
     * Coverage requires at least one block thickness on each of the 6 sides.
     * 
     * @param selection      The selection area to check.
     * @param coverageBlocks List of block states that are allowed as coverage.
     * @return true if the area is fully covered, false otherwise.
     */
    public static boolean isAreaCovered(Selection selection, List<BlockState> coverageBlocks) {
        if (getEnclosedPositions(selection, coverageBlocks).isEmpty()) {
            return false;
        }

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        Level world = selection.getWorld();

        return isAreaCovered(min, max, coverageBlocks, world::getBlockState);
    }

    /**
     * Functional interface for providing block states at specific positions.
     */
    @FunctionalInterface
    public interface BlockStateProvider {
        BlockState getBlockState(BlockPos pos);
    }

    /**
     * Internal implementation of coverage check using a BlockStateProvider.
     */
    public static boolean isAreaCovered(BlockPos min, BlockPos max, List<BlockState> coverageBlocks,
            BlockStateProvider provider) {
        return isAreaCoveredGeneric(min, max, coverageBlocks, provider::getBlockState,
                StructureTeleporter::isBlockInList);
    }

    /**
     * Functional interface for checking if an object matches a list of allowed
     * objects.
     */
    @FunctionalInterface
    public interface StateMatcher<T> {
        boolean matches(T state, List<T> allowed);
    }

    /**
     * Functional interface for getting a state at a position.
     */
    @FunctionalInterface
    public interface StateProvider<T> {
        T getState(BlockPos pos);
    }

    /**
     * Generic implementation of coverage check.
     */
    public static <T> boolean isAreaCoveredGeneric(BlockPos min, BlockPos max, List<T> allowedStates,
            StateProvider<T> provider, StateMatcher<T> matcher) {
        if (allowedStates == null || allowedStates.isEmpty()) {
            return true;
        }

        // Check each of the 6 faces
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                // Front and Back faces (Z_MIN and Z_MAX)
                if (!matcher.matches(provider.getState(new BlockPos(x, y, min.getZ())), allowedStates) ||
                        !matcher.matches(provider.getState(new BlockPos(x, y, max.getZ())), allowedStates)) {
                    return false;
                }
            }
        }

        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                // Top and Bottom faces (Y_MIN and Y_MAX)
                if (!matcher.matches(provider.getState(new BlockPos(x, min.getY(), z)), allowedStates) ||
                        !matcher.matches(provider.getState(new BlockPos(x, max.getY(), z)), allowedStates)) {
                    return false;
                }
            }
        }

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                // Left and Right faces (X_MIN and X_MAX)
                if (!matcher.matches(provider.getState(new BlockPos(min.getX(), y, z)), allowedStates) ||
                        !matcher.matches(provider.getState(new BlockPos(max.getX(), y, z)), allowedStates)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static boolean isBlockInList(BlockState state, List<BlockState> list) {
        for (BlockState allowed : list) {
            if (state.getBlock() == allowed.getBlock()) {
                return true;
            }
        }
        return false;
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
            // If list is null, fallback to API defaults
            if (preservedBlocks == null) {
                preservedBlocks = getDefaultPreservedBlocks();
            }

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
        return copyStructure(selection, excludedBlocks, checkExclusions, includeAir, null);
    }

    @SuppressWarnings("null")
    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks,
            boolean checkExclusions, boolean includeAir, Set<BlockPos> enclosedPositions) {

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

                    // Coverage filter check
                    if (enclosedPositions != null && !enclosedPositions.contains(pos)) {
                        continue;
                    }

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
                    // Note: blockEntity.saveWithFullMetadata() ensures that all modded data
                    // like AE2 cell contents, Botania mana, and various block timers are preserved.
                    CompoundTag nbt = null;
                    BlockEntity blockEntity = world.getBlockEntity(pos);
                    if (blockEntity != null) {
                        nbt = blockEntity.saveWithFullMetadata();

                        // Clean up volatile tags (coordinates, multiblock links)
                        for (String tag : DEFAULT_CLEANED_TAGS) {
                            nbt.remove(tag);
                        }
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

    /**
     * Copy specific positions to a list of BlockData.
     * 
     * @param world     The world to copy from
     * @param positions Collection of absolute positions to copy
     * @param origin    The reference point for relative coordinates
     * @return List of copied block data
     */
    public static List<BlockData> copyStructure(Level world, Collection<BlockPos> positions, BlockPos origin) {
        return copyStructure(world, positions, origin, null, true, true);
    }

    /**
     * Copy specific positions to a list of BlockData.
     * 
     * @param world           The world to copy from
     * @param positions       Collection of absolute positions to copy
     * @param origin          The reference point for relative coordinates
     * @param excludedBlocks  List of blocks to exclude
     * @param checkExclusions Whether to check exclusions
     * @param includeAir      Whether to include air blocks
     * @return List of copied block data
     */
    @SuppressWarnings("null")
    public static List<BlockData> copyStructure(Level world, Collection<BlockPos> positions, BlockPos origin,
            List<BlockState> excludedBlocks, boolean checkExclusions, boolean includeAir) {
        if (positions == null || positions.isEmpty() || world == null) {
            return new ArrayList<>();
        }

        List<BlockData> blocks = new ArrayList<>();

        for (BlockPos pos : positions) {
            BlockState state = world.getBlockState(pos);

            // Skip air if not included
            if (!includeAir && state.isAir()) {
                continue;
            }

            // Skip excluded blocks
            if (isExcluded(state, excludedBlocks, checkExclusions)) {
                continue;
            }

            // Get NBT data if available
            CompoundTag nbt = null;
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity != null) {
                nbt = blockEntity.saveWithFullMetadata();

                // Clean up volatile tags
                for (String tag : DEFAULT_CLEANED_TAGS) {
                    nbt.remove(tag);
                }
            }

            // Calculate relative position based on provided origin
            BlockPos relativePos = pos.subtract(origin);

            // Save block (sanitized)
            blocks.add(new BlockData(relativePos, sanitizeBlockState(state), nbt));
        }

        TeleportAPI.LOGGER.info("Blocks copied from collection: " + blocks.size());
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
            TeleportAPI.LOGGER.warn("[TeleportAPI] Paste: No blocks to paste!");
            return;
        }

        // Force load chunks at target location
        if (world instanceof ServerLevel serverLevel) {
            Set<ChunkPos> targetedChunks = new HashSet<>();
            for (BlockData blockData : blocks) {
                BlockPos absolutePos = targetPos.offset(blockData.relativePos);
                targetedChunks.add(new ChunkPos(absolutePos));
            }

            for (ChunkPos chunkPos : targetedChunks) {
                serverLevel.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
            }
        }

        // Create a map for NBT data for Pass 1
        Map<BlockPos, CompoundTag> nbtMap = new HashMap<>();
        for (BlockData blockData : blocks) {
            if (blockData.nbt != null) {
                nbtMap.put(targetPos.offset(blockData.relativePos), blockData.nbt);
            }
        }

        // Clearing in reverse order (Top-to-Bottom) to prevent dependent blocks from
        // dropping.
        for (int i = blocks.size() - 1; i >= 0; i--) {
            BlockData blockData = blocks.get(i);
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            if (isOutsideHeightLimits(absolutePos, world.getMinBuildHeight(), world.getMaxBuildHeight())) {
                continue;
            }

            BlockState existingState = world.getBlockState(absolutePos);

            if (shouldReplace(existingState, blockData.blockState, mode, preservedBlocks)) {
                // Clear to AIR (flag 2 | 16 | 32 | 64)
                // 2 = UPDATE_CLIENTS, 16 = NO_NEIGHBOR_UPDATE, 32 = PREVENT_NEIGHBOR_REACTIONS,
                // 64 = IS_MOVING
                world.setBlock(absolutePos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32 | 64);
            }
        }

        // Pass 2: Set real blocks and IMMEDIATELY load NBT

        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            if (isOutsideHeightLimits(absolutePos, world.getMinBuildHeight(), world.getMaxBuildHeight())) {
                continue;
            }

            if (shouldReplace(world.getBlockState(absolutePos), blockData.blockState, mode, preservedBlocks)) {
                world.setBlock(absolutePos, blockData.blockState, 16);

                // Immediately load NBT if it exists
                if (nbtMap.containsKey(absolutePos)) {
                    BlockEntity be = world.getBlockEntity(absolutePos);
                    if (be != null) {
                        CompoundTag tag = nbtMap.get(absolutePos).copy();
                        // Restore coordinates for the new location
                        tag.putInt("x", absolutePos.getX());
                        tag.putInt("y", absolutePos.getY());
                        tag.putInt("z", absolutePos.getZ());
                        // Ensure ID is removed to avoid conflict if the mod tries to assign a new one
                        tag.remove("id");
                        be.load(tag);
                        be.setChanged();
                    }
                }
            }
        }

        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            if (isOutsideHeightLimits(absolutePos, world.getMinBuildHeight(), world.getMaxBuildHeight())) {
                continue;
            }

            BlockState state = world.getBlockState(absolutePos);

            // Update neighbors at the position (notifies neighbors of this block)
            world.updateNeighborsAt(absolutePos, state.getBlock());

            /**
             * Aggressive shape update for the block itself.
             * This ensures that blocks like fences or walls correctly connect to their new
             * neighbors.
             * flag 3 = 1 (UPDATE_NEIGHBORS) | 2 (UPDATE_CLIENTS)
             */
            state.updateNeighbourShapes(world, absolutePos, 3);

            /**
             * Manual survival check for solitary blocks.
             * This ensures that blocks like grass, torches, or redstone re-evaluate their
             * "survival" (physics check) even if they have no neighbors to trigger an
             * update.
             * If the block cannot survive (e.g. grass on air), it is dropped as an item.
             */
            if (!state.canSurvive(world, absolutePos)) {
                world.destroyBlock(absolutePos, true);
            } else {
                // If it survives, still notify it just in case of other state dependencies
                world.neighborChanged(absolutePos, Blocks.AIR, absolutePos.below());
            }
        }

        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            if (isOutsideHeightLimits(absolutePos, world.getMinBuildHeight(), world.getMaxBuildHeight())) {
                continue;
            }

            BlockState state = world.getBlockState(absolutePos);
            // flag 2 = UPDATE_CLIENTS to synchronize but avoid redundant updates
            world.sendBlockUpdated(absolutePos, Blocks.AIR.defaultBlockState(), state, 2);
        }

    }

    /**
     * High-level API: Teleport a selection to a target position in the same world.
     */
    public static TeleportResult teleport(Selection selection, BlockPos targetPos) {
        return teleport(new TeleportRequest.Builder(selection, targetPos).build());
    }

    /**
     * High-level API: Teleport structure defined by two corners.
     * Uses default parameters.
     */
    public static TeleportResult teleport(Level world, BlockPos p1, BlockPos p2, BlockPos targetPos) {
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromCorners(p1, p2);
        return teleport(new TeleportRequest.Builder(selection, targetPos).build());
    }

    /**
     * High-level API: Teleport a specific collection of arbitrary block positions.
     */
    public static TeleportResult teleport(Level world, Collection<BlockPos> positions, BlockPos targetPos) {
        if (positions == null || positions.isEmpty()) {
            return TeleportResult.builder()
                    .success(false)
                    .message("Positions collection cannot be null or empty")
                    .build();
        }
        Selection selection = new Selection();
        selection.setWorld(world);
        selection.setFromPositions(positions);
        return teleport(new TeleportRequest.Builder(selection, targetPos).build());
    }

    /**
     * Main teleportation method using the new TeleportRequest API.
     */
    @SuppressWarnings({ "null" })
    public static TeleportResult teleport(TeleportRequest request) {
        Selection selection = request.getSelection();
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("Selection not complete!");
            return TeleportResult.builder()
                    .success(false)
                    .message("Selection not complete")
                    .build();
        }

        Level sourceWorld = selection.getWorld();
        Level targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : sourceWorld;
        BlockPos targetPos = request.getTargetPos();
        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        Player player = request.getPlayer();

        // Coverage filter
        Set<BlockPos> filter = request.getFilter();
        if (filter != null && filter.isEmpty()) {
            TeleportAPI.LOGGER.warn("Teleportation denied: Filter is empty.");
            return TeleportResult.failure("Teleportation denied: Filter is empty.", 0, 0, new HashSet<>(), 0, 0);
        }

        // Metrics and tracking
        int totalBlocks = 0;
        int excludedCount = 0;
        int replacedCount = 0;
        int skippedCount = 0;
        int skippedByLimitCount = 0;
        int airBlockCount = 0;
        int solidBlockCount = 0;
        int destinationSolidBlocksLost = 0;

        Set<BlockState> excludedTypes = new HashSet<>();
        Map<BlockState, Integer> sourceBlockCounts = new HashMap<>();
        Map<BlockState, Integer> replacedBlocksMap = new HashMap<>();
        Map<BlockState, Integer> skippedBlocksMap = new HashMap<>();

        List<BlockState> excludedBlocks = request.getExcludedBlocks();
        boolean checkExclusions = request.isCheckExclusions();
        boolean includeAir = request.isIncludeAir();
        PasteMode pasteMode = request.getPasteMode();
        List<BlockState> preservedBlocks = request.getPreservedBlocks();

        // Pass 1: Count metrics
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = sourceWorld.getBlockState(pos);

                    if (filter != null && !filter.contains(pos))
                        continue;

                    if (state.isAir()) {
                        if (includeAir) {
                            airBlockCount++;
                            totalBlocks++;
                            sourceBlockCounts.merge(state, 1, (a, b) -> a + b);
                        }
                        continue;
                    }

                    totalBlocks++;
                    sourceBlockCounts.merge(state, 1, (a, b) -> a + b);
                    if (isExcluded(state, excludedBlocks, checkExclusions)) {
                        excludedCount++;
                        excludedTypes.add(state);
                    } else {
                        solidBlockCount++;
                    }
                }
            }
        }

        // Pass 2: Scanning target area feedback
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos srcPos = new BlockPos(x, y, z);
                    BlockState srcState = sourceWorld.getBlockState(srcPos);

                    if (filter != null && !filter.contains(srcPos))
                        continue;
                    if (srcState.isAir() && !includeAir)
                        continue;

                    if (!isExcluded(srcState, excludedBlocks, checkExclusions)) {
                        BlockPos relPos = srcPos.subtract(min);
                        BlockPos dstPos = targetPos.offset(relPos);

                        if (isOutsideHeightLimits(dstPos, targetLevel.getMinBuildHeight(),
                                targetLevel.getMaxBuildHeight())) {
                            skippedByLimitCount++;
                            continue;
                        }

                        BlockState dstState = targetLevel.getBlockState(dstPos);
                        if (shouldReplace(dstState, srcState, pasteMode, preservedBlocks)) {
                            replacedCount++;
                            replacedBlocksMap.merge(dstState, 1, (a, b) -> a + b);
                            if (!dstState.isAir())
                                destinationSolidBlocksLost++;
                        } else {
                            skippedCount++;
                            skippedBlocksMap.merge(srcState, 1, (a, b) -> a + b);
                        }
                    }
                }
            }
        }

        // Entity Detection
        AABB selectionBox = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1,
                max.getZ() + 1);
        List<Entity> entities = sourceWorld.getEntitiesOfClass(Entity.class, selectionBox);
        List<EntityData> entitiesToTeleport = new ArrayList<>();

        for (Entity entity : entities) {
            boolean isPlayer = entity instanceof Player;
            if (isPlayer && !request.isTeleportPlayers())
                continue;
            if (!isPlayer && !request.isTeleportEntities())
                continue;

            String entityPlayerName = isPlayer ? ((Player) entity).getName().getString() : null;
            double relX = entity.getX() - min.getX();
            double relY = entity.getY() - min.getY();
            double relZ = entity.getZ() - min.getZ();
            entitiesToTeleport.add(new EntityData(entity, relX, relY, relZ, entityPlayerName));
        }

        // Permission Checks
        CheckResult sourceCheck = PermissionHelper.checkAreaPermissions(player, sourceWorld, selection, true);
        if (!sourceCheck.isAllowed()) {
            return TeleportResult.permissionDeny("Source permission denied: " + sourceCheck.getReason(),
                    totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount,
                    sourceCheck.getFailedPos(), sourceCheck.getReason());
        }

        Selection targetSelection = new Selection();
        targetSelection.setWorld(targetLevel);
        targetSelection.setFromCorners(targetPos, targetPos.offset(max.subtract(min)));
        CheckResult targetCheck = PermissionHelper.checkAreaPermissions(player, targetLevel, targetSelection, false);
        if (!targetCheck.isAllowed()) {
            return TeleportResult.permissionDeny("Target permission denied: " + targetCheck.getReason(),
                    totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount,
                    targetCheck.getFailedPos(), targetCheck.getReason());
        }

        double distance = Math.sqrt(min.distSqr(targetPos));
        String sourceDim = sourceWorld.dimension().location().toString();
        String targetDim = targetLevel.dimension().location().toString();

        if (!request.shouldTeleport()) {
            return TeleportResult.builder()
                    .success(true)
                    .totalBlocks(totalBlocks)
                    .excludedBlocks(excludedCount)
                    .excludedBlockTypes(excludedTypes)
                    .message("Scan complete (shouldTeleport=false)")
                    .teleported(false)
                    .replacedBlockCount(replacedCount)
                    .skippedBlockCount(skippedCount)
                    .skippedByLimitCount(skippedByLimitCount)
                    .airBlockCount(airBlockCount)
                    .solidBlockCount(solidBlockCount)
                    .destinationSolidBlocksLost(destinationSolidBlocksLost)
                    .replacedBlocksMap(replacedBlocksMap)
                    .skippedBlocksMap(skippedBlocksMap)
                    .teleportedEntitiesCount(entitiesToTeleport.size())
                    .teleportedPlayerNames(entitiesToTeleport.stream().filter(e -> e.playerName != null)
                            .map(e -> e.playerName).collect(Collectors.toList()))
                    .distance(distance)
                    .sourceDimension(sourceDim)
                    .targetDimension(targetDim)
                    .sourceBlockCounts(sourceBlockCounts)
                    .build();
        }

        // Fire Pre Event
        StructureTeleportEvent.Pre preEvent = new StructureTeleportEvent.Pre(selection, targetLevel, targetPos, player);
        if (MinecraftForge.EVENT_BUS.post(preEvent)) {
            return TeleportResult.failure("Teleportation canceled by event", totalBlocks, excludedCount, excludedTypes,
                    airBlockCount, solidBlockCount);
        }

        // Actual Teleportation
        List<BlockData> blocksToMove = copyStructure(selection, excludedBlocks, checkExclusions, includeAir, filter);
        if (blocksToMove.isEmpty()) {
            return TeleportResult.failure("No blocks to teleport after exclusions", totalBlocks, excludedCount,
                    excludedTypes, airBlockCount, solidBlockCount);
        }

        List<String> teleportedPlayers = new ArrayList<>();
        try {
            // Remove source blocks
            for (int y = max.getY(); y >= min.getY(); y--) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (filter != null && !filter.contains(pos))
                            continue;
                        BlockState state = sourceWorld.getBlockState(pos);
                        if (!isExcluded(state, excludedBlocks, checkExclusions)) {
                            sourceWorld.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32 | 64);
                        }
                    }
                }
            }

            // Neighbor updates for source
            for (int y = max.getY(); y >= min.getY(); y--) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (filter != null && !filter.contains(pos))
                            continue;
                        BlockState state = sourceWorld.getBlockState(pos);
                        sourceWorld.updateNeighborsAt(pos, state.getBlock());
                        state.updateNeighbourShapes(sourceWorld, pos, 3);
                    }
                }
            }

            // Paste target
            pasteStructure(blocksToMove, targetPos, targetLevel, pasteMode, preservedBlocks);

            // Teleport entities
            for (EntityData info : entitiesToTeleport) {
                double tx = targetPos.getX() + info.relX;
                double ty = targetPos.getY() + info.relY;
                double tz = targetPos.getZ() + info.relZ;

                if (targetLevel != sourceWorld && info.entity instanceof ServerPlayer sp) {
                    if (targetLevel instanceof ServerLevel sl) {
                        sp.teleportTo(sl, tx, ty, tz, sp.getYRot(), sp.getXRot());
                    }
                } else {
                    info.entity.teleportTo(tx, ty, tz);
                    if (targetLevel != sourceWorld && targetLevel instanceof ServerLevel sl) {
                        info.entity.changeDimension(sl);
                    }
                }
                if (info.playerName != null)
                    teleportedPlayers.add(info.playerName);
            }

        } catch (Exception e) {
            TeleportAPI.LOGGER.error("Teleportation failed! Attempting rollback. Error: " + e.getMessage(), e);
            try {
                pasteStructure(blocksToMove, min, sourceWorld, PasteMode.FORCE_REPLACE, null);
                return TeleportResult.failure("Teleportation failed: " + e.getMessage() + ". Rollback successful.",
                        totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount);
            } catch (Exception re) {
                TeleportAPI.LOGGER.error("ROLLBACK FAILED! Error: " + re.getMessage(), re);
                return TeleportResult.failure("CRITICAL: Teleportation and Rollback failed! Error: " + e.getMessage(),
                        totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount);
            }
        }

        TeleportResult result = TeleportResult.builder()
                .success(true)
                .totalBlocks(totalBlocks)
                .excludedBlocks(excludedCount)
                .excludedBlockTypes(excludedTypes)
                .message("Structure teleported successfully.")
                .teleported(true)
                .replacedBlockCount(replacedCount)
                .skippedBlockCount(skippedCount)
                .skippedByLimitCount(skippedByLimitCount)
                .airBlockCount(airBlockCount)
                .solidBlockCount(solidBlockCount)
                .destinationSolidBlocksLost(destinationSolidBlocksLost)
                .replacedBlocksMap(replacedBlocksMap)
                .skippedBlocksMap(skippedBlocksMap)
                .teleportedEntitiesCount(entitiesToTeleport.size())
                .teleportedPlayerNames(teleportedPlayers)
                .distance(distance)
                .sourceDimension(sourceDim)
                .targetDimension(targetDim)
                .sourceBlockCounts(sourceBlockCounts)
                .build();

        MinecraftForge.EVENT_BUS
                .post(new StructureTeleportEvent.Post(selection, targetLevel, targetPos, player, result));
        return result;
    }

    /**
     * Checks if a destination position is outside the target level's height
     * limits.
     *
     * @param pos       The destination position.
     * @param minHeight The minimum build height of the level.
     * @param maxHeight The maximum build height of the level.
     * @return true if the position is out of bounds.
     */
    public static boolean isOutsideHeightLimits(BlockPos pos, int minHeight, int maxHeight) {
        return pos.getY() < minHeight || pos.getY() >= maxHeight;
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
}
