package com.teleportapi.event;

import com.teleportapi.Selection;
import com.teleportapi.TeleportResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for all structure teleportation events.
 */
public abstract class StructureTeleportEvent extends Event {
    private final Selection selection;
    private final Level targetLevel;
    private final BlockPos targetPos;
    @Nullable
    private final Player player;

    protected StructureTeleportEvent(Selection selection, Level targetLevel, BlockPos targetPos,
            @Nullable Player player) {
        this.selection = selection;
        this.targetLevel = targetLevel;
        this.targetPos = targetPos;
        this.player = player;
    }

    public Selection getSelection() {
        return selection;
    }

    public Level getSourceLevel() {
        return selection.getWorld();
    }

    public Level getTargetLevel() {
        return targetLevel;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    /**
     * Fired before the teleportation begins.
     * If canceled, the teleportation will not occur.
     */
    @Cancelable
    public static class Pre extends StructureTeleportEvent {
        public Pre(Selection selection, Level targetLevel, BlockPos targetPos, @Nullable Player player) {
            super(selection, targetLevel, targetPos, player);
        }
    }

    /**
     * Fired after the teleportation is complete.
     */
    public static class Post extends StructureTeleportEvent {
        private final TeleportResult teleportResult;

        public Post(Selection selection, Level targetLevel, BlockPos targetPos, @Nullable Player player,
                TeleportResult result) {
            super(selection, targetLevel, targetPos, player);
            this.teleportResult = result;
        }

        public TeleportResult getTeleportResult() {
            return teleportResult;
        }
    }
}
