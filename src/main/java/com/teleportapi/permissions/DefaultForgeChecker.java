package com.teleportapi.permissions;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of IPermissionChecker that uses standard Forge events.
 * This ensures compatibility with most protection mods (WorldGuard,
 * GriefPrevention, FTB Chunks, etc.)
 * that listen to these events.
 */
public class DefaultForgeChecker implements IPermissionChecker {

    @SuppressWarnings("null")
    @Override
    public boolean canBreak(@Nullable Player player, Level level, BlockPos pos) {
        if (level.isClientSide)
            return true;

        Player effectivePlayer = getPlayerOrFake(player, level);
        BlockState state = level.getBlockState(pos);

        // Simulating the BreakEvent
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, effectivePlayer);
        return !MinecraftForge.EVENT_BUS.post(event);
    }

    @SuppressWarnings("null")
    @Override
    public boolean canPlace(@Nullable Player player, Level level, BlockPos pos) {
        if (level.isClientSide)
            return true;

        Player effectivePlayer = getPlayerOrFake(player, level);

        // Simulating the EntityPlaceEvent
        net.minecraftforge.common.util.BlockSnapshot snapshot = net.minecraftforge.common.util.BlockSnapshot
                .create(level.dimension(), level, pos);
        BlockEvent.EntityPlaceEvent event = new BlockEvent.EntityPlaceEvent(snapshot, level.getBlockState(pos),
                effectivePlayer);
        return !MinecraftForge.EVENT_BUS.post(event);
    }

    /**
     * If no player is provided (e.g., machine-triggered teleport), use a Forge
     * FakePlayer
     * to ensure events still have a valid actor for protection mods to check.
     */
    private Player getPlayerOrFake(@Nullable Player player, Level level) {
        if (player != null) {
            // If the player is in the same level, use the real player
            if (player.level() == level) {
                return player;
            }
            // If dimensions differ, create a fake player in the target level with the real
            // player's profile
            // This ensures protection mods check against the target world context
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                return FakePlayerFactory.get(serverLevel, player.getGameProfile());
            }
        }
        return FakePlayerFactory.getMinecraft((net.minecraft.server.level.ServerLevel) level);
    }

    @Override
    public int getPriority() {
        return -100; // Low priority, let specific mod checkers run first
    }
}
