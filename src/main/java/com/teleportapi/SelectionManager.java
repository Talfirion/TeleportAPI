package com.teleportapi;

import net.minecraft.world.entity.player.Player;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// This class manages selections for each player
public class SelectionManager {
    // Static instance (Singleton pattern - only one object for the entire mod)
    // Volatile ensures visibility across threads
    private static volatile SelectionManager instance;

    // Map (dictionary) for storing each player's selections
    // Key - player UUID, value - their Selection
    // Using ConcurrentHashMap for thread-safe operations in multiplayer
    private final Map<UUID, Selection> selections;

    // Private constructor (so it cannot be created from outside)
    private SelectionManager() {
        selections = new ConcurrentHashMap<>();
    }

    // Get the single instance of the manager
    // Double-checked locking for thread-safe lazy initialization
    public static SelectionManager getInstance() {
        if (instance == null) {
            synchronized (SelectionManager.class) {
                if (instance == null) {
                    instance = new SelectionManager();
                }
            }
        }
        return instance;
    }

    // Get player's selection (or create a new one if it doesn't exist)
    // ConcurrentHashMap.computeIfAbsent is thread-safe
    public Selection getSelection(Player player) {
        UUID playerId = player.getUUID();
        return selections.computeIfAbsent(playerId, k -> new Selection());
    }

    // Clear player's selection
    public void clearSelection(Player player) {
        selections.remove(player.getUUID());
    }
}
