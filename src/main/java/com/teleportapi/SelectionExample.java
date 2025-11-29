package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Example usage of Selection class showing different methods to define regions.
 * All examples use consistent coordinates that match the README.md
 * documentation.
 */
public class SelectionExample {

    /**
     * Example 1: Using setFromCorners (2 corners)
     * The simplest method - define a region using two opposite corners.
     */
    public static Selection createSelectionFromCorners(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Set selection using two opposite corners
        selection.setFromCorners(
                new BlockPos(0, 64, 0),
                new BlockPos(10, 74, 10));

        return selection;
    }

    /**
     * Example 2: Using setFromPoints (any number of points)
     * Automatically calculates the bounding box from all provided points.
     */
    public static Selection createSelectionFromPoints(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Provide any number of points - the system finds min/max for each axis
        selection.setFromPoints(
                new BlockPos(5, 70, 5),
                new BlockPos(2, 68, 3),
                new BlockPos(8, 72, 9));

        return selection;
    }

    /**
     * Example 3: Using setFromSixPoints (exactly 6 points)
     * Define region using exactly six points.
     */
    public static Selection createSelectionFromSixPoints(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Set selection using exactly 6 points
        selection.setFromSixPoints(
                new BlockPos(10, 65, 35),
                new BlockPos(20, 70, 40),
                new BlockPos(15, 64, 38),
                new BlockPos(12, 80, 42),
                new BlockPos(18, 72, 30),
                new BlockPos(14, 68, 50));

        return selection;
    }

    /**
     * Example 4: Using setFacePoint (individual face points)
     * For axis-aligned box, only the relevant coordinate is used:
     * - For X faces: x-coordinate of the point
     * - For Y faces: y-coordinate of the point
     * - For Z faces: z-coordinate of the point
     */
    public static Selection createSelectionWithFacePoints(Level world) {
        Selection selection = new Selection();
        selection.setWorld(world);

        // Set individual face points
        selection.setFacePoint(FaceType.X_MIN, new BlockPos(10, 65, 35));
        selection.setFacePoint(FaceType.X_MAX, new BlockPos(20, 70, 40));
        selection.setFacePoint(FaceType.Y_MIN, new BlockPos(15, 64, 38));
        selection.setFacePoint(FaceType.Y_MAX, new BlockPos(12, 80, 42));
        selection.setFacePoint(FaceType.Z_MIN, new BlockPos(18, 72, 30));
        selection.setFacePoint(FaceType.Z_MAX, new BlockPos(14, 68, 50));

        // Result: bounding box X[10..20], Y[64..80], Z[30..50]
        return selection;
    }

    /**
     * Example 5: Working with selection information
     * Shows how to check completion and retrieve selection data.
     */
    public static void printSelectionInfo(Selection selection) {
        System.out.println("=== Selection Information ===");

        if (selection.isComplete()) {
            System.out.println(selection.getInfo());
            System.out.println("Min point: " + selection.getMin());
            System.out.println("Max point: " + selection.getMax());
            System.out.println("Volume: " + selection.getVolume() + " blocks");
        } else {
            System.out.println("Selection is incomplete. Faces set: " + selection.getSetFacesCount());
        }
    }
}
