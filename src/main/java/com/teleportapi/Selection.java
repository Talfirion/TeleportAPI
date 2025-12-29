package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * Class for storing information about a selected region.
 * The region is defined by 6 points in space - one for each face
 * of the cuboid.
 * For axis-aligned box, each point defines the position of a face along
 * the corresponding axis.
 * 
 * Internal Logic:
 * - Stores 6 face points (one for each side of a cube)
 * - Each face is identified by FaceType enum
 * - Automatically calculates min/max bounding box when using
 * setFromPoints/setFromCorners
 * - Validates that min <= max for each axis
 */
public class Selection {
    // Storage for cuboid face points
    private Map<FaceType, BlockPos> facePoints = new HashMap<>();

    // World in which the selection was made
    private Level world;

    /**
     * Set a point for a specific face of the cuboid
     * 
     * @param faceType face type (X_MIN, X_MAX, Y_MIN, Y_MAX, Z_MIN, Z_MAX)
     * @param point    point in space through which the face passes
     */
    public void setFacePoint(FaceType faceType, BlockPos point) {
        facePoints.put(faceType, point);
        validateCoordinates();
    }

    /**
     * Get the point for a specific face
     */
    public BlockPos getFacePoint(FaceType faceType) {
        return facePoints.get(faceType);
    }

    /**
     * Remove the point for a specific face
     */
    public void removeFacePoint(FaceType faceType) {
        facePoints.remove(faceType);
    }

    // Getters for face coordinates (for compatibility)

    public Integer getXMin() {
        BlockPos point = facePoints.get(FaceType.X_MIN);
        return point != null ? point.getX() : null;
    }

    public Integer getXMax() {
        BlockPos point = facePoints.get(FaceType.X_MAX);
        return point != null ? point.getX() : null;
    }

    public Integer getYMin() {
        BlockPos point = facePoints.get(FaceType.Y_MIN);
        return point != null ? point.getY() : null;
    }

    public Integer getYMax() {
        BlockPos point = facePoints.get(FaceType.Y_MAX);
        return point != null ? point.getY() : null;
    }

    public Integer getZMin() {
        BlockPos point = facePoints.get(FaceType.Z_MIN);
        return point != null ? point.getZ() : null;
    }

    public Integer getZMax() {
        BlockPos point = facePoints.get(FaceType.Z_MAX);
        return point != null ? point.getZ() : null;
    }

    // Setters for convenience (create points with zero unused coordinates)

    public void setXMin(int x) {
        setFacePoint(FaceType.X_MIN, new BlockPos(x, 0, 0));
    }

    public void setXMax(int x) {
        setFacePoint(FaceType.X_MAX, new BlockPos(x, 0, 0));
    }

    public void setYMin(int y) {
        setFacePoint(FaceType.Y_MIN, new BlockPos(0, y, 0));
    }

    public void setYMax(int y) {
        setFacePoint(FaceType.Y_MAX, new BlockPos(0, y, 0));
    }

    public void setZMin(int z) {
        setFacePoint(FaceType.Z_MIN, new BlockPos(0, 0, z));
    }

    public void setZMax(int z) {
        setFacePoint(FaceType.Z_MAX, new BlockPos(0, 0, z));
    }

    public Level getWorld() {
        return world;
    }

    public void setWorld(Level world) {
        this.world = world;
    }

    // Coordinate validation - check that min <= max for each axis
    private void validateCoordinates() {
        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        if (xMin != null && xMax != null && xMin > xMax) {
            TeleportAPI.LOGGER.warn("Warning: xMin (" + xMin + ") > xMax (" + xMax + ")!");
        }
        if (yMin != null && yMax != null && yMin > yMax) {
            TeleportAPI.LOGGER.warn("Warning: yMin (" + yMin + ") > yMax (" + yMax + ")!");
        }
        if (zMin != null && zMax != null && zMin > zMax) {
            TeleportAPI.LOGGER.warn("Warning: zMin (" + zMin + ") > zMax (" + zMax + ")!");
        }
    }

    // Check that all sides are set
    public boolean isComplete() {
        return facePoints.size() == 6 && world != null;
    }

    // Get the minimum point (bottom left front corner)
    public BlockPos getMin() {
        if (!isComplete())
            return null;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        return new BlockPos(
                Math.min(xMin, xMax),
                Math.min(yMin, yMax),
                Math.min(zMin, zMax));
    }

    // Get the maximum point (top right back corner)
    public BlockPos getMax() {
        if (!isComplete())
            return null;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        return new BlockPos(
                Math.max(xMin, xMax),
                Math.max(yMin, yMax),
                Math.max(zMin, zMax));
    }

    // Get the size of the selected region (volume in blocks)
    public int getVolume() {
        if (!isComplete())
            return 0;

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        int sizeX = Math.abs(xMax - xMin) + 1;
        int sizeY = Math.abs(yMax - yMin) + 1;
        int sizeZ = Math.abs(zMax - zMin) + 1;

        return sizeX * sizeY * sizeZ;
    }

