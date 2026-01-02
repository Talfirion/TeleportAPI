package com.teleportapi;

/**
 * Mode for pasting structures at the destination.
 */
public enum PasteMode {
    /** Replace all blocks at destination (default). */
    FORCE_REPLACE,
    /** Replace all except blocks in the preserved list. */
    PRESERVE_LIST,
    /** Do not replace any existing non-air blocks. */
    PRESERVE_EXISTING
}
