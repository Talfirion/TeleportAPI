package com.teleportapi.undo;

import com.teleportapi.StructureTeleporter;
import com.teleportapi.StructureTeleporter.BlockData;
import com.teleportapi.StructureTeleporter.EntityData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Stores the context of a teleportation action to allow for undo operations.
 */
public class UndoContext {
    private final Level sourceLevel;
    private final Level targetLevel;
    private final BlockPos sourceOrigin;
    private final BlockPos targetOrigin;
    private final List<BlockData> sourceSnapshot;
    private final List<BlockData> targetSnapshot;
    private final List<EntityData> entities;

    public UndoContext(Level sourceLevel, Level targetLevel, BlockPos sourceOrigin, BlockPos targetOrigin,
            List<BlockData> sourceSnapshot, List<BlockData> targetSnapshot, List<EntityData> entities) {
        this.sourceLevel = sourceLevel;
        this.targetLevel = targetLevel;
        this.sourceOrigin = sourceOrigin;
        this.targetOrigin = targetOrigin;
        this.sourceSnapshot = sourceSnapshot;
        this.targetSnapshot = targetSnapshot;
        this.entities = entities;
    }

    /**
     * Reverts the teleportation action.
     */
    public void restore() {
        // 1. Restore Target Area:
        if (targetSnapshot != null && !targetSnapshot.isEmpty()) {
            StructureTeleporter.pasteStructure(targetSnapshot, targetOrigin, targetLevel);
        }

        // 2. Restore Source Area:
        if (sourceSnapshot != null && !sourceSnapshot.isEmpty()) {
            StructureTeleporter.pasteStructure(sourceSnapshot, sourceOrigin, sourceLevel);
        }

        // 3. Teleport Entities back to Source:
        if (entities != null && !entities.isEmpty()) {
            // Move from targetLevel back to sourceLevel (sourceOrigin). Rotation/Mirror
            // NONE.
            StructureTeleporter.teleportEntities(entities, sourceLevel, sourceOrigin,
                    net.minecraft.world.level.block.Rotation.NONE,
                    net.minecraft.world.level.block.Mirror.NONE,
                    net.minecraft.core.Vec3i.ZERO,
                    targetLevel,
                    null);
        }
    }

    public Level getSourceLevel() {
        return sourceLevel;
    }

    public Level getTargetLevel() {
        return targetLevel;
    }
}
