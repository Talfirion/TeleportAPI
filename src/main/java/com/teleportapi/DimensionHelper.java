package com.teleportapi;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for handling dimension discovery and management.
 * Supports both automatic discovery of registered dimensions (including modded
 * ones)
 * and manual configuration via a custom list.
 */
public class DimensionHelper {

    private static List<ResourceLocation> customDimensionList = null;

    /**
     * Sets a custom list of dimension IDs to use instead of automatic discovery.
     * Note: This list is primarily used by
     * {@link #getAvailableDimensions(MinecraftServer)}
     * for UI selection or filtering. It does not prevent teleportation to other
     * valid dimensions using direct IDs.
     * 
     * @param ids List of dimension ResourceLocations (e.g. "minecraft:overworld",
     *            "modid:dim_id")
     */
    public static void setCustomDimensionList(List<ResourceLocation> ids) {
        customDimensionList = ids;
    }

    /**
     * Gets all registered dimension keys from the server.
     * 
     * @param server The Minecraft server instance
     * @return Set of all dimension ResourceKeys
     */
    public static Set<ResourceKey<Level>> getAllDimensions(MinecraftServer server) {
        return server.levelKeys();
    }

    /**
     * Gets IDs of all registered dimensions on the server.
     * 
     * @param server The Minecraft server instance
     * @return List of dimension ResourceLocations
     */
    public static List<ResourceLocation> getDimensionIds(MinecraftServer server) {
        return getAllDimensions(server).stream()
                .map(ResourceKey::location)
                .sorted((a, b) -> a.toString().compareTo(b.toString()))
                .collect(Collectors.toList());
    }

    /**
     * Gets available dimensions according to the current configuration.
     * If a custom dimension list is set, it returns that list.
     * Otherwise, it returns all discovered dimensions from the server.
     * 
     * @param server The Minecraft server instance
     * @return List of available dimension ResourceLocations
     */
    public static List<ResourceLocation> getAvailableDimensions(MinecraftServer server) {
        if (customDimensionList != null && !customDimensionList.isEmpty()) {
            return customDimensionList;
        }
        return getDimensionIds(server);
    }

    /**
     * Utility method to get a ServerLevel from its ResourceLocation ID.
     * 
     * @param server The Minecraft server instance
     * @param id     Dimension ResourceLocation ID
     * @return The ServerLevel instance, or null if not found
     */
    @SuppressWarnings("null")
    public static ServerLevel getServerLevel(MinecraftServer server, ResourceLocation id) {
        @SuppressWarnings("null")
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        return server.getLevel(key);
    }

    /**
     * Checks if a dimension ID is valid and registered on the server.
     * 
     * @param server The Minecraft server instance
     * @param id     Dimension ResourceLocation ID to check
     * @return true if the dimension is registered
     */
    public static boolean isDimensionValid(MinecraftServer server, ResourceLocation id) {
        return getDimensionIds(server).contains(id);
    }
}
