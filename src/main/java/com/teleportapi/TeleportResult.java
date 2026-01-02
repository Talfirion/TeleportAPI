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
    private final boolean permissionDenied;
    private final net.minecraft.core.BlockPos failedPos;
    private final String denialReason;
    private final double distance;
    private final String sourceDimension;
    private final String targetDimension;
    private final Map<BlockState, Integer> sourceBlockCounts;

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported, 0, 0,
                0, 0, 0, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>(), false, null, null, 0.0, "", "",
                new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, 0, 0, totalBlocks - excludedBlocks, 0,
                new HashMap<>(), new HashMap<>(), 0, new ArrayList<>(), false, null, null, 0.0, "", "",
                new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, 0, 0, totalBlocks - excludedBlocks, 0,
                replacedBlocksMap, skippedBlocksMap, 0, new ArrayList<>(), false, null, null, 0.0, "", "",
                new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount, int skippedByLimitCount,
            int airBlockCount, int solidBlockCount, int destinationSolidBlocksLost,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap,
            int teleportedEntitiesCount, List<String> teleportedPlayerNames) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, skippedByLimitCount, airBlockCount,
                solidBlockCount, destinationSolidBlocksLost, replacedBlocksMap, skippedBlocksMap,
                teleportedEntitiesCount, teleportedPlayerNames, false, null, null, 0.0, "", "", new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount, int skippedByLimitCount,
            int airBlockCount, int solidBlockCount, int destinationSolidBlocksLost,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap,
            int teleportedEntitiesCount, List<String> teleportedPlayerNames,
            boolean permissionDenied, net.minecraft.core.BlockPos failedPos, String denialReason) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, skippedByLimitCount, airBlockCount,
                solidBlockCount, destinationSolidBlocksLost, replacedBlocksMap, skippedBlocksMap,
                teleportedEntitiesCount, teleportedPlayerNames, permissionDenied, failedPos, denialReason, 0.0, "", "",
                new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount, int skippedByLimitCount,
            int airBlockCount, int solidBlockCount, int destinationSolidBlocksLost,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap,
            int teleportedEntitiesCount, List<String> teleportedPlayerNames,
            boolean permissionDenied, net.minecraft.core.BlockPos failedPos, String denialReason,
            double distance, String sourceDimension, String targetDimension) {
        this(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                replacedBlockCount, skippedBlockCount, skippedByLimitCount, airBlockCount,
                solidBlockCount, destinationSolidBlocksLost, replacedBlocksMap, skippedBlocksMap,
                teleportedEntitiesCount, teleportedPlayerNames, permissionDenied, failedPos, denialReason,
                distance, sourceDimension, targetDimension, new HashMap<>());
    }

    public TeleportResult(boolean success, int totalBlocks, int excludedBlocks,
            Set<BlockState> excludedBlockTypes, String message, boolean teleported,
            int replacedBlockCount, int skippedBlockCount, int skippedByLimitCount,
            int airBlockCount, int solidBlockCount, int destinationSolidBlocksLost,
            Map<BlockState, Integer> replacedBlocksMap, Map<BlockState, Integer> skippedBlocksMap,
            int teleportedEntitiesCount, List<String> teleportedPlayerNames,
            boolean permissionDenied, net.minecraft.core.BlockPos failedPos, String denialReason,
            double distance, String sourceDimension, String targetDimension,
            Map<BlockState, Integer> sourceBlockCounts) {
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
        this.permissionDenied = permissionDenied;
        this.failedPos = failedPos;
        this.denialReason = denialReason;
        this.distance = distance;
        this.sourceDimension = sourceDimension;
        this.targetDimension = targetDimension;
        this.sourceBlockCounts = sourceBlockCounts != null ? new HashMap<>(sourceBlockCounts) : new HashMap<>();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success;
        private int totalBlocks;
        private int excludedBlocks;
        private Set<BlockState> excludedBlockTypes = new HashSet<>();
        private String message = "";
        private boolean teleported;
        private int replacedBlockCount;
        private int skippedBlockCount;
        private int skippedByLimitCount;
        private int airBlockCount;
        private int solidBlockCount;
        private int destinationSolidBlocksLost;
        private Map<BlockState, Integer> replacedBlocksMap = new HashMap<>();
        private Map<BlockState, Integer> skippedBlocksMap = new HashMap<>();
        private int teleportedEntitiesCount;
        private List<String> teleportedPlayerNames = new ArrayList<>();
        private boolean permissionDenied;
        private net.minecraft.core.BlockPos failedPos;
        private String denialReason;
        private double distance;
        private String sourceDimension = "";
        private String targetDimension = "";
        private Map<BlockState, Integer> sourceBlockCounts = new HashMap<>();

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder totalBlocks(int totalBlocks) {
            this.totalBlocks = totalBlocks;
            return this;
        }

        public Builder excludedBlocks(int excludedBlocks) {
            this.excludedBlocks = excludedBlocks;
            return this;
        }

        public Builder excludedBlockTypes(Set<BlockState> types) {
            this.excludedBlockTypes = types;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder teleported(boolean teleported) {
            this.teleported = teleported;
            return this;
        }

        public Builder replacedBlockCount(int count) {
            this.replacedBlockCount = count;
            return this;
        }

        public Builder skippedBlockCount(int count) {
            this.skippedBlockCount = count;
            return this;
        }

        public Builder skippedByLimitCount(int count) {
            this.skippedByLimitCount = count;
            return this;
        }

        public Builder airBlockCount(int count) {
            this.airBlockCount = count;
            return this;
        }

        public Builder solidBlockCount(int count) {
            this.solidBlockCount = count;
            return this;
        }

        public Builder destinationSolidBlocksLost(int count) {
            this.destinationSolidBlocksLost = count;
            return this;
        }

        public Builder replacedBlocksMap(Map<BlockState, Integer> map) {
            this.replacedBlocksMap = map;
            return this;
        }

        public Builder skippedBlocksMap(Map<BlockState, Integer> map) {
            this.skippedBlocksMap = map;
            return this;
        }

        public Builder teleportedEntitiesCount(int count) {
            this.teleportedEntitiesCount = count;
            return this;
        }

        public Builder teleportedPlayerNames(List<String> names) {
            this.teleportedPlayerNames = names;
            return this;
        }

        public Builder permissionDenied(boolean denied) {
            this.permissionDenied = denied;
            return this;
        }

        public Builder failedPos(net.minecraft.core.BlockPos pos) {
            this.failedPos = pos;
            return this;
        }

        public Builder denialReason(String reason) {
            this.denialReason = reason;
            return this;
        }

        public Builder distance(double distance) {
            this.distance = distance;
            return this;
        }

        public Builder sourceDimension(String dim) {
            this.sourceDimension = dim;
            return this;
        }

        public Builder targetDimension(String dim) {
            this.targetDimension = dim;
            return this;
        }

        public Builder sourceBlockCounts(Map<BlockState, Integer> counts) {
            this.sourceBlockCounts = counts;
            return this;
        }

        public TeleportResult build() {
            return new TeleportResult(success, totalBlocks, excludedBlocks, excludedBlockTypes, message, teleported,
                    replacedBlockCount, skippedBlockCount, skippedByLimitCount, airBlockCount, solidBlockCount,
                    destinationSolidBlocksLost, replacedBlocksMap, skippedBlocksMap, teleportedEntitiesCount,
                    teleportedPlayerNames, permissionDenied, failedPos, denialReason, distance, sourceDimension,
                    targetDimension, sourceBlockCounts);
        }
    }

    public static TeleportResult permissionDeny(String message, int totalBlocks, int excludedCount,
            Set<BlockState> excludedTypes, int airCount, int solidCount,
            net.minecraft.core.BlockPos failedPos, String denialReason) {
        return new TeleportResult(false, totalBlocks, excludedCount, excludedTypes, message, false, 0, 0, 0, airCount,
                solidCount, 0, new HashMap<>(), new HashMap<>(), 0, new ArrayList<>(),
                true, failedPos, denialReason, 0.0, "", "");
    }

    public static TeleportResult failure(String message, int totalBlocks, int excludedCount,
            Set<BlockState> excludedTypes,
            int airCount, int solidCount) {
        return new TeleportResult(false, totalBlocks, excludedCount, excludedTypes, message, false, 0, 0, 0, airCount,
                solidCount, 0, new HashMap<>(), new HashMap<>(), 0, new ArrayList<>());
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

    public Map<BlockState, Integer> getSourceBlockCounts() {
        return Collections.unmodifiableMap(sourceBlockCounts);
    }

    public int getTeleportedEntitiesCount() {
        return teleportedEntitiesCount;
    }

    public List<String> getTeleportedPlayerNames() {
        return Collections.unmodifiableList(teleportedPlayerNames);
    }

    public boolean isPermissionDenied() {
        return permissionDenied;
    }

    public net.minecraft.core.BlockPos getFailedPos() {
        return failedPos;
    }

    public String getDenialReason() {
        return denialReason;
    }

    public double getDistance() {
        return distance;
    }

    public String getSourceDimension() {
        return sourceDimension;
    }

    public String getTargetDimension() {
        return targetDimension;
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
        sb.append(", distance=").append(distance);
        sb.append(", sourceDim='").append(sourceDimension).append('\'');
        sb.append(", targetDim='").append(targetDimension).append('\'');
        if (!teleportedPlayerNames.isEmpty()) {
            sb.append(", players=").append(teleportedPlayerNames);
        }
        if (excludedBlocks > 0) {
            sb.append(", excludedBlockTypes=").append(excludedBlockTypes.size()).append(" types");
        }
        if (!sourceBlockCounts.isEmpty()) {
            sb.append(", blockCounts=").append(sourceBlockCounts.size()).append(" types");
        }
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
