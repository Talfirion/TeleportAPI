package com.teleportapi;

/**
 * Enum для определения типа грани параллелепипеда.
 * Каждая грань перпендикулярна одной из осей координат.
 */
public enum FaceType {
    X_MIN("Левая грань (min X)"), // Левая грань (перпендикулярна оси X)
    X_MAX("Правая грань (max X)"), // Правая грань (перпендикулярна оси X)
    Y_MIN("Нижняя грань (min Y)"), // Нижняя грань (перпендикулярна оси Y)
    Y_MAX("Верхняя грань (max Y)"), // Верхняя грань (перпендикулярна оси Y)
    Z_MIN("Передняя грань (min Z)"), // Передняя грань (перпендикулярна оси Z)
    Z_MAX("Задняя грань (max Z)"); // Задняя грань (перпендикулярна оси Z)

    private final String description;

    FaceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Проверяет, является ли эта грань минимальной (MIN) гранью
     */
    public boolean isMinFace() {
        return this == X_MIN || this == Y_MIN || this == Z_MIN;
    }

    /**
     * Проверяет, является ли эта грань максимальной (MAX) гранью
     */
    public boolean isMaxFace() {
        return this == X_MAX || this == Y_MAX || this == Z_MAX;
    }

    /**
     * Получить ось, к которой перпендикулярна эта грань
     */
    public Axis getAxis() {
        if (this == X_MIN || this == X_MAX)
            return Axis.X;
        if (this == Y_MIN || this == Y_MAX)
            return Axis.Y;
        return Axis.Z;
    }

    public enum Axis {
        X, Y, Z
    }
}
