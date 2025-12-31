package com.teleportapi;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for storing teleportation results.
 * Contains information about success, total blocks, excluded blocks, their
 * types, and teleported entities.
 */
import java.util.ArrayList;
import java.util.List;

public class TeleportResult {
    private final boolean success;
    private final int totalBlocks;
    private final int excludedBlocks;
    private final Set<BlockState> excludedBlockTypes;
    private final String message;
    private final boolean teleported;
    private final int replacedBlockCount;
    private final int skippedBlockCount;
    private final int airBlockCount;
    private final int solidBlockCount;
    private final int destinationSolidBlocksLost;
    private final Map<BlockState, Integer> replacedBlocksMap;
    private final Map<BlockState, Integer> skippedBlocksMap;
    private final int skippedByLimitCount;
    private final int teleportedEntitiesCount;
    private final List<String> teleportedPlayerNames;

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported, 0, 0,
                0, 0, 0, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, 0, 0, totalBlocks - excludedBlocks, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, 0, 0, totalBlocks - excludedBlocks, 0,
                replacedBlocksMap, skippedBlocksMap, 0, new ArrayList<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount, int skippedByLimitCount,
            int airBlockCount, int solidBlockCount, int destinationSolidBlocksLost,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap,
            int teleportedEntitiesCount, List<String> teleportedPlayerNames) {
        this.success = success;
        this.totalBlocks = totalBlocks;
        this.excludedBlocks = excludedBlocks;
        this.excludedBlockTypes = new HashSet<>(excludedBlockTypes);
        this.message = message;
        this.teleported = teleported;
        this.replacedBlockCount = replacedBlockCount;
        this.skippedBlockCount = skippedBlockCount;
        this.skippedByLimitCount = skippedByLimitCount;
        this.airBlockCount = airBlockCount;
        this.solidBlockCount = solidBlockCount;
        this.destinationSolidBlocksLost = destinationSolidBlocksLost;
        this.replacedBlocksMap = replacedBlocksMap != null ? new HashMap<>(replacedBlocksMap) : new HashMap<>();
        this.skippedBlocksMap = skippedBlocksMap != null ? new HashMap<>(skippedBlocksMap) : new HashMap<>();
        this.teleportedEntitiesCount = teleportedEntitiesCount;
        this.teleportedPlayerNames = teleportedPlayerNames != null ? new ArrayList<>(teleportedPlayerNames)
                : new ArrayList<>();
    }

    public boolean isSuccess() {
        return success;
    }

    public int getTotalBlocks() {
        return totalBlocks;
    }

    public int getExcludedBlocks() {
        return excludedBlocks;
    }

    public Set<BlockState> getExcludedBlockTypes() {
        return Collections.unmodifiableSet(excludedBlockTypes);
    }

    public String getMessage() {
        return message;
    }

    public boolean isTeleported() {
        return teleported;
    }

    public int getTeleportedBlocks() {
        return totalBlocks - excludedBlocks;
    }

    public int getReplacedBlockCount() {
        return replacedBlockCount;
    }

    public int getSkippedBlockCount() {
        return skippedBlockCount;
    }

    public int getSkippedByLimitCount() {
        return skippedByLimitCount;
    }

    public int getAirBlockCount() {
        return airBlockCount;
    }

    public int getSolidBlockCount() {
        return solidBlockCount;
    }

    public int getDestinationSolidBlocksLost() {
        return destinationSolidBlocksLost;
    }

    public Map<BlockState, Integer> getReplacedBlocksMap() {
        return Collections.unmodifiableMap(replacedBlocksMap);
    }

    public Map<BlockState, Integer> getSkippedBlocksMap() {
        return Collections.unmodifiableMap(skippedBlocksMap);
    }

    public int getTeleportedEntitiesCount() {
        return teleportedEntitiesCount;
    }

    public List<String> getTeleportedPlayerNames() {
        return Collections.unmodifiableList(teleportedPlayerNames);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TeleportResult{");
        sb.append("success=").append(success);
        sb.append(", teleported=").append(teleported);
        sb.append(", totalBlocks=").append(totalBlocks);
        sb.append(", excludedBlocks=").append(excludedBlocks);
        sb.append(", teleportedBlocks=").append(getTeleportedBlocks());
        sb.append(", replacedBlocks=").append(replacedBlockCount);
        sb.append(", skippedBlocks=").append(skippedBlockCount);
        sb.append(", skippedByLimit=").append(skippedByLimitCount);
        sb.append(", entitiesTeleported=").append(teleportedEntitiesCount);
        if (!teleportedPlayerNames.isEmpty()) {
            sb.append(", players=").append(teleportedPlayerNames);
        }
        if (excludedBlocks > 0) {
            sb.append(", excludedBlockTypes=").append(excludedBlockTypes.size()).append(" types");
        }
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
