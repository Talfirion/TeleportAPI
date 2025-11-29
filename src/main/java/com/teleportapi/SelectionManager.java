package com.teleportapi;

import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// This class manages selections for each player
public class SelectionManager {
    // Static instance (Singleton pattern - only one object for the entire mod)
    private static SelectionManager instance;

    // Map (dictionary) for storing each player's selections
    // Key - player UUID, value - their Selection
    private Map<UUID, Selection> selections;

    // Private constructor (so it cannot be created from outside)
    private SelectionManager() {
        selections = new HashMap<>();
    }

    // Get the single instance of the manager
    public static SelectionManager getInstance() {
        if (instance == null) {
            instance = new SelectionManager();
        }
        return instance;
    }

    // Get player's selection (or create a new one if it doesn't exist)
    public Selection getSelection(Player player) {
        UUID playerId = player.getUUID();

        // If player doesn't have a selection yet, create a new one
        if (!selections.containsKey(playerId)) {
            selections.put(playerId, new Selection());
        }

        return selections.get(playerId);
    }

    // Clear player's selection
    public void clearSelection(Player player) {
        selections.remove(player.getUUID());
    }
}
