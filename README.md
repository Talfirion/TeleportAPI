# TeleportAPI

TeleportAPI is a library for Minecraft Forge 1.20.1 that provides functionality for selecting and teleporting structures.

## Selection

The `Selection` class defines a cuboid region in the world.

```java
Selection selection = new Selection();
selection.setWorld(level);

// Way 1: From two diagonal corners (most common)
selection.setFromCorners(new BlockPos(0, 64, 0), new BlockPos(10, 74, 10));

// Way 2: From arbitrary points (automatically finds min/max bounding box)
selection.setFromPoints(pos1, pos2, pos3 ...);

// Way 3: Manual face control (advanced)
selection.setFacePoint(FaceType.X_MIN, pos); 
// Note: manual setting requires ensuring min <= max for all axes

// Utility methods
if (selection.isComplete()) {
    BlockPos min = selection.getMin();
    BlockPos max = selection.getMax();
    int volume = selection.getVolume();
}
```

## StructureTeleporter

Handles copying, pasting, and teleportation logic.

### Teleport Structure (Move)

```java
// Teleport a structure to a destination and return result
TeleportResult result = StructureTeleporter.teleportStructure(
    selection, 
    targetPos, 
    StructureTeleporter.getDefaultExcludedBlocks(), // Blocks to skip (bedrock, etc.)
    true // Perform teleport (false for scan only)
);

if (result.isSuccess()) {
    System.out.println("Teleported " + result.getTeleportedBlocks() + " blocks");
}

// Way 2: Teleport to another dimension using ID
StructureTeleporter.teleportStructure(
    selection,
    new ResourceLocation("minecraft:the_nether"), // Destination dimension ID
    targetPos,
    null,
    true
);
```

### Copy & Paste Separately

```java
// Copy to memory relative to selection.getMin()
List<BlockData> copied = StructureTeleporter.copyStructure(selection);

// Paste at target location in FORCE_REPLACE mode (default)
StructureTeleporter.pasteStructure(copied, targetPos, level);

// Paste with specific mode
StructureTeleporter.pasteStructure(
    copied, 
    targetPos, 
    level, 
    PasteMode.PRESERVE_EXISTING, // Options: FORCE_REPLACE, PRESERVE_LIST, PRESERVE_EXISTING
    null // Custom preserved blocks list
);
```

## TeleportResult

Provides detailed metrics after a teleport operation.

| Method | Description |
|--------|-------------|
| `isSuccess()` | True if selection was complete and operation finished |
| `getTeleportedBlocks()` | Count of non-air, non-excluded blocks moved |
| `getReplacedBlockCount()` | Blocks at destination that were overwritten |
| `getExcludedBlocks()` | Count of blocks skipped based on exclusion list |
| `getTeleportedEntitiesCount()` | Players and entities moved within selection |

## Forge Events

TeleportAPI fires Forge events that allow other mods to intercept and react to teleportation. This is useful for protection mods (claims) or logging.

### StructureTeleportEvent

- **`Pre`**: Fired before teleportation begins. It is `@Cancelable`. If canceled, the operation is aborted.
- **`Post`**: Fired after successful teleportation. Contains the `TeleportResult`.

```java
@SubscribeEvent
public void onTeleportPre(StructureTeleportEvent.Pre event) {
    if (isAreaProtected(event.getTargetLevel(), event.getTargetPos())) {
        event.setCanceled(true); // Prevent teleportation
    }
}

@SubscribeEvent
public void onTeleportPost(StructureTeleportEvent.Post event) {
    TeleportResult result = event.getTeleportResult();
    LOGGER.info("Teleportation finished with " + result.getTeleportedBlocks() + " blocks.");
}
```

## Common Configuration

- **Excluded Blocks**: Bedrock and portal frames are excluded by default to maintain world integrity.
- **NBT Data**: The API automatically preserves TileEntity data (inventories, sign text, etc.).
- **Physics**: Blocks like torches or redstone are sanitized and re-checked for "survival" at destination to prevent floating items.
- **Dimensions**: Full support for modded dimensions. Use `DimensionHelper` to discover available dimensions or `StructureTeleporter` with `ResourceLocation` IDs for high-level teleportation.
- **Compatibility**: Events allow integration with protection mods like FTB Chunks, MineColonies, etc.
