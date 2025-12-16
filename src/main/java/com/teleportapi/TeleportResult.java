package com.teleportapi;

import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Class for storing teleportation results.
 * Contains information about success, total blocks, excluded blocks, and their
 * types.
 */
public class TeleportResult {
    private final boolean success;
    private final int totalBlocks;
    private final int excludedBlocks;
    private final Set<BlockState> excludedBlockTypes;
    private final String message;
    private final boolean teleported;
    private final int replacedBlockCount;
    private final int skippedBlockCount;
    private final Map<BlockState, Integer> replacedBlocksMap;
    private final Map<BlockState, Integer> skippedBlocksMap;

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported, 0, 0,
                new HashMap<>(), new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, new HashMap<>(), new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap) {
        this.success = success;
        this.totalBlocks = totalBlocks;
        this.excludedBlocks = excludedBlocks;
        this.excludedBlockTypes = new HashSet<>(excludedBlockTypes);
        this.message = message;
        this.teleported = teleported;
        this.replacedBlockCount = replacedBlockCount;
        this.skippedBlockCount = skippedBlockCount;
        this.replacedBlocksMap = replacedBlocksMap != null ? new HashMap<>(replacedBlocksMap) : new HashMap<>();
        this.skippedBlocksMap = skippedBlocksMap != null ? new HashMap<>(skippedBlocksMap) : new HashMap<>();
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

    public Map<BlockState, Integer> getReplacedBlocksMap() {
        return Collections.unmodifiableMap(replacedBlocksMap);
    }

    public Map<BlockState, Integer> getSkippedBlocksMap() {
        return Collections.unmodifiableMap(skippedBlocksMap);
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
        if (excludedBlocks > 0) {
            sb.append(", excludedBlockTypes=").append(excludedBlockTypes.size()).append(" types");
        }
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
