package com.teleportapi.undo;

import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Manages undo history for players.
 */
public class UndoManager {
    private static final UndoManager INSTANCE = new UndoManager();
    private static final int MAX_HISTORY = 5; // Keep memory usage safe

    private final Map<UUID, Deque<UndoContext>> history = new HashMap<>();

    private UndoManager() {
    }

    public static UndoManager getInstance() {
        return INSTANCE;
    }

    public void push(Player player, UndoContext context) {
        if (player == null)
            return;
        history.computeIfAbsent(player.getUUID(), k -> new ArrayDeque<>()).push(context);

        // Enforce limit
        Deque<UndoContext> stack = history.get(player.getUUID());
        while (stack.size() > MAX_HISTORY) {
            stack.removeLast();
        }
    }

    public boolean undo(Player player) {
        if (player == null)
            return false;
        Deque<UndoContext> stack = history.get(player.getUUID());
        if (stack == null || stack.isEmpty())
            return false;

        UndoContext context = stack.pop();
        context.restore();
        return true;
    }

    public void clear(Player player) {
        if (player != null) {
            history.remove(player.getUUID());
        }
    }
}
