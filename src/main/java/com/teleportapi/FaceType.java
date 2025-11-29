package com.teleportapi;

/**
 * Enum for defining the type of face of a cuboid.
 * Each face is perpendicular to one of the coordinate axes.
 */
public enum FaceType {
    X_MIN("Left face (min X)"), // Left face (perpendicular to X axis)
    X_MAX("Right face (max X)"), // Right face (perpendicular to X axis)
    Y_MIN("Bottom face (min Y)"), // Bottom face (perpendicular to Y axis)
    Y_MAX("Top face (max Y)"), // Top face (perpendicular to Y axis)
    Z_MIN("Front face (min Z)"), // Front face (perpendicular to Z axis)
    Z_MAX("Back face (max Z)"); // Back face (perpendicular to Z axis)

    private final String description;

    FaceType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Check if this is a minimum (MIN) face
     */
    public boolean isMinFace() {
        return this == X_MIN || this == Y_MIN || this == Z_MIN;
    }

    /**
     * Check if this is a maximum (MAX) face
     */
    public boolean isMaxFace() {
        return this == X_MAX || this == Y_MAX || this == Z_MAX;
    }

    /**
     * Get the axis to which this face is perpendicular
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
