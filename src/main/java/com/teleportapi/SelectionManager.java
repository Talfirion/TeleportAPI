package com.teleportapi;

import net.minecraft.world.entity.player.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// Этот класс управляет выделениями для каждого игрока
public class SelectionManager {
    // Статический экземпляр (паттерн Singleton - только один объект на весь мод)
    private static SelectionManager instance;
    
    // Map (словарь) для хранения выделений каждого игрока
    // Ключ - UUID игрока, значение - его Selection
    private Map<UUID, Selection> selections;
    
    // Приватный конструктор (чтобы нельзя было создать извне)
    private SelectionManager() {
        selections = new HashMap<>();
    }
    
    // Получить единственный экземпляр менеджера
    public static SelectionManager getInstance() {
        if (instance == null) {
            instance = new SelectionManager();
        }
        return instance;
    }
    
    // Получить выделение игрока (или создать новое, если его нет)
    public Selection getSelection(Player player) {
        UUID playerId = player.getUUID();
        
        // Если у игрока еще нет выделения, создаем новое
        if (!selections.containsKey(playerId)) {
            selections.put(playerId, new Selection());
        }
        
        return selections.get(playerId);
    }
    
    // Очистить выделение игрока
    public void clearSelection(Player player) {
        selections.remove(player.getUUID());
    }
}
