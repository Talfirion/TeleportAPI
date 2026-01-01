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

    // Method for teleporting structure (remove from old location and paste in
    // new one)
    public static void teleportStructure(Selection selection, BlockPos targetPos) {
        teleportStructure(selection, selection.getWorld(), targetPos, null, true, true, PasteMode.FORCE_REPLACE, null,
                true, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport) {
        return teleportStructure(selection, selection.getWorld(), targetPos, excludedBlocks, shouldTeleport, true,
                PasteMode.FORCE_REPLACE, null, true, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions) {
        return teleportStructure(selection, selection.getWorld(), targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, PasteMode.FORCE_REPLACE, null, true, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport,
            boolean checkExclusions) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                PasteMode.FORCE_REPLACE, null, true, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, true, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, true, true, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir,
            boolean teleportPlayers, boolean teleportEntities) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, teleportPlayers, teleportEntities, null, null);
    }

    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir,
            boolean teleportPlayers, boolean teleportEntities, @org.jetbrains.annotations.Nullable Player player) {
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, teleportPlayers, teleportEntities, player, null);
    }

    public static TeleportResult teleportStructure(Selection selection,
            net.minecraft.resources.ResourceLocation targetDimId,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport) {
        if (selection.getWorld() == null || selection.getWorld().getServer() == null) {
            return new TeleportResult(false, 0, 0, new HashSet<>(), "Server or World is null", false);
        }

        ServerLevel targetLevel = DimensionHelper.getServerLevel(selection.getWorld().getServer(), targetDimId);
        if (targetLevel == null) {
            return new TeleportResult(false, 0, 0, new HashSet<>(), "Target dimension not found: " + targetDimId,
                    false);
        }

        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, true,
                PasteMode.FORCE_REPLACE, null, true, true, true, null, null);
    }

    /**
     * Main teleportation method with events support.
     */
    @SuppressWarnings("null")
    public static TeleportResult teleportStructure(Selection selection, Level targetLevel, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            PasteMode pasteMode, List<BlockState> preservedBlocks, boolean includeAir,
            boolean teleportPlayers, boolean teleportEntities, @org.jetbrains.annotations.Nullable Player player,
            @org.jetbrains.annotations.Nullable Set<BlockPos> coverageBlocks) { // Changed to Set<BlockPos>
        if (!selection.isComplete()) {
            TeleportAPI.LOGGER.warn("Selection not complete!");
            return new TeleportResult(false, 0, 0, new HashSet<>(),
                    "Selection not complete", false, 0, 0, 0, 0, 0, 0, null, null, 0, null);
        }

        Level sourceWorld = selection.getWorld();
        if (targetLevel == null) {
            targetLevel = sourceWorld;
        }
        BlockPos min = selection.getMin();
        BlockPos max = selection.getMax();

        // Coverage filter
        Set<BlockPos> enclosedPositions = null;
        if (coverageBlocks != null && !coverageBlocks.isEmpty()) {
            enclosedPositions = coverageBlocks; // Directly use the provided set
            if (enclosedPositions.isEmpty()) {
                TeleportAPI.LOGGER.warn("Teleportation denied: No blocks identified as part of a ship hull/interior.");
                return TeleportResult.failure(
                        "Teleportation denied: No blocks identified as part of a ship hull/interior.", 0, 0,
                        new HashSet<>(), 0, 0);
            }
        }

        // Count metrics
        int totalBlocks = 0;
        int excludedCount = 0;
        int replacedCount = 0;
        int skippedCount = 0;
        int skippedByLimitCount = 0;
        int airBlockCount = 0;
        int solidBlockCount = 0;
        int destinationSolidBlocksLost = 0;

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

                    // Apply enclosedPositions filter during counting
                    if (enclosedPositions != null && !enclosedPositions.contains(pos)) {
                        continue;
                    }

                    if (state.isAir()) {
                        if (includeAir) {
                            airBlockCount++;
                            totalBlocks++;
                        }
                        continue;
                    }
                    totalBlocks++;
                    if (isExcluded(state, excludedBlocks, checkExclusions)) {
                        excludedCount++;
                        excludedTypes.add(state);
                    } else {
                        solidBlockCount++;
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

                    // Apply enclosedPositions filter during counting
                    if (enclosedPositions != null && !enclosedPositions.contains(srcPos)) {
                        continue;
                    }

                    if (srcState.isAir() && !includeAir) {
                        continue;
                    }

                    if (!isExcluded(srcState, excludedBlocks, checkExclusions)) {
                        // Check target
                        BlockPos relPos = srcPos.subtract(min);
                        @SuppressWarnings("null")
                        BlockPos dstPos = targetPos.offset(relPos);

                        // Check building limits (height limit)
                        if (isOutsideHeightLimits(dstPos, targetLevel.getMinBuildHeight(),
                                targetLevel.getMaxBuildHeight())) {
                            skippedByLimitCount++;
                            continue; // Skip this block if it's out of bounds
                        }
                        @SuppressWarnings("null")
                        BlockState dstState = targetLevel.getBlockState(dstPos);

                        if (shouldReplace(dstState, srcState, pasteMode, preservedBlocks)) {
                            replacedCount++;
                            replacedBlocksMap.merge(dstState, 1, (a, b) -> a + b);
                            if (!dstState.isAir()) {
                                destinationSolidBlocksLost++;
                            }
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
            String entityPlayerName = null;
            if (entity instanceof Player p) {
                entityPlayerName = p.getName().getString();
            }

            // Calculate relative offset from selection min
            double relX = entity.getX() - min.getX();
            double relY = entity.getY() - min.getY();
            double relZ = entity.getZ() - min.getZ();

            entitiesToTeleport.add(new EntityData(entity, relX, relY, relZ, entityPlayerName));
        }

        // --- ACTIVE PERMISSION CHECKS ---

        // 1. Check if we can REMOVE blocks from source area
        CheckResult sourceCheck = PermissionHelper.checkAreaPermissions(player, sourceWorld, selection, true);

        if (!sourceCheck.isAllowed()) {
            String denyMsg = "Teleportation denied: No permission to break blocks at " + sourceCheck.getFailedPos()
                    + " (" + sourceCheck.getReason() + ")";
            TeleportAPI.LOGGER.warn(denyMsg);
            return TeleportResult.permissionDeny(denyMsg, totalBlocks, excludedCount, excludedTypes, airBlockCount,
                    solidBlockCount, sourceCheck.getFailedPos(), sourceCheck.getReason());
        }

        // 2. Check if we can PLACE blocks at destination area
        // We create a temporary selection representing the target area for checking
        Selection targetSelection = new Selection();
        targetSelection.setWorld(targetLevel);
        targetSelection.setFromCorners(targetPos, targetPos.offset(max.subtract(min)));

        CheckResult targetCheck = PermissionHelper.checkAreaPermissions(player, targetLevel, targetSelection, false);

        if (!targetCheck.isAllowed()) {
            String denyMsg = "Teleportation denied: No permission to place blocks at " + targetCheck.getFailedPos()
                    + " (" + targetCheck.getReason() + ")";
            TeleportAPI.LOGGER.warn(denyMsg);
            return TeleportResult.permissionDeny(denyMsg, totalBlocks, excludedCount, excludedTypes, airBlockCount,
                    solidBlockCount, targetCheck.getFailedPos(), targetCheck.getReason());
        }
        // --------------------------------

        // If shouldTeleport is false, just return the scan results
        if (!shouldTeleport) {
            double distance = Math.sqrt(min.distSqr(targetPos));
            String sourceDim = sourceWorld.dimension().location().toString();
            String targetDim = targetLevel.dimension().location().toString();

            message.append("Teleportation was not performed (shouldTeleport=false).");
            TeleportAPI.LOGGER.info(message.toString());
            return new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                    message.toString(), false, replacedCount, skippedCount, skippedByLimitCount,
                    airBlockCount, solidBlockCount, destinationSolidBlocksLost,
                    replacedBlocksMap, skippedBlocksMap,
                    entitiesToTeleport.size(), entitiesToTeleport.stream()
                            .filter(e -> e.playerName != null)
                            .map(e -> e.playerName)
                            .collect(Collectors.toList()),
                    false, null, null,
                    distance, sourceDim, targetDim);
        }

        // --- FIRE PRE EVENT ---
        StructureTeleportEvent.Pre preEvent = new StructureTeleportEvent.Pre(selection, targetLevel, targetPos, player);
        if (MinecraftForge.EVENT_BUS.post(preEvent)) {
            TeleportAPI.LOGGER.info("Teleportation canceled by event.");
            return TeleportResult.failure("Teleportation canceled by event", totalBlocks, excludedCount, excludedTypes,
                    airBlockCount, solidBlockCount);
        }
        // -----------------------

        // Third pass: actual teleportation
        // Use the filter during copy
        List<BlockData> blocksToMove = copyStructure(selection, excludedBlocks, checkExclusions, includeAir,
                enclosedPositions);

        if (blocksToMove == null || blocksToMove.isEmpty()) {
            return new TeleportResult(false, totalBlocks, excludedCount, excludedTypes,
                    "No blocks to teleport after exclusions", false, 0, 0, 0,
                    airBlockCount, solidBlockCount, 0,
                    new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());
        }

        // 4. Teleport entities
        List<String> teleportedPlayers = new ArrayList<>();
        try {
            @SuppressWarnings("unused")
            int removedCount = 0;
            // Iterate Top-to-Bottom to ensure dependent blocks are removed before their
            // support
            for (int y = max.getY(); y >= min.getY(); y--) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // Coverage filter check
                        if (enclosedPositions != null && !enclosedPositions.contains(pos)) {
                            continue;
                        }

                        BlockState state = sourceWorld.getBlockState(pos);

                        if (!isExcluded(state, excludedBlocks, checkExclusions)) {
                            /*
                             * We use flag 2 (UPDATE_CLIENTS), 16 (NO_NEIGHBOR_UPDATE), 32
                             * (PREVENT_NEIGHBOR_REACTIONS)
                             * and 64 (IS_MOVING) to prevent dependent blocks (redstone, torches, etc.)
                             * from dropping as items during the removal process while keeping the client in
                             * sync.
                             */
                            sourceWorld.setBlock(pos, Blocks.AIR.defaultBlockState(), 2 | 16 | 32 | 64);
                            removedCount++;
                        }
                    }
                }
            }

            for (int y = max.getY(); y >= min.getY(); y--) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    for (int z = min.getZ(); z <= max.getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);

                        // Coverage filter check
                        if (enclosedPositions != null && !enclosedPositions.contains(pos)) {
                            continue;
                        }

                        BlockState state = sourceWorld.getBlockState(pos);

                        // Update neighbors at the position (notifies neighbors that this block is now
                        // AIR)
                        sourceWorld.updateNeighborsAt(pos, state.getBlock());

                        // Aggressive shape update for neighbors
                        // flag 3 = 1 (UPDATE_NEIGHBORS) | 2 (UPDATE_CLIENTS)
                        state.updateNeighbourShapes(sourceWorld, pos, 3);
                    }
                }
            }

            // 3. Paste in new location
            pasteStructure(blocksToMove, targetPos, targetLevel, pasteMode, preservedBlocks);

            // 4. Teleport entities
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
        } catch (Exception e) {
            TeleportAPI.LOGGER.error("Teleportation failed! Attempting rollback. Error: " + e.getMessage(), e);

            // ROLLBACK: Restore original blocks to source location
            try {
                pasteStructure(blocksToMove, min, sourceWorld, PasteMode.FORCE_REPLACE, null);
                return TeleportResult.failure("Teleportation failed due to an error: " + e.getMessage()
                        + ". Rollback successful - original structure restored.", totalBlocks, excludedCount,
                        excludedTypes,
                        airBlockCount, solidBlockCount);
            } catch (Exception rollbackError) {
                TeleportAPI.LOGGER.error("ROLLBACK FAILED! Structure may be lost or fragmented! Error: "
                        + rollbackError.getMessage(), rollbackError);
                return TeleportResult.failure("CRITICAL ERROR: Teleportation failed and ROLLBACK ALSO FAILED! "
                        + "Error: " + e.getMessage() + ". Rollback Error: " + rollbackError.getMessage(), totalBlocks,
                        excludedCount, excludedTypes,
                        airBlockCount, solidBlockCount);
            }
        }

        if (!replacedBlocksMap.isEmpty()) {
            message.append("\nReplaced Blocks:");
            replacedBlocksMap.forEach((state, count) -> message.append("\n  - ")
                    .append(state.getBlock().getName().getString()).append(": ").append(count));
        }

        if (skippedByLimitCount > 0) {
            message.append("\nSkipped (Beyond Building Limit) Blocks: ").append(skippedByLimitCount);
        }

        TeleportAPI.LOGGER.info(message.toString());

        // Calculate distance
        double distance = Math.sqrt(min.distSqr(targetPos));
        String sourceDim = sourceWorld.dimension().location().toString();
        String targetDim = targetLevel.dimension().location().toString();

        TeleportResult result = new TeleportResult(true, totalBlocks, excludedCount, excludedTypes,
                message.toString(), true, replacedCount, skippedCount, skippedByLimitCount,
                airBlockCount, solidBlockCount, destinationSolidBlocksLost,
                replacedBlocksMap, skippedBlocksMap,
                entitiesToTeleport.size(), teleportedPlayers,
                false, null, null,
                distance, sourceDim, targetDim);

        // --- FIRE POST EVENT ---
        MinecraftForge.EVENT_BUS
                .post(new StructureTeleportEvent.Post(selection, targetLevel, targetPos, player, result));
        // -----------------------

        return result;
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
            // get list blocks for exclusions and preservations
            List<BlockState> excludedBlocks = getDefaultExcludedBlocks();
            List<BlockState> preservedBlocks = getDefaultPreservedBlocks();
            teleportByCorners(world, p1, p2, world, targetPos, excludedBlocks, true, true, true, true, true,
                    PasteMode.FORCE_REPLACE, preservedBlocks);
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
            // get list blocks for exclusions and preservations
            List<BlockState> excludedBlocks = getDefaultExcludedBlocks();
            List<BlockState> preservedBlocks = getDefaultPreservedBlocks();
            teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, true, true, true, true,
                    true, PasteMode.FORCE_REPLACE, preservedBlocks);
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
        return teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, includeAir, teleportPlayers, teleportEntities, PasteMode.FORCE_REPLACE, null);
    }

    public static TeleportResult teleportByCorners(Level sourceWorld, BlockPos p1, BlockPos p2,
            net.minecraft.resources.ResourceLocation targetDimId, BlockPos targetPos,
            List<BlockState> excludedBlocks, boolean shouldTeleport) {
        if (sourceWorld == null || sourceWorld.getServer() == null) {
            return new TeleportResult(false, 0, 0, new HashSet<>(), "Server or World is null", false);
        }

        ServerLevel targetLevel = DimensionHelper.getServerLevel(sourceWorld.getServer(), targetDimId);
        if (targetLevel == null) {
            return new TeleportResult(false, 0, 0, new HashSet<>(), "Target dimension not found: " + targetDimId,
                    false);
        }

        return teleportByCorners(sourceWorld, p1, p2, targetLevel, targetPos, excludedBlocks, shouldTeleport, true);
    }

    public static TeleportResult teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities, PasteMode pasteMode,
            List<BlockState> preservedBlocks) {
        return teleportByCorners(sourceLevel, p1, p2, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, includeAir, teleportPlayers, teleportEntities, pasteMode, preservedBlocks, null);
    }

    public static TeleportResult teleportByCorners(Level sourceLevel, BlockPos p1, BlockPos p2, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities, PasteMode pasteMode,
            List<BlockState> preservedBlocks, @org.jetbrains.annotations.Nullable Player player) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromCorners(p1, p2);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, teleportPlayers, teleportEntities, player);
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
        return teleportBySixPoints(sourceLevel, points, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, includeAir, teleportPlayers, teleportEntities, PasteMode.FORCE_REPLACE, null);
    }

    public static TeleportResult teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities, PasteMode pasteMode,
            List<BlockState> preservedBlocks) {
        return teleportBySixPoints(sourceLevel, points, targetLevel, targetPos, excludedBlocks, shouldTeleport,
                checkExclusions, includeAir, teleportPlayers, teleportEntities, pasteMode, preservedBlocks, null);
    }

    public static TeleportResult teleportBySixPoints(Level sourceLevel, BlockPos[] points, Level targetLevel,
            BlockPos targetPos, List<BlockState> excludedBlocks, boolean shouldTeleport, boolean checkExclusions,
            boolean includeAir, boolean teleportPlayers, boolean teleportEntities, PasteMode pasteMode,
            List<BlockState> preservedBlocks, @org.jetbrains.annotations.Nullable Player player) {
        Selection selection = new Selection();
        selection.setWorld(sourceLevel);
        selection.setFromSixPoints(points);
        return teleportStructure(selection, targetLevel, targetPos, excludedBlocks, shouldTeleport, checkExclusions,
                pasteMode, preservedBlocks, includeAir, teleportPlayers, teleportEntities, player);
    }
}