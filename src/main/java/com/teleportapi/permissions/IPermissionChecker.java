package com.teleportapi.permissions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for checking if a player has permission to perform actions in a
 * specific location.
 */
public interface IPermissionChecker {
    /**
     * Check if the player can break (remove) a block at the specified position.
     * 
     * @param player The player performing the action.
     * @param level  The level where the action occurs.
     * @param pos    The position of the block.
     * @return True if allowed, false if denied.
     */
    boolean canBreak(@Nullable Player player, Level level, BlockPos pos);

    /**
     * Check if the player can place (add/replace) a block at the specified
     * position.
     * 
     * @param player The player performing the action.
     * @param level  The level where the action occurs.
     * @param pos    The position of the block.
     * @return True if allowed, false if denied.
     */
    boolean canPlace(@Nullable Player player, Level level, BlockPos pos);

    /**
     * Priority of this checker. Higher values are checked first.
     */
    default int getPriority() {
        return 0;
    }
}
