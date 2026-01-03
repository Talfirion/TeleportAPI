package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A request object for teleportation, containing all necessary parameters and
 * flags.
 * Use the {@link Builder} to create instances.
 */
public class TeleportRequest {
    private final Selection selection;
    private final Level targetLevel;
    private final BlockPos targetPos;
    private final List<BlockState> excludedBlocks;
    private final List<BlockState> preservedBlocks;
    private final PasteMode pasteMode;
    private final boolean shouldTeleport;
    private final boolean checkExclusions;
    private final boolean includeAir;
    private final boolean teleportPlayers;
    private final boolean teleportEntities;
    @Nullable
    private final Player player;
    @Nullable
    private final Set<BlockPos> filter;
    private final Rotation rotation;
    private final Mirror mirror;

    private TeleportRequest(Builder builder) {
        this.selection = builder.selection;
        this.targetLevel = builder.targetLevel;
        this.targetPos = builder.targetPos;
        this.excludedBlocks = builder.excludedBlocks != null ? new ArrayList<>(builder.excludedBlocks) : null;
        this.preservedBlocks = builder.preservedBlocks != null ? new ArrayList<>(builder.preservedBlocks) : null;
        this.pasteMode = builder.pasteMode;
        this.shouldTeleport = builder.shouldTeleport;
        this.checkExclusions = builder.checkExclusions;
        this.includeAir = builder.includeAir;
        this.teleportPlayers = builder.teleportPlayers;
        this.teleportEntities = builder.teleportEntities;
        this.player = builder.player;
        this.filter = builder.filter != null ? new HashSet<>(builder.filter) : null;
        this.rotation = builder.rotation;
        this.mirror = builder.mirror;
    }

    public Selection getSelection() {
        return selection;
    }

    public Level getTargetLevel() {
        return targetLevel;
    }

    public BlockPos getTargetPos() {
        return targetPos;
    }

    public List<BlockState> getExcludedBlocks() {
        return excludedBlocks;
    }

    public List<BlockState> getPreservedBlocks() {
        return preservedBlocks;
    }

    public PasteMode getPasteMode() {
        return pasteMode;
    }

    public boolean shouldTeleport() {
        return shouldTeleport;
    }

    public boolean isCheckExclusions() {
        return checkExclusions;
    }

    public boolean isIncludeAir() {
        return includeAir;
    }

    public boolean isTeleportPlayers() {
        return teleportPlayers;
    }

    public boolean isTeleportEntities() {
        return teleportEntities;
    }

    @Nullable
    public Player getPlayer() {
        return player;
    }

    @Nullable
    public Set<BlockPos> getFilter() {
        return filter;
    }

    public Rotation getRotation() {
        return rotation;
    }

    public Mirror getMirror() {
        return mirror;
    }

    public static class Builder {
        private Selection selection;
        private Level targetLevel;
        private BlockPos targetPos;
        private List<BlockState> excludedBlocks;
        private List<BlockState> preservedBlocks;
        private PasteMode pasteMode = PasteMode.FORCE_REPLACE;
        private boolean shouldTeleport = true;
        private boolean checkExclusions = true;
        private boolean includeAir = true;
        private boolean teleportPlayers = true;
        private boolean teleportEntities = true;
        private Player player;
        private Set<BlockPos> filter;
        private Rotation rotation = Rotation.NONE;
        private Mirror mirror = Mirror.NONE;

        public Builder(Selection selection, BlockPos targetPos) {
            this.selection = selection;
            this.targetPos = targetPos;
            this.targetLevel = selection.getWorld();
        }

        public Builder targetLevel(Level targetLevel) {
            this.targetLevel = targetLevel;
            return this;
        }

        public Builder excludedBlocks(List<BlockState> excludedBlocks) {
            this.excludedBlocks = excludedBlocks;
            return this;
        }

        public Builder preservedBlocks(List<BlockState> preservedBlocks) {
            this.preservedBlocks = preservedBlocks;
            return this;
        }

        public Builder pasteMode(PasteMode pasteMode) {
            this.pasteMode = pasteMode;
            return this;
        }

        public Builder shouldTeleport(boolean shouldTeleport) {
            this.shouldTeleport = shouldTeleport;
            return this;
        }

        public Builder checkExclusions(boolean checkExclusions) {
            this.checkExclusions = checkExclusions;
            return this;
        }

        public Builder includeAir(boolean includeAir) {
            this.includeAir = includeAir;
            return this;
        }

        public Builder teleportPlayers(boolean teleportPlayers) {
            this.teleportPlayers = teleportPlayers;
            return this;
        }

        public Builder teleportEntities(boolean teleportEntities) {
            this.teleportEntities = teleportEntities;
            return this;
        }

        public Builder player(@Nullable Player player) {
            this.player = player;
            return this;
        }

        public Builder filter(@Nullable Set<BlockPos> filter) {
            this.filter = filter;
            return this;
        }

        public Builder rotation(Rotation rotation) {
            this.rotation = rotation;
            return this;
        }

        public Builder mirror(Mirror mirror) {
            this.mirror = mirror;
            return this;
        }

        public TeleportRequest build() {
            if (selection == null)
                throw new IllegalStateException("Selection must be provided");
            if (targetPos == null)
                throw new IllegalStateException("Target position must be provided");
            return new TeleportRequest(this);
        }
    }
}
