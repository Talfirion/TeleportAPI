package com.teleportapi;

/**
 * Types of visualization for asynchronous teleportation.
 */
public enum VisualizationType {
    /** No special effects, just blocks appearing batch by batch. */
    NONE,
    /** Blocks appear gradually from bottom to top. (WIP: Not implemented) */
    ASSEMBLY,
    /**
     * A "warp" effect with noisy sorting, particles, and sounds. (WIP: Not
     * implemented)
     */
    WARP,
    /**
     * StarCraft II style warp: projection phase followed by hull-first
     * materialization. (WIP: Not implemented)
     */
    WARP_SC2
}
