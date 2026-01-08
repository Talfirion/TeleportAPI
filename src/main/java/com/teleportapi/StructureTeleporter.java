package com.teleportapi;

import net.minecraft.core.BlockPos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.core.Vec3i;

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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.ArrayList;
import java.util.Collection;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import java.util.Set;

import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;

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
        return copyStructure(selection, excludedBlocks, checkExclusions, includeAir, null, null);
    }

    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks,
            boolean checkExclusions, boolean includeAir, Set<BlockPos> enclosedPositions) {
        return copyStructure(selection, excludedBlocks, checkExclusions, includeAir, enclosedPositions, null);
    }

    @SuppressWarnings("null")
    public static List<BlockData> copyStructure(Selection selection, List<BlockState> excludedBlocks,
            boolean checkExclusions, boolean includeAir, Set<BlockPos> enclosedPositions,
            java.util.BitSet validBlocksMask) {

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

                    // BitSet filter check - CRITICAL for preventing unwanted blocks from being
                    // copied
                    if (validBlocksMask != null) {
                        int dx = x - min.getX();
                        int dy = y - min.getY();
                        int dz = z - min.getZ();
                        int width = max.getX() - min.getX() + 1;
                        int height = max.getY() - min.getY() + 1;
                        int index = dx + width * (dy + height * dz);

                        // If bit is not set, skip this block (don't copy it)
                        if (!validBlocksMask.get(index)) {
                            continue;
                        }
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

    /**
     * Clear blocks within a selection without dropping items or causing block
     * entity drops.
     * Useful for cleaning up source locations during teleportation or structure
     * removal.
     * 
     * @param selection The selection area to clear
     * @param silent    If true, skip neighbor updates for performance
     */
    @SuppressWarnings("null")
    public static void clearStructure(Selection selection, boolean silent) {
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("[TeleportAPI] clearStructure: Selection not complete!");
            return;
        }

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();
        Level world = selection.getWorld();

        TeleportAPI.LOGGER.info("[TeleportAPI] Clearing structure from " + min + " to " + max);

        // Pass 1: Remove all Block Entities (prevents item drops from containers)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = max.getY(); y >= min.getY(); y--) { // Top to bottom
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockEntity be = world.getBlockEntity(pos);
                    if (be != null) {
                        // Remove block entity data before breaking block
                        world.removeBlockEntity(pos);
                    }
                }
            }
        }

        // Pass 2: Set blocks to AIR (top-down to prevent physics drops)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = max.getY(); y >= min.getY(); y--) { // Top to bottom!
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(pos);

                    if (!state.isAir()) {
                        // Flag explanation:
                        // 2 = UPDATE_CLIENTS (send to clients)
                        // 16 = NO_NEIGHBOR_UPDATE (prevents redstone triggers)
                        // 32 = NO_OBSERVER (prevents observer triggers)
                        // 64 = UPDATE_INVISIBLE (update anyway)
                        // This combination silently removes blocks without drops
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32 | 64);
                    }
                }
            }
        }

        // Pass 3: Final cleanup - update neighbors if not silent
        if (!silent) {
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int y = min.getY(); y <= max.getY(); y++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        world.updateNeighborsAt(pos, Blocks.AIR);
                    }
                }
            }
        }

        TeleportAPI.LOGGER.info("[TeleportAPI] Structure cleared successfully");
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
                // Pre-emptively remove block entity to prevent item drops (e.g. chests)
                BlockEntity be = world.getBlockEntity(absolutePos);
                if (be != null) {
                    world.removeBlockEntity(absolutePos);
                }
                // Clear to AIR (flag 2 | 16 | 32 | 64)
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
                // Set block with UPDATE_CLIENTS and NO_NEIGHBOR_UPDATE
                world.setBlock(absolutePos, blockData.blockState, 18);

                // Immediately load NBT if it exists
                if (nbtMap.containsKey(absolutePos)) {
                    BlockEntity be = world.getBlockEntity(absolutePos);
                    if (be != null) {
                        CompoundTag tag = nbtMap.get(absolutePos).copy();
                        tag.putInt("x", absolutePos.getX());
                        tag.putInt("y", absolutePos.getY());
                        tag.putInt("z", absolutePos.getZ());
                        tag.remove("id");
                        be.load(tag);
                        be.setChanged();
                    }
                }
            }
        }

        // Pass 3: Combined Updates and Client Sync
        for (BlockData blockData : blocks) {
            BlockPos absolutePos = targetPos.offset(blockData.relativePos);

            if (isOutsideHeightLimits(absolutePos, world.getMinBuildHeight(), world.getMaxBuildHeight())) {
                continue;
            }

            BlockState state = world.getBlockState(absolutePos);

            // 1. Neighbor and Shape Updates
            world.updateNeighborsAt(absolutePos, state.getBlock());
            state.updateNeighbourShapes(world, absolutePos, 3);

            // 2. Survival Check
            if (!state.canSurvive(world, absolutePos)) {
                world.destroyBlock(absolutePos, true);
            } else {
                world.neighborChanged(absolutePos, Blocks.AIR, absolutePos.below());
            }

            // 3. Client Synchronization
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
    @SuppressWarnings({ "null", "deprecation" })
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

        Rotation rotation = request.getRotation();
        Mirror mirror = request.getMirror();
        Vec3i sourceSize = max.subtract(min);

        // Metrics and tracking
        int totalBlocks = 0;
        int excludedCount = 0;
        int replacedCount = 0;
        int skippedCount = 0;
        int skippedByLimitCount = 0;
        int airBlockCount = 0;
        int solidBlockCount = 0;
        int fluidBlockCount = 0;
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

        Integer blocksPerTickVal = request.getBlocksPerTick();
        int blocksPerTick = blocksPerTickVal != null ? blocksPerTickVal : 0;
        boolean useAsync = blocksPerTick > 0;

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
                        if (!state.getFluidState().isEmpty() && state.getFluidState().isSource()) {
                            fluidBlockCount++;
                        }
                    }
                }
            }
        }

        // Pass 2: Scanning target area feedback
        // In the legacy synchronous version, we can do a simplified pre-check or just
        // calculate potential overlaps.
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
                        BlockPos transformedRelPos = transformPos(relPos, rotation, mirror, sourceSize);
                        BlockPos dstPos = targetPos.offset(transformedRelPos);

                        if (isOutsideHeightLimits(dstPos, targetLevel.getMinBuildHeight(),
                                targetLevel.getMaxBuildHeight())) {
                            skippedByLimitCount++;
                            continue;
                        }

                        BlockState dstState = targetLevel.getBlockState(dstPos);

                        boolean isDstBlockFromSource = false;
                        if (sourceWorld == targetLevel) {
                            if (dstPos.getX() >= min.getX() && dstPos.getX() <= max.getX() &&
                                    dstPos.getY() >= min.getY() && dstPos.getY() <= max.getY() &&
                                    dstPos.getZ() >= min.getZ() && dstPos.getZ() <= max.getZ()) {
                                // It's inside the bounding box, check filter
                                if (filter == null || filter.contains(dstPos)) {
                                    BlockState dstSourceState = sourceWorld.getBlockState(dstPos);
                                    if (!isExcluded(dstSourceState, excludedBlocks, checkExclusions)) {
                                        isDstBlockFromSource = true;
                                    }
                                }
                            }
                        }

                        BlockState effectiveDstState = isDstBlockFromSource ? Blocks.AIR.defaultBlockState() : dstState;

                        if (shouldReplace(effectiveDstState, srcState, pasteMode, preservedBlocks)) {
                            // Predicted replacement
                            if (!dstState.isAir() && !isDstBlockFromSource)
                                destinationSolidBlocksLost++;
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
            CompoundTag entityTag = null;
            GameType gameType = null;

            if (isPlayer) {
                if (entity instanceof ServerPlayer sp) {
                    gameType = sp.gameMode.getGameModeForPlayer();
                }
            } else {
                entityTag = new CompoundTag();
                if (!entity.save(entityTag)) {
                    entityTag = entity.saveWithoutId(new CompoundTag());
                }
                if (!entityTag.contains("id")) {
                    net.minecraft.resources.ResourceLocation key = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES
                            .getKey(entity.getType());
                    if (key != null) {
                        entityTag.putString("id", key.toString());
                    }
                }
            }

            double relX = entity.getX() - min.getX();
            double relY = entity.getY() - min.getY();
            double relZ = entity.getZ() - min.getZ();
            entitiesToTeleport.add(new EntityData(entity, relX, relY, relZ, entityPlayerName, entityTag, gameType));
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
        BlockPos transformedSize = transformPos(new BlockPos(sourceSize), rotation, mirror, sourceSize);
        // Approximate target bounds
        targetSelection.setFromCorners(targetPos, targetPos.offset(transformedSize));
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
                    .airBlockCount(airBlockCount)
                    .solidBlockCount(solidBlockCount)
                    .destinationSolidBlocksLost(destinationSolidBlocksLost)
                    .teleportedEntitiesCount(entitiesToTeleport.size())
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

        // *** RESTORED LEGACY LOGIC ***

        TeleportAPI.LOGGER.info("Starting Reliable Synchronous Teleport...");

        // Get BitSet mask if provided - needed for copyStructure
        java.util.BitSet validBlocksMask = request.getValidBlocksMask();

        // 1. SNAPSHOT: Copy blocks to memory
        // Pass BitSet to copyStructure to prevent unwanted blocks from being copied
        List<BlockData> sourceSnapshot = copyStructure(selection, excludedBlocks, checkExclusions, includeAir, filter,
                validBlocksMask);
        if (sourceSnapshot.isEmpty()) {
            return TeleportResult.failure("No blocks to teleport after filtering.", totalBlocks, 0, new HashSet<>(), 0,
                    0);
        }

        // Prepare Target List (Transformed) - Keep sourceSnapshot intact for Rollback!
        List<BlockData> blocksToPaste = new ArrayList<>(sourceSnapshot.size());
        Set<BlockPos> targetPositions = new HashSet<>();
        for (BlockData srcData : sourceSnapshot) {
            BlockPos transformedRelPos = transformPos(srcData.relativePos, rotation, mirror, sourceSize);
            BlockState transformedState = srcData.blockState.rotate(rotation).mirror(mirror);
            blocksToPaste.add(new BlockData(transformedRelPos, transformedState, srcData.nbt));
            targetPositions.add(targetPos.offset(transformedRelPos));
        }

        // UNDO SYSTEM INTEGRATION
        // Capture the state of the target area BEFORE we do anything to it.
        // We use copyStructure to get what's currently there.
        if (request.getPlayer() != null) {
            List<BlockData> targetSnapshot = copyStructure(targetLevel, targetPositions, targetPos, null, true, true);
            com.teleportapi.undo.UndoManager.getInstance().push(request.getPlayer(),
                    new com.teleportapi.undo.UndoContext(sourceWorld, targetLevel, min, targetPos, sourceSnapshot,
                            targetSnapshot, entitiesToTeleport));
        }

        List<String> teleportedPlayers = new ArrayList<>();

        try {
            // 2. CLEAR SOURCE
            // Extracted to reusable API method
            clearAreaWithMask(sourceWorld, selection, validBlocksMask, excludedBlocks, checkExclusions, includeAir,
                    filter);

            // 3. PASTE TARGET
            if (useAsync && blocksPerTick > 0) {
                // ASYNC MODE
                TeleportAPI.LOGGER.info("Starting ASYNC PASTE Task (" + blocksPerTick + " blocks/tick)");

                // Put players in SPECTATOR mode "Limbo"
                for (EntityData info : entitiesToTeleport) {
                    if (info.entity instanceof ServerPlayer sp) {
                        sp.setGameMode(GameType.SPECTATOR);
                        // Ideally we should also teleport them to a safe spot or keep them at source?
                        // For now, Spectator at source is fine, they will fly.
                    }
                }

                TeleportResult.Builder resultBuilder = TeleportResult.builder()
                        .totalBlocks(totalBlocks)
                        .excludedBlocks(excludedCount)
                        .excludedBlockTypes(excludedTypes)
                        .replacedBlockCount(replacedCount)
                        .skippedBlockCount(skippedCount)
                        .skippedByLimitCount(skippedByLimitCount)
                        .airBlockCount(airBlockCount)
                        .solidBlockCount(solidBlockCount)
                        .fluidBlockCount(fluidBlockCount)
                        .destinationSolidBlocksLost(destinationSolidBlocksLost)
                        .replacedBlocksMap(replacedBlocksMap)
                        .skippedBlocksMap(skippedBlocksMap)
                        .distance(distance)
                        .sourceDimension(sourceDim)
                        .targetDimension(targetDim)
                        .sourceBlockCounts(sourceBlockCounts);

                AsyncPasteTask pasteTask = new AsyncPasteTask(blocksToPaste, targetPos, targetLevel, pasteMode,
                        preservedBlocks,
                        entitiesToTeleport, player, selection, rotation, mirror, sourceSize, blocksPerTick,
                        resultBuilder);
                MinecraftForge.EVENT_BUS.register(pasteTask);

                // Return "InProgress" result or null?
                // The prompt implies we return a result.
                // We should return a generic "Started" result.
                return resultBuilder
                        .success(true)
                        .message("Async Teleportation Started")
                        .teleported(false) // Not yet
                        .build();

            } else {
                // SYNC MODE (Instant)
                pasteStructure(blocksToPaste, targetPos, targetLevel, pasteMode, preservedBlocks);
            }

            // 4. TELEPORT ENTITIES (Sync only - Async handles it in finish())
            if (!useAsync || blocksPerTick <= 0) {
                teleportEntities(entitiesToTeleport, targetLevel, targetPos, rotation, mirror, sourceSize, sourceWorld,
                        teleportedPlayers);
            } // End Sync-Only Entity Teleport

        } catch (Exception e) {
            TeleportAPI.LOGGER.error("Teleportation FAILED! Attempting ROLLBACK...", e);

            // 5. ROLLBACK LOGIC
            try {
                // Restore source blocks from snapshot
                // We paste sourceSnapshot back to `min` (Source Origin)
                pasteStructure(sourceSnapshot, min, sourceWorld, PasteMode.FORCE_REPLACE, null);
                TeleportAPI.LOGGER.info("Rollback successful.");
            } catch (Exception ex) {
                TeleportAPI.LOGGER.error("CRITICAL: Rollback FAILED!", ex);
                return TeleportResult.failure(
                        "CRITICAL: Teleportation failed AND Rollback failed! World may be in inconsistent state.",
                        totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount);
            }

            return TeleportResult.failure("Teleportation failed and was rolled back. Error: " + e.getMessage(),
                    totalBlocks, excludedCount, excludedTypes, airBlockCount, solidBlockCount);
        }

        TeleportResult result = TeleportResult.builder()
                .success(true)
                .totalBlocks(totalBlocks)
                .excludedBlocks(excludedCount)
                .excludedBlockTypes(excludedTypes)
                .message("Teleportation complete (Synchronous)")
                .teleported(true)
                .replacedBlockCount(replacedCount) // Count from Pass 2 estimation? Or we should have counted during
                                                   // Paste?
                // `pasteStructure` doesn't return count. We can use the estimate
                // `replacedBlocksMap` from scan for now.
                .replacedBlockCount(replacedCount)
                .skippedBlockCount(skippedCount)
                .skippedByLimitCount(skippedByLimitCount)
                .airBlockCount(airBlockCount)
                .solidBlockCount(solidBlockCount)
                .fluidBlockCount(fluidBlockCount)
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

    /**
     * Start an asynchronous simulation.
     */
    public static void simulateAsync(TeleportRequest request) {
        AsyncScanTask task = new AsyncScanTask(request,
                request.getBlocksPerTick() != null ? request.getBlocksPerTick() : 1000);
        MinecraftForge.EVENT_BUS.register(task);
    }

    /**
     * Asynchronous scanning task for simulation
     */
    /**
     * Asynchronous scanning task for simulation
     */
    public static class AsyncScanTask {
        private final TeleportRequest request;
        private final Selection selection;
        private final Level sourceWorld;
        private final Level targetLevel;
        private final BlockPos targetPos;
        private final Player player;
        private final int blocksPerTick;

        // State
        private int x, y, z;
        private int minX, minY, minZ;
        private int maxX, maxY, maxZ;
        private boolean pass1Complete = false;
        private boolean pass2Complete = false;
        private final Set<BlockPos> filter;

        // Metrics
        private int totalBlocks = 0;
        private int excludedCount = 0;
        private int replacedCount = 0;
        private int skippedCount = 0;
        private int skippedByLimitCount = 0;
        private int airBlockCount = 0;
        private int solidBlockCount = 0;
        private int fluidBlockCount = 0;
        private int destinationSolidBlocksLost = 0;
        private Set<BlockState> excludedTypes = new HashSet<>();
        private Map<BlockState, Integer> sourceBlockCounts = new HashMap<>();
        private Map<BlockState, Integer> replacedBlocksMap = new HashMap<>();
        private Map<BlockState, Integer> skippedBlocksMap = new HashMap<>();

        private List<BlockState> excludedBlocks;
        private boolean checkExclusions;
        private boolean includeAir;
        private PasteMode pasteMode;
        List<BlockState> preservedBlocks;

        // BitSet for optimization (1 = valid block to teleport, 0 = skip/air)
        private final java.util.BitSet validBlocks;
        private final int width;
        private final int height;
        private final int depth;

        public AsyncScanTask(TeleportRequest request, int blocksPerTick) {
            this.request = request;
            this.selection = request.getSelection();
            this.sourceWorld = selection.getWorld();
            this.targetLevel = request.getTargetLevel() != null ? request.getTargetLevel() : sourceWorld;
            this.targetPos = request.getTargetPos();
            this.player = request.getPlayer();
            this.blocksPerTick = blocksPerTick <= 0 ? Integer.MAX_VALUE : blocksPerTick;
            this.filter = request.getFilter();

            this.excludedBlocks = request.getExcludedBlocks();
            this.checkExclusions = request.isCheckExclusions();
            this.includeAir = request.isIncludeAir();
            this.pasteMode = request.getPasteMode();
            this.preservedBlocks = request.getPreservedBlocks();

            BlockPos min = selection.getMin();
            BlockPos max = selection.getMax();
            this.minX = min.getX();
            this.minY = min.getY();
            this.minZ = min.getZ();
            this.maxX = max.getX();
            this.maxY = max.getY();
            this.maxZ = max.getZ();

            this.width = maxX - minX + 1;
            this.height = maxY - minY + 1;
            this.depth = maxZ - minZ + 1;
            // Initialize BitSet with size equal to total volume
            this.validBlocks = new java.util.BitSet(width * height * depth);

            // Start P1
            this.x = minX;
            this.y = minY;
            this.z = minZ;
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END)
                return;

            if (!pass1Complete) {
                runPass1();
            } else if (!pass2Complete) {
                runPass2();
            } else {
                finish();
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }

        private int getIndex(int x, int y, int z) {
            int dx = x - minX;
            int dy = y - minY;
            int dz = z - minZ;
            // Local index calculation: dx + width * (dy + height * dz)
            // Correction: Standard mapping is x + width * (y + height * z)?
            // Convention: x is fastest changing, then z, then y? Or x, y, z?
            // Let's use x + width * (y + height * z) to match typical Minecraft loops (y is
            // height).
            // Actually, in loops we do X (inner), Y (mid), Z (outer) in some places, or X,
            // Y, Z.
            // Let's stick to X + (Width * (Y + Height * Z)) to be safe and consistent.
            // Wait, common is (x, y, z). unique index.
            // dx [0..width-1]
            // dy [0..height-1]
            // dz [0..depth-1]
            // index = dx + width * (dy + height * dz)
            return dx + width * (dy + height * dz);
        }

        @SuppressWarnings("null")
        private void runPass1() {
            int actions = 0;
            // 50x scan speed for P1 (read only)
            int limit = (blocksPerTick == Integer.MAX_VALUE) ? Integer.MAX_VALUE : blocksPerTick * 50;

            while (actions < limit) {
                if (z > maxZ) {
                    z = minZ;
                    y++;
                    if (y > maxY) {
                        y = minY;
                        x++;
                        if (x > maxX) {
                            pass1Complete = true;
                            // Reset for Pass 2
                            x = minX;
                            y = minY;
                            z = minZ;
                            return;
                        }
                    }
                }

                BlockPos pos = new BlockPos(x, y, z);
                // Save current coords for BitSet before incrementing
                int cx = x;
                int cy = y;
                int cz = z;
                z++; // advance cursor for next loop

                if (filter != null && !filter.contains(pos))
                    continue;

                // Chunk load check (P1 only needs source)
                if (sourceWorld instanceof ServerLevel sl) {
                    ChunkPos cp = new ChunkPos(pos);
                    if (!sl.hasChunk(cp.x, cp.z)) {
                        // We could force load, or skip? Simulation should probably force load or it's
                        // inaccurate.
                        // Let's force load lightly
                        sl.getChunkSource().addRegionTicket(TicketType.FORCED, cp, 2, cp);
                        sl.getChunk(cp.x, cp.z);
                    }
                }

                BlockState state = sourceWorld.getBlockState(pos);
                if (state.isAir()) {
                    if (includeAir) {
                        airBlockCount++;
                        totalBlocks++;
                        sourceBlockCounts.merge(state, 1, (a, b) -> a + b);
                        // Mark as valid if air is included
                        validBlocks.set(getIndex(cx, cy, cz));
                    }
                    actions++;
                    continue;
                }

                totalBlocks++;
                sourceBlockCounts.merge(state, 1, (a, b) -> a + b);
                if (isExcluded(state, excludedBlocks, checkExclusions)) {
                    excludedCount++;
                    excludedTypes.add(state);
                    // Do NOT set bit - excluded blocks are treated as "nothing" (0)
                } else {
                    solidBlockCount++;
                    if (!state.getFluidState().isEmpty() && state.getFluidState().isSource()) {
                        fluidBlockCount++;
                    }
                    // Mark as valid
                    validBlocks.set(getIndex(cx, cy, cz));
                }
                actions++;
            }
        }

        @SuppressWarnings("null")
        private void runPass2() {
            int actions = 0;
            // 20x scan speed for P2 (read target)
            int limit = (blocksPerTick == Integer.MAX_VALUE) ? Integer.MAX_VALUE : blocksPerTick * 20;

            Rotation rotation = request.getRotation();
            Mirror mirror = request.getMirror();
            Vec3i sourceSize = new BlockPos(maxX, maxY, maxZ).subtract(new BlockPos(minX, minY, minZ));
            BlockPos min = new BlockPos(minX, minY, minZ);

            while (actions < limit) {
                if (z > maxZ) {
                    z = minZ;
                    y++;
                    if (y > maxY) {
                        y = minY;
                        x++;
                        if (x > maxX) {
                            pass2Complete = true;
                            return;
                        }
                    }
                }

                BlockPos srcPos = new BlockPos(x, y, z);
                z++;

                BlockState srcState = sourceWorld.getBlockState(srcPos);

                if (filter != null && !filter.contains(srcPos))
                    continue;

                if (srcState.isAir() && !includeAir)
                    continue;

                if (!isExcluded(srcState, excludedBlocks, checkExclusions)) {
                    BlockPos relPos = srcPos.subtract(min);
                    BlockPos transformedRelPos = transformPos(relPos, rotation, mirror, sourceSize);
                    @SuppressWarnings("null")
                    BlockPos dstPos = targetPos.offset(transformedRelPos);

                    if (targetLevel instanceof ServerLevel sl) {
                        @SuppressWarnings("null")
                        ChunkPos cp = new ChunkPos(dstPos);
                        if (!sl.hasChunk(cp.x, cp.z)) {
                            sl.getChunkSource().addRegionTicket(TicketType.FORCED, cp, 2, cp);
                            sl.getChunk(cp.x, cp.z);
                        }
                    }

                    if (isOutsideHeightLimits(dstPos, targetLevel.getMinBuildHeight(),
                            targetLevel.getMaxBuildHeight())) {
                        skippedByLimitCount++;
                        actions++;
                        continue;
                    }

                    @SuppressWarnings("null")
                    BlockState dstState = targetLevel.getBlockState(dstPos);
                    boolean isDstBlockFromSource = false;
                    if (sourceWorld == targetLevel) {
                        // Optimization: Simple bounds check for self-overlap
                        if (dstPos.getX() >= minX && dstPos.getX() <= maxX &&
                                dstPos.getY() >= minY && dstPos.getY() <= maxY &&
                                dstPos.getZ() >= minZ && dstPos.getZ() <= maxZ) {
                            if (filter == null || filter.contains(dstPos)) {
                                BlockState dstSourceState = sourceWorld.getBlockState(dstPos);
                                if (!isExcluded(dstSourceState, excludedBlocks, checkExclusions)) {
                                    isDstBlockFromSource = true;
                                }
                            }
                        }
                    }

                    BlockState effectiveDstState = isDstBlockFromSource ? Blocks.AIR.defaultBlockState() : dstState;

                    if (shouldReplace(effectiveDstState, srcState, pasteMode, preservedBlocks)) {
                        replacedCount++;
                        replacedBlocksMap.merge(dstState, 1, (a, b) -> a + b);
                        if (!dstState.isAir() && !isDstBlockFromSource)
                            destinationSolidBlocksLost++;
                    } else {
                        skippedCount++;
                        skippedBlocksMap.merge(srcState, 1, (a, b) -> a + b);
                    }
                    actions++;
                }
            }
        }

        private void finish() {
            // Entities
            BlockPos min = new BlockPos(minX, minY, minZ);
            AABB selectionBox = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
            List<Entity> entities = sourceWorld.getEntitiesOfClass(Entity.class, selectionBox);
            int entityCount = 0;
            List<String> playerNames = new ArrayList<>();
            for (Entity entity : entities) {
                boolean isPlayer = entity instanceof Player;
                if (isPlayer && !request.isTeleportPlayers())
                    continue;
                if (!isPlayer && !request.isTeleportEntities())
                    continue;
                entityCount++;
                if (isPlayer)
                    playerNames.add(((Player) entity).getName().getString());
            }

            @SuppressWarnings("null")
            double distance = Math.sqrt(min.distSqr(targetPos));

            TeleportResult result = TeleportResult.builder()
                    .success(true)
                    .totalBlocks(totalBlocks)
                    .excludedBlocks(excludedCount)
                    .excludedBlockTypes(excludedTypes)
                    .message("Async scan complete")
                    .teleported(false)
                    .replacedBlockCount(replacedCount)
                    .skippedBlockCount(skippedCount)
                    .skippedByLimitCount(skippedByLimitCount)
                    .airBlockCount(airBlockCount)
                    .solidBlockCount(solidBlockCount)
                    .fluidBlockCount(fluidBlockCount)
                    .destinationSolidBlocksLost(destinationSolidBlocksLost)
                    .replacedBlocksMap(replacedBlocksMap)
                    .skippedBlocksMap(skippedBlocksMap)
                    .teleportedEntitiesCount(entityCount)
                    .teleportedPlayerNames(playerNames)
                    .distance(distance)
                    .sourceDimension(sourceWorld.dimension().location().toString())
                    .targetDimension(targetLevel.dimension().location().toString())
                    .sourceBlockCounts(sourceBlockCounts)
                    .validBlocksMask(validBlocks) // Pass the BitSet
                    .build();

            MinecraftForge.EVENT_BUS
                    .post(new StructureTeleportEvent.Post(selection, targetLevel, targetPos, player, result));
        }
    }

    /**
     * Transform a relative position based on rotation and mirror.
     */
    public static BlockPos transformPos(BlockPos pos, Rotation rotation, Mirror mirror, Vec3i size) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        // 1. Mirroring
        switch (mirror) {
            case LEFT_RIGHT:
                z = size.getZ() - z;
                break;
            case FRONT_BACK:
                x = size.getX() - x;
                break;
            default:
                break;
        }

        // 2. Rotation (around anchor 0,0,0)
        switch (rotation) {
            case CLOCKWISE_90:
                return new BlockPos(size.getZ() - z, y, x);
            case CLOCKWISE_180:
                return new BlockPos(size.getX() - x, y, size.getZ() - z);
            case COUNTERCLOCKWISE_90:
                return new BlockPos(z, y, size.getX() - x);
            default:
                return new BlockPos(x, y, z);
        }
    }

    /**
     * Mirror a rotation angle.
     */
    public static float mirrorRotation(Mirror mirror, float yRot) {
        switch (mirror) {
            case LEFT_RIGHT:
                return (180.0F - yRot) % 360.0F;
            case FRONT_BACK:
                return (-yRot) % 360.0F;
            default:
                return yRot;
        }
    }

    public static class EntityData {
        public final Entity entity;
        public final double relX;
        public final double relY;
        public final double relZ;
        public final String playerName;
        public final CompoundTag entityData; // For mobs: serialized data
        public final GameType originalGameType; // For players: original game mode

        public EntityData(Entity entity, double relX, double relY, double relZ, String playerName,
                CompoundTag entityData,
                GameType originalGameType) {
            this.entity = entity;
            this.relX = relX;
            this.relY = relY;
            this.relZ = relZ;
            this.playerName = playerName;
            this.entityData = entityData;
            this.originalGameType = originalGameType;
        }
    }

    public static class AsyncPasteTask {
        private final List<BlockData> blocksToPaste;
        private final BlockPos targetPos;
        private final Level targetLevel;
        private final PasteMode mode;
        private final List<BlockState> preservedBlocks;
        private final List<EntityData> entitiesToTeleport;
        private final Player player;
        private final Selection selection;
        private final Rotation rotation;
        private final Mirror mirror;
        private final Vec3i sourceSize;
        private final int blocksPerTick;
        // Result building
        private final TeleportResult.Builder resultBuilder;

        private int currentIndex = 0;
        private boolean isCompleted = false;

        public AsyncPasteTask(List<BlockData> blocksToPaste, BlockPos targetPos, Level targetLevel, PasteMode mode,
                List<BlockState> preservedBlocks, List<EntityData> entitiesToTeleport, Player player,
                Selection selection, Rotation rotation, Mirror mirror, Vec3i sourceSize, int blocksPerTick,
                TeleportResult.Builder resultBuilder) {
            this.blocksToPaste = blocksToPaste;
            this.targetPos = targetPos;
            this.targetLevel = targetLevel;
            this.mode = mode;
            this.preservedBlocks = preservedBlocks;
            this.entitiesToTeleport = entitiesToTeleport;
            this.player = player;
            this.selection = selection;
            this.rotation = rotation;
            this.mirror = mirror;
            this.sourceSize = sourceSize;
            this.blocksPerTick = blocksPerTick;
            this.resultBuilder = resultBuilder;
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END)
                return;
            if (isCompleted)
                return;

            processBatch();
        }

        @SuppressWarnings("null")
        private void processBatch() {
            int processed = 0;
            int total = blocksToPaste.size();

            while (processed < blocksPerTick && currentIndex < total) {
                BlockData blockData = blocksToPaste.get(currentIndex);
                @SuppressWarnings("null")
                BlockPos absolutePos = targetPos.offset(blockData.relativePos);

                if (isOutsideHeightLimits(absolutePos, targetLevel.getMinBuildHeight(),
                        targetLevel.getMaxBuildHeight())) {
                    currentIndex++;
                    processed++;
                    continue;
                }

                if (targetLevel instanceof ServerLevel sl) {
                    @SuppressWarnings("null")
                    ChunkPos cp = new ChunkPos(absolutePos);
                    if (!sl.hasChunk(cp.x, cp.z)) {
                        sl.getChunkSource().addRegionTicket(TicketType.FORCED, cp, 2, cp);
                        sl.getChunk(cp.x, cp.z);
                    }
                }

                if (shouldReplace(targetLevel.getBlockState(absolutePos), blockData.blockState, mode,
                        preservedBlocks)) {
                    targetLevel.setBlock(absolutePos, blockData.blockState, 16);

                    if (blockData.nbt != null) {
                        @SuppressWarnings("null")
                        BlockEntity be = targetLevel.getBlockEntity(absolutePos);
                        if (be != null) {
                            CompoundTag tag = blockData.nbt.copy();
                            tag.putInt("x", absolutePos.getX());
                            tag.putInt("y", absolutePos.getY());
                            tag.putInt("z", absolutePos.getZ());
                            tag.remove("id");
                            be.load(tag);
                            be.setChanged();
                        }
                    }
                }

                currentIndex++;
                processed++;
            }

            if (currentIndex >= total) {
                finish();
            }
        }

        @SuppressWarnings("null")
        private void finish() {
            isCompleted = true;
            MinecraftForge.EVENT_BUS.unregister(this);

            for (BlockData blockData : blocksToPaste) {
                @SuppressWarnings("null")
                BlockPos absolutePos = targetPos.offset(blockData.relativePos);
                @SuppressWarnings("null")
                BlockState state = targetLevel.getBlockState(absolutePos);
                state.updateNeighbourShapes(targetLevel, absolutePos, 3);
                targetLevel.sendBlockUpdated(absolutePos, Blocks.AIR.defaultBlockState(), state, 3);
            }

            List<String> teleportedPlayers = new ArrayList<>();
            for (EntityData info : entitiesToTeleport) {
                BlockPos transformedRelEntityPos = transformPos(
                        new BlockPos((int) info.relX, (int) info.relY, (int) info.relZ), rotation, mirror, sourceSize);

                double dx = info.relX - (int) info.relX;
                double dz = info.relZ - (int) info.relZ;

                double finalRelX = transformedRelEntityPos.getX();
                double finalRelZ = transformedRelEntityPos.getZ();

                if (rotation == Rotation.CLOCKWISE_90) {
                    finalRelX += 1.0 - dz;
                    finalRelZ += dx;
                } else if (rotation == Rotation.CLOCKWISE_180) {
                    finalRelX += 1.0 - dx;
                    finalRelZ += 1.0 - dz;
                } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
                    finalRelX += dz;
                    finalRelZ += 1.0 - dx;
                } else {
                    finalRelX += dx;
                    finalRelZ += dz;
                }

                double tx = targetPos.getX() + finalRelX;
                double ty = targetPos.getY() + info.relY;
                double tz = targetPos.getZ() + finalRelZ;

                float yRot = info.entity.getYRot();
                yRot = mirrorRotation(mirror, yRot);
                yRot = (yRot + rotation.ordinal() * 90) % 360;

                if (info.entity instanceof ServerPlayer sp) {
                    if (targetLevel != selection.getWorld() && targetLevel instanceof ServerLevel sl) {
                        sp.teleportTo(sl, tx, ty, tz, yRot, sp.getXRot());
                    } else {
                        sp.connection.teleport(tx, ty, tz, yRot, sp.getXRot());
                    }
                    if (info.originalGameType != null) {
                        sp.setGameMode(info.originalGameType);
                    }
                } else if (info.entityData != null) {
                    if (targetLevel instanceof ServerLevel sl && info.entity.isRemoved()) {
                        info.entity.teleportTo(sl, tx, ty, tz, Set.of(), yRot, info.entity.getXRot());
                    } else {
                        if (targetLevel != selection.getWorld() && targetLevel instanceof ServerLevel sl) {
                            info.entity.changeDimension(sl);
                            info.entity.teleportTo(sl, tx, ty, tz, Set.of(), yRot, info.entity.getXRot());
                        } else {
                            info.entity.teleportTo(tx, ty, tz);
                            info.entity.setYRot(yRot);
                        }
                    }
                }
                if (info.playerName != null)
                    teleportedPlayers.add(info.playerName);
            }

            TeleportResult result = resultBuilder
                    .success(true)
                    .message("Teleportation complete (Async)")
                    .teleported(true)
                    .teleportedEntitiesCount(entitiesToTeleport.size())
                    .teleportedPlayerNames(teleportedPlayers)
                    .build();

            MinecraftForge.EVENT_BUS
                    .post(new StructureTeleportEvent.Post(selection, targetLevel, targetPos, player, result));
        }
    }

    public static class AsyncTeleportTask {
        private final java.util.BitSet validBlocks; // The mask from simulation
        private final Level targetLevel;
        private final Level sourceWorld;
        private final int blocksPerTick;
        // ... other fields ...

        // Iteration bounds
        private final int minX, minY, minZ;
        private final int maxX, maxY, maxZ;
        private final int width, height, depth;

        // Iteration state
        private int currentX, currentY, currentZ;
        private final int startX, startY, startZ;
        private final int endX, endY, endZ;
        private final int stepX, stepY, stepZ;
        private boolean completed = false;

        private final PasteMode pasteMode;
        private final List<BlockState> preservedBlocks;
        private final List<EntityData> entitiesToTeleport;
        private final Rotation rotation;
        private final Mirror mirror;
        private final Vec3i sourceSize;
        private final BlockPos targetPos;
        private final TeleportResult.Builder resultBuilder;
        private final Player player;
        private final Selection selection;
        private final Set<BlockPos> filter;
        private final List<BlockState> excludedBlocks;
        private final boolean checkExclusions;
        private final boolean includeAir;

        // Metrics for result
        private int replacedCount = 0;
        private int skippedCount = 0;
        private int skippedByLimitCount = 0;
        private int destinationSolidBlocksLost = 0;
        private TeleportResult finalResult;

        @SuppressWarnings("null")
        public AsyncTeleportTask(BlockPos targetPos, Level targetLevel,
                Level sourceWorld, PasteMode pasteMode, List<BlockState> preservedBlocks,
                int blocksPerTick, List<EntityData> entities, Rotation rotation,
                Mirror mirror, Vec3i sourceSize, TeleportResult.Builder resultBuilder,
                Player player, Selection selection, List<BlockState> excludedBlocks,
                boolean checkExclusions, Set<BlockPos> filter, boolean includeAir,
                java.util.BitSet validBlocks) {
            this.targetLevel = targetLevel;
            this.sourceWorld = sourceWorld;
            this.blocksPerTick = blocksPerTick;
            this.pasteMode = pasteMode;
            this.preservedBlocks = preservedBlocks;
            this.entitiesToTeleport = entities;
            this.rotation = rotation;
            this.mirror = mirror;
            this.sourceSize = sourceSize;
            this.targetPos = targetPos;
            this.resultBuilder = resultBuilder;
            this.player = player;
            this.selection = selection;
            this.filter = filter;
            this.excludedBlocks = excludedBlocks;
            this.checkExclusions = checkExclusions;
            this.includeAir = includeAir;
            this.validBlocks = validBlocks; // May be null if forced directly, handle graceful fallback?
            // Assuming non-null or treating null as "all valid" (or error) if requested.
            // For now, let's assume it's passed. If null, we might default to all 1s or
            // re-scan logic?
            // Given the prompt "consume the BitSet", we treat it as the map.

            BlockPos sourceMin = selection.getMin();
            BlockPos sourceMax = selection.getMax();
            this.minX = sourceMin.getX();
            this.minY = sourceMin.getY();
            this.minZ = sourceMin.getZ();
            this.maxX = sourceMax.getX();
            this.maxY = sourceMax.getY();
            this.maxZ = sourceMax.getZ();

            this.width = maxX - minX + 1;
            this.height = maxY - minY + 1;
            this.depth = maxZ - minZ + 1;

            // Determine direction based on overlap/offset
            int offsetX = targetPos.getX() - minX;
            int offsetY = targetPos.getY() - minY;
            int offsetZ = targetPos.getZ() - minZ;

            // Check for actual overlap
            // Source: [minX, maxX]
            // Target: [targetPos.getX(), targetPos.getX() + width - 1]
            boolean xOverlap = (minX <= targetPos.getX() + width - 1 && maxX >= targetPos.getX());
            boolean yOverlap = (minY <= targetPos.getY() + height - 1 && maxY >= targetPos.getY());
            boolean zOverlap = (minZ <= targetPos.getZ() + depth - 1 && maxZ >= targetPos.getZ());
            boolean overlaps = xOverlap && yOverlap && zOverlap;

            // If there is overlap, we MUST use directional iteration to prevent data loss.
            // If no overlap, we enforce Bottom-Up (Physics Safe) iteration.

            if (overlaps && offsetX > 0) {
                this.stepX = -1;
                this.startX = maxX;
                this.endX = minX - 1; // Exclusive bound
            } else {
                this.stepX = 1;
                this.startX = minX;
                this.endX = maxX + 1;
            }

            // Y Axis - Critical for Gravity blocks/dependencies
            // Only iterate Top-Down if we are moving Up AND we overlap.
            if (overlaps && offsetY > 0) {
                this.stepY = -1;
                this.startY = maxY;
                this.endY = minY - 1;
            } else {
                this.stepY = 1; // Bottom-Up (Standard)
                this.startY = minY;
                this.endY = maxY + 1;
            }

            if (overlaps && offsetZ > 0) {
                this.stepZ = -1;
                this.startZ = maxZ;
                this.endZ = minZ - 1;
            } else {
                this.stepZ = 1;
                this.startZ = minZ;
                this.endZ = maxZ + 1;
            }

            this.currentX = startX;
            this.currentY = startY;
            this.currentZ = startZ;

            TeleportAPI.LOGGER.info("[AsyncTeleport] BitSet Task created. Steps: " + stepX + "," + stepY + "," + stepZ);

            // Force load chunks at target location
            if (targetLevel instanceof ServerLevel serverLevel) {
                Set<ChunkPos> targetedChunks = new HashSet<>();
                BlockPos min = targetPos;
                @SuppressWarnings("null")
                BlockPos max = targetPos.offset(sourceSize);

                for (int x = min.getX(); x <= max.getX(); x += 16) {
                    for (int z = min.getZ(); z <= max.getZ(); z += 16) {
                        targetedChunks.add(new ChunkPos(x >> 4, z >> 4));
                    }
                }
                // Add edges
                targetedChunks.add(new ChunkPos(min));
                targetedChunks.add(new ChunkPos(max));

                for (ChunkPos chunkPos : targetedChunks) {
                    serverLevel.getChunkSource().addRegionTicket(TicketType.FORCED, chunkPos, 2, chunkPos);
                }
            }
        }

        public TeleportResult getResult() {
            return finalResult;
        }

        private int getIndex(int x, int y, int z) {
            int dx = x - minX;
            int dy = y - minY;
            int dz = z - minZ;
            return dx + width * (dy + height * dz);
        }

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || completed)
                return;

            processBatch(blocksPerTick);
        }

        public void processBatch(int count) {
            int actions = 0;

            while (actions < count && !completed) {
                // Process current block logic
                processCurrentBlock();
                actions++;

                // Advance Iterator
                currentX += stepX;
                if (currentX == endX) {
                    currentX = startX;
                    currentZ += stepZ;
                    if (currentZ == endZ) {
                        currentZ = startZ;
                        currentY += stepY;
                        if (currentY == endY) {
                            completed = true;
                        }
                    }
                }
            }

            if (completed) {
                onComplete();
                try {
                    MinecraftForge.EVENT_BUS.unregister(this);
                } catch (Exception e) {
                    // Ignore if not registered (Sync mode)
                }
            }
        }

        @SuppressWarnings("null")
        private void processCurrentBlock() {
            BlockPos srcPos = new BlockPos(currentX, currentY, currentZ);

            // BITSET LOGIC: Enforce simulation state
            // If includeAir=true, BitSet check is redundant (we teleport everything
            // including air)
            // Otherwise, BitSet determines what was valid during simulation
            if (validBlocks != null && !includeAir) {
                int idx = getIndex(currentX, currentY, currentZ);
                boolean wasValidDuringScan = validBlocks.get(idx);

                if (!wasValidDuringScan) {
                    // Block was AIR during simulation (bit=0)
                    // GAME DESIGN RULE: Force AIR at destination to match preview
                    BlockPos relativePos = srcPos.subtract(new BlockPos(minX, minY, minZ));
                    BlockPos transformedRelPos = transformPos(relativePos, rotation, mirror, sourceSize);
                    @SuppressWarnings("null")
                    BlockPos destPos = targetPos.offset(transformedRelPos);

                    // Place AIR if allowed by paste mode
                    @SuppressWarnings("null")
                    BlockState existingState = targetLevel.getBlockState(destPos);
                    if (shouldReplace(existingState, Blocks.AIR.defaultBlockState(), pasteMode, preservedBlocks)) {
                        targetLevel.setBlock(destPos, Blocks.AIR.defaultBlockState(), 16);
                    }
                    return; // Done with this position
                }
            }

            // Standard teleportation logic for valid blocks (bit=1)

            // Double check filter if provided (redundant if BitSet is accurate, but good
            // safety)
            if (filter != null && !filter.contains(srcPos))
                return;

            BlockState state = sourceWorld.getBlockState(srcPos);

            // Final check for air/exclusion even if BitSet said yes (e.g. state changed
            // since scan?)
            if (!includeAir && state.isAir())
                return;
            if (isExcluded(state, excludedBlocks, checkExclusions))
                return;

            // Logic to Move
            // 1. Capture
            CompoundTag nbt = null;
            BlockEntity blockEntity = sourceWorld.getBlockEntity(srcPos);
            if (blockEntity != null) {
                nbt = blockEntity.saveWithFullMetadata();
                for (String tag : DEFAULT_CLEANED_TAGS) {
                    nbt.remove(tag);
                }
            }

            // 2. Transform Destination
            BlockPos relativePos = srcPos.subtract(new BlockPos(minX, minY, minZ));
            BlockPos transformedRelPos = transformPos(relativePos, rotation, mirror, sourceSize);
            @SuppressWarnings({ "null", "deprecation" })
            BlockState transformedState = state.rotate(rotation).mirror(mirror);
            @SuppressWarnings("null")
            BlockPos destPos = targetPos.offset(transformedRelPos);

            // 3. Place at Dest
            // Chunk load check for Dest
            if (targetLevel instanceof ServerLevel sl) {
                // We could optimize this by caching current chunk, but simpler logic first
                @SuppressWarnings("null")
                ChunkPos cp = new ChunkPos(destPos);
                if (!sl.hasChunk(cp.x, cp.z)) {
                    sl.getChunkSource().addRegionTicket(TicketType.FORCED, cp, 2, cp);
                    sl.getChunk(cp.x, cp.z);
                }
            }

            // Place
            try {
                @SuppressWarnings("null")
                BlockState existingState = targetLevel.getBlockState(destPos);
                if (shouldReplace(existingState, transformedState, pasteMode, preservedBlocks)) {
                    // Flag 16 (0x10): PREVENT_NEIGHBOR_REACTIONS/UPDATE_KNOWN_SHAPE (Optimization)
                    // We use 16 to suppress client updates during bulk operations.
                    // The bulk refresh at the end handles visual updates.
                    targetLevel.setBlock(destPos, transformedState, 16);
                    if (nbt != null) {
                        @SuppressWarnings("null")
                        BlockEntity be = targetLevel.getBlockEntity(destPos);
                        if (be != null) {
                            CompoundTag tag = nbt.copy();
                            tag.putInt("x", destPos.getX());
                            tag.putInt("y", destPos.getY());
                            tag.putInt("z", destPos.getZ());
                            tag.remove("id");
                            try {
                                be.load(tag);
                                be.setChanged();
                            } catch (Exception e) {
                                TeleportAPI.LOGGER.error("[Async] Failed to load NBT for block at " + destPos, e);
                            }
                        }
                    }
                    replacedCount++;
                }
            } catch (Exception e) {
                TeleportAPI.LOGGER.error("[Async] Error placing block at " + destPos, e);
            }

            // 4. Clear Source (if different and not explicitly copying)
            // Teleport implies move.
            // We should check if source == dest and we just overwrote it?
            // With directional iteration, we are safe to clear `srcPos` even if it was a
            // destination for another block,
            // EXCEPT if we are moving WITHIN the same structure/area.
            // Wait. If source == dest, we shouldn't clear 'srcPos' if it now contains the
            // NEW block.
            // IF (srcPos.equals(destPos)) -> We just set it. Don't clear.
            // IF (srcPos != destPos) -> We can clear.
            // BUT: What if `srcPos` was the destination for a *previous* block?
            // `destPos` is where we wrote. `srcPos` is where read.
            // If we are iterating safely, `srcPos` should NOT have been written to yet by
            // another block if it's "behind" us?
            // Directional iteration prevents reading *corrupted* data (data that was
            // overwritten by a move).
            // It doesn't necessarily prevent clearing *new* data?
            // Actually, if we iterate "backwards", we move A -> B.
            // If B is `srcPos` for a later iteration?
            // Example: Move Right. A(0) -> B(1). B(1) -> C(2).
            // Order: Loop 1 (at 1): Move B->C. Clear B.
            // Order: Loop 2 (at 0): Move A->B. Clear A.
            // At Loop 1 using "Clear B": We clear B. That's fine, B is moved.
            // At Loop 2: We write to B. B is now A. We clear A.
            // Result: A->B, B->C. Correct.
            // So yes, we can clear `srcPos` immediately after moving, UNLESS srcPos ==
            // destPos.

            if (!srcPos.equals(destPos)) {
                // Flag 16 (0x10): PREVENT_NEIGHBOR_REACTIONS
                // 18 was (16 | 2) but 2 (UPDATE_CLIENTS) causes excessive lag.
                sourceWorld.setBlock(srcPos, Blocks.AIR.defaultBlockState(), 16);
            }
        }

        @SuppressWarnings("null")
        private void onComplete() {
            TeleportAPI.LOGGER.info("[AsyncTeleport] Block placement complete, teleporting entities");

            // Bulk Client Refresh (Optimization for Flag 16)
            // Bulk Client Refresh (Always execute for async teleport)
            if (targetLevel instanceof ServerLevel serverLevel) {
                Set<ChunkPos> affectedChunks = new HashSet<>();
                BlockPos min = targetPos;
                @SuppressWarnings("null")
                BlockPos max = targetPos.offset(sourceSize);
                for (int x = min.getX(); x <= max.getX(); x += 16) {
                    for (int z = min.getZ(); z <= max.getZ(); z += 16) {
                        affectedChunks.add(new ChunkPos(x >> 4, z >> 4));
                    }
                }
                affectedChunks.add(new ChunkPos(min.getX() >> 4, min.getZ() >> 4));
                affectedChunks.add(new ChunkPos(max.getX() >> 4, max.getZ() >> 4));

                for (ChunkPos chunkPos : affectedChunks) {
                    try {
                        net.minecraft.world.level.chunk.LevelChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                        serverLevel.getChunkSource().chunkMap.getPlayers(chunkPos, false).forEach(player -> {
                            player.connection.send(
                                    new net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket(chunk,
                                            serverLevel.getLightEngine(), null, null));
                        });
                    } catch (Exception e) {
                        TeleportAPI.LOGGER.error("Failed to send chunk packet", e);
                    }
                }
            }

            List<String> teleportedPlayers = new ArrayList<>();
            for (EntityData info : entitiesToTeleport) {
                BlockPos transformedRelEntityPos = transformPos(
                        new BlockPos((int) info.relX, (int) info.relY, (int) info.relZ), rotation, mirror, sourceSize);

                double dx = info.relX - (int) info.relX;
                double dz = info.relZ - (int) info.relZ;

                double finalRelX = transformedRelEntityPos.getX();
                double finalRelZ = transformedRelEntityPos.getZ();

                if (rotation == Rotation.CLOCKWISE_90) {
                    finalRelX += 1.0 - dz;
                    finalRelZ += dx;
                } else if (rotation == Rotation.CLOCKWISE_180) {
                    finalRelX += 1.0 - dx;
                    finalRelZ += 1.0 - dz;
                } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
                    finalRelX += dz;
                    finalRelZ += 1.0 - dx;
                } else {
                    finalRelX += dx;
                    finalRelZ += dz;
                }

                double tx = targetPos.getX() + finalRelX;
                double ty = targetPos.getY() + info.relY;
                double tz = targetPos.getZ() + finalRelZ;

                float yRot = info.entity.getYRot();
                yRot = mirrorRotation(mirror, yRot);
                yRot = (yRot + rotation.ordinal() * 90) % 360;

                if (info.entity instanceof ServerPlayer sp) {
                    if (targetLevel != sourceWorld && targetLevel instanceof ServerLevel sl) {
                        sp.teleportTo(sl, tx, ty, tz, yRot, sp.getXRot());
                    } else {
                        sp.connection.teleport(tx, ty, tz, yRot, sp.getXRot());
                    }
                    // Restore Gamemode
                    if (info.originalGameType != null) {
                        sp.setGameMode(info.originalGameType);
                    }
                } else if (info.entityData != null) {
                    // Respawn mob from NBT
                    if (targetLevel instanceof ServerLevel sl) {
                        // Update position in NBT
                        CompoundTag tag = info.entityData.copy();

                        // Need to update Pos list in NBT
                        ListTag posList = new ListTag();
                        posList.add(FloatTag.valueOf((float) tx));
                        posList.add(FloatTag.valueOf((float) ty));
                        posList.add(FloatTag.valueOf((float) tz));
                        tag.put("Pos", posList);
                        tag.remove("UUID"); // Remove UUID to generate a new one (safe clone)

                        Entity newEntity = EntityType.create(tag, sl).orElse(null);

                        if (newEntity != null) {
                            newEntity.moveTo(tx, ty, tz, yRot, info.entity.getXRot());
                            sl.addFreshEntity(newEntity);
                        } else {
                            TeleportAPI.LOGGER.warn("Failed to respawn entity: " + info.entity.getName().getString());
                        }
                    }
                }
                if (info.playerName != null)
                    teleportedPlayers.add(info.playerName);
            }

            finalResult = resultBuilder
                    .teleportedPlayerNames(teleportedPlayers)
                    .replacedBlockCount(replacedCount)
                    .skippedBlockCount(skippedCount)
                    .skippedByLimitCount(skippedByLimitCount)
                    .destinationSolidBlocksLost(destinationSolidBlocksLost)
                    .build();

            MinecraftForge.EVENT_BUS
                    .post(new StructureTeleportEvent.Post(selection, targetLevel, targetPos, player, finalResult));

            TeleportAPI.LOGGER.info("[AsyncTeleport] Task completed successfully");
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    // ========================================================================================================
    // REFACTORED API METHODS
    // ========================================================================================================

    /**
     * Clears a structure area with advanced masking and filtering options.
     * Prevents item drops by removing BlockEntities before blocks.
     */
    @SuppressWarnings("null")
    public static void clearAreaWithMask(Level world, Selection selection, java.util.BitSet validBlocksMask,
            List<BlockState> excludedBlocks, boolean checkExclusions, boolean includeAir, Set<BlockPos> filter) {

        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        int width = max.getX() - min.getX() + 1;
        int height = max.getY() - min.getY() + 1;

        // Combined Pass: Remove Block Entities and Set blocks to AIR (top-down)
        for (int y = max.getY(); y >= min.getY(); y--) { // Top to bottom!
            for (int x = min.getX(); x <= max.getX(); x++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (filter != null && !filter.contains(pos))
                        continue;

                    if (validBlocksMask != null) {
                        int index = (x - min.getX()) + width * ((y - min.getY()) + height * (z - min.getZ()));
                        if (!validBlocksMask.get(index)) {
                            continue;
                        }
                    }

                    BlockState state = world.getBlockState(pos);
                    if (state.isAir() && !includeAir)
                        continue;

                    if (!isExcluded(state, excludedBlocks, checkExclusions)) {
                        BlockEntity be = world.getBlockEntity(pos);
                        if (be != null) {
                            world.removeBlockEntity(pos);
                        }
                        // 2 = UPDATE_CLIENTS, 16 = NO_NEIGHBOR_UPDATE, 32 = NO_OBSERVER, 64 =
                        // UPDATE_INVISIBLE
                        world.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32 | 64);
                    }
                }
            }
        }

        // Notify neighbors OUTSIDE the source area (on the faces)
        notifyBoundingBoxNeighbors(world, min, max);
    }

    public static void notifyBoundingBoxNeighbors(Level world, BlockPos min, BlockPos max) {
        // X-axis faces (min and max X)
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                checkAndNotifyNeighbor(world, new BlockPos(min.getX() - 1, y, z), new BlockPos(min.getX(), y, z));
                checkAndNotifyNeighbor(world, new BlockPos(max.getX() + 1, y, z), new BlockPos(max.getX(), y, z));
            }
        }

        // Y-axis faces (min and max Y)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                checkAndNotifyNeighbor(world, new BlockPos(x, min.getY() - 1, z), new BlockPos(x, min.getY(), z));
                checkAndNotifyNeighbor(world, new BlockPos(x, max.getY() + 1, z), new BlockPos(x, max.getY(), z));
            }
        }

        // Z-axis faces (min and max Z)
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                checkAndNotifyNeighbor(world, new BlockPos(x, y, min.getZ() - 1), new BlockPos(x, y, min.getZ()));
                checkAndNotifyNeighbor(world, new BlockPos(x, y, max.getZ() + 1), new BlockPos(x, y, max.getZ()));
            }
        }
    }

    @SuppressWarnings("null")
    private static void checkAndNotifyNeighbor(Level world, BlockPos neighborPos, BlockPos sourcePos) {
        if (neighborPos.getY() >= world.getMinBuildHeight() && neighborPos.getY() < world.getMaxBuildHeight()) {
            BlockState neighborState = world.getBlockState(neighborPos);
            if (!neighborState.isAir()) {
                world.neighborChanged(neighborPos, Blocks.AIR, sourcePos);
                world.updateNeighborsAt(neighborPos, neighborState.getBlock());
                // Force block update for dependent blocks like portals
                neighborState.updateNeighbourShapes(world, neighborPos, 3);
                // Check if block can still survive (for torches, etc.)
                if (!neighborState.canSurvive(world, neighborPos)) {
                    world.destroyBlock(neighborPos, true);
                }
            }
        }
    }

    @SuppressWarnings("null")
    public static void teleportEntities(List<EntityData> entitiesToTeleport, Level targetLevel, BlockPos targetPos,
            Rotation rotation, Mirror mirror, Vec3i sourceSize, Level sourceWorld, List<String> teleportedPlayers) {

        for (EntityData info : entitiesToTeleport) {
            BlockPos transformedRelEntityPos = transformPos(
                    new BlockPos((int) info.relX, (int) info.relY, (int) info.relZ), rotation, mirror,
                    sourceSize);

            double dx = info.relX - (int) info.relX;
            double dz = info.relZ - (int) info.relZ;

            double finalRelX = transformedRelEntityPos.getX();
            double finalRelZ = transformedRelEntityPos.getZ();

            if (rotation == Rotation.CLOCKWISE_90) {
                finalRelX += 1.0 - dz;
                finalRelZ += dx;
            } else if (rotation == Rotation.CLOCKWISE_180) {
                finalRelX += 1.0 - dx;
                finalRelZ += 1.0 - dz;
            } else if (rotation == Rotation.COUNTERCLOCKWISE_90) {
                finalRelX += dz;
                finalRelZ += 1.0 - dx;
            } else {
                finalRelX += dx;
                finalRelZ += dz;
            }

            double tx = targetPos.getX() + finalRelX;
            double ty = targetPos.getY() + info.relY;
            double tz = targetPos.getZ() + finalRelZ;

            float yRot = info.entity.getYRot();
            yRot = mirrorRotation(mirror, yRot);
            yRot = (yRot + rotation.ordinal() * 90) % 360;

            if (info.entity instanceof ServerPlayer sp) {
                if (targetLevel != sourceWorld && targetLevel instanceof ServerLevel sl) {
                    sp.teleportTo(sl, tx, ty, tz, yRot, sp.getXRot());
                } else {
                    sp.connection.teleport(tx, ty, tz, yRot, sp.getXRot());
                }
                if (info.originalGameType != null) {
                    sp.setGameMode(info.originalGameType);
                }
            } else {
                if (targetLevel instanceof ServerLevel sl) {
                    Entity newEntity = info.entity;
                    boolean isCrossDimension = targetLevel != sourceWorld;

                    if (isCrossDimension) {
                        newEntity = info.entity.getType().create(targetLevel);
                        if (newEntity != null) {
                            newEntity.restoreFrom(info.entity);
                            info.entity.discard();
                        }
                    }

                    if (newEntity != null) {
                        newEntity.moveTo(tx, ty, tz, yRot, newEntity.getXRot());

                        if (isCrossDimension) {
                            if (info.entityData != null) {
                                CompoundTag cleanTag = info.entityData.copy();
                                cleanTag.remove("UUID");
                                newEntity.load(cleanTag);
                                newEntity.setPos(tx, ty, tz);
                            }
                            sl.addFreshEntity(newEntity);
                        } else {
                            if (info.entityData != null) {
                                CompoundTag cleanTag = info.entityData.copy();
                                cleanTag.remove("UUID");
                                newEntity.load(cleanTag);
                                newEntity.setPos(tx, ty, tz);
                            }
                            newEntity.setYRot(yRot);
                        }
                    }
                }
            }
            if (info.playerName != null && teleportedPlayers != null) {
                teleportedPlayers.add(info.playerName);
            }
        }
    }
}