    // Method for displaying information about the selection
    public String getInfo() {
        if (!isComplete()) {
            return "Selection incomplete. Faces set: " + facePoints.size() + "/6";
        }

        Integer xMin = getXMin();
        Integer xMax = getXMax();
        Integer yMin = getYMin();
        Integer yMax = getYMax();
        Integer zMin = getZMin();
        Integer zMax = getZMax();

        StringBuilder info = new StringBuilder();
        info.append(String.format("Selection: X[%d..%d] Y[%d..%d] Z[%d..%d], Volume: %d blocks\n",
                xMin, xMax, yMin, yMax, zMin, zMax, getVolume()));

        info.append("Face points:\n");
        for (FaceType faceType : FaceType.values()) {
            BlockPos point = facePoints.get(faceType);
            if (point != null) {
                info.append(String.format("  %s: %s\n", faceType.getDescription(), point.toShortString()));
            }
        }

        return info.toString();
    }

    // Method to reset all sides
    public void reset() {
        facePoints.clear();
        world = null;
    }

    /**
     * Get the number of faces set
     */
    public int getSetFacesCount() {
        return facePoints.size();
    }

    /**
     * Set selection from arbitrary points in space.
     * The method automatically calculates minimum and maximum coordinates,
     * and forms an axis-aligned bounding box (cuboid).
     * 
     * @param points array of points (usually 6 points, but can be any number)
     * @throws IllegalArgumentException if array is empty or null
     */
    public void setFromPoints(BlockPos... points) {
        // Internal Logic:
        // 1. Iterates through ALL provided points
        // 2. For each coordinate axis, finds minimum and maximum
        // 3. Creates a bounding box that contains all points
        if (points == null || points.length == 0) {
            throw new IllegalArgumentException("Points array cannot be empty!");
        }

        // Initialize min/max with first point
        int minX = points[0].getX();
        int maxX = points[0].getX();
        int minY = points[0].getY();
        int maxY = points[0].getY();
        int minZ = points[0].getZ();
        int maxZ = points[0].getZ();

        // Iterate through all remaining points and find minimums/maximums
        for (int i = 1; i < points.length; i++) {
            BlockPos point = points[i];

            if (point.getX() < minX)
                minX = point.getX();
            if (point.getX() > maxX)
                maxX = point.getX();

            if (point.getY() < minY)
                minY = point.getY();
            if (point.getY() > maxY)
                maxY = point.getY();

            if (point.getZ() < minZ)
                minZ = point.getZ();
            if (point.getZ() > maxZ)
                maxZ = point.getZ();
        }

        // Set faces with found coordinates
        // Use the points themselves to store complete information
        setFacePoint(FaceType.X_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.X_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Y_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Y_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Z_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Z_MAX, new BlockPos(maxX, maxY, maxZ));

        TeleportAPI.LOGGER.info(String.format(
                "Built cuboid from %d points: X[%d..%d] Y[%d..%d] Z[%d..%d]",
                points.length, minX, maxX, minY, maxY, minZ, maxZ));
    }

    /**
     * Set selection from two points (corners).
     * This is the classic way of selecting a region (like in WorldEdit).
     * 
     * @param p1 First point
     * @param p2 Second point
     */
    public void setFromCorners(BlockPos p1, BlockPos p2) {
        // Internal Logic:
        // 1. Accepts two diagonal corners of the cuboid (order independent)
        // 2. Compares coordinates to determine X_MIN/MAX, Y_MIN/MAX, Z_MIN/MAX
        if (p1 == null || p2 == null) {
            throw new IllegalArgumentException("Points cannot be null!");
        }

        int minX = Math.min(p1.getX(), p2.getX());
        int maxX = Math.max(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int maxY = Math.max(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxZ = Math.max(p1.getZ(), p2.getZ());

        setFacePoint(FaceType.X_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.X_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Y_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Y_MAX, new BlockPos(maxX, maxY, maxZ));
        setFacePoint(FaceType.Z_MIN, new BlockPos(minX, minY, minZ));
        setFacePoint(FaceType.Z_MAX, new BlockPos(maxX, maxY, maxZ));
    }

    /**
     * Set selection from 6 points.
     * Exactly 6 points are required.
     * 
     * @param points Array of 6 points
     */
    public void setFromSixPoints(BlockPos... points) {
        if (points == null || points.length != 6) {
            throw new IllegalArgumentException("Exactly 6 points are required!");
        }
        setFromPoints(points);
    }

    /**
     * Set selection from arbitrary points in space with world specification.
     * 
     * @param world  world in which the selection is located
     * @param points array of points
     */
    public void setFromPoints(Level world, BlockPos... points) {
        this.world = world;
        setFromPoints(points);
    }
}
