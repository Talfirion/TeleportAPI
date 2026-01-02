# TeleportAPI

TeleportAPI is a powerful library for Minecraft Forge 1.20.1 that provides a structured and efficient way to select, copy, and teleport structures across the world and dimensions.

## 🚀 Basic Usage (Simple Introduction)

For most use cases, you can perform a teleportation in just a few lines of code.

### 1. Defining a Selection
The `Selection` class defines a cuboid region. The most common way to create one is from two diagonal corners.

```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromCorners(new BlockPos(0, 64, 0), new BlockPos(10, 74, 10));
```

### 2. Simple Teleportation
Use the high-level `teleport` methods for quick operations.

```java
// Teleport by corners (shorthand)
StructureTeleporter.teleport(level, pos1, pos2, targetPos);

// Teleport an existing selection
StructureTeleporter.teleport(selection, targetPos);

// Teleport a specific collection of arbitrary block positions
Collection<BlockPos> positions = ...;
StructureTeleporter.teleport(level, positions, targetPos);
```

### 3. Handling Results
Every teleport operation returns a `TeleportResult`.

```java
TeleportResult result = StructureTeleporter.teleport(selection, targetPos);

if (result.isSuccess()) {
    System.out.println("Moved " + result.getTeleportedBlocks() + " blocks!");
} else {
    System.err.println("Teleport failed: " + result.getMessage());
}
```

---

## 🛠️ Advanced Teleportation (Custom Settings)

For complex scenarios requiring fine-grained control, use the **Teleport Request API**.

### The TeleportRequest Builder
The `TeleportRequest` object encapsulates all possible parameters, including dimension travel, entity teleportation, and custom paste modes.

```java
TeleportRequest request = new TeleportRequest.Builder(selection, targetPos)
    .targetLevel(netherLevel)     // Teleport to another dimension
    .includeAir(false)            // Skip air blocks during move
    .teleportEntities(true)       // Move entities along with blocks
    .teleportPlayers(true)        // Move players if they are in the area
    .pasteMode(PasteMode.PRESERVE_EXISTING) // Don't overwrite destination blocks
    .checkExclusions(true)        // Skip bedrock/portals
    .player(triggeringPlayer)     // Associate a player for permission checks
    .build();

TeleportResult result = StructureTeleporter.teleport(request);
```

### Custom Paste Modes
| Mode | Description |
|------|-------------|
| `FORCE_REPLACE` | Standard behavior: overwrites anything at the destination. |
| `PRESERVE_EXISTING` | Only pastes blocks into air/water; won't overwrite solid blocks. |
| `PRESERVE_LIST` | Won't overwrite blocks specified in the `preservedBlocks` list. |

### Copy & Paste Separately
If you need to store a structure in memory before pasting it later:

```java
// Copy structure to memory
List<BlockData> data = StructureTeleporter.copyStructure(selection);

// ... or copy arbitrary positions
List<BlockData> data = StructureTeleporter.copyStructure(level, positions);

// Paste later
StructureTeleporter.pasteStructure(data, targetPos, level);
```

---

## 📊 Detailed Metrics (`TeleportResult`)
The `TeleportResult` provides granular feedback about the operation.

| Method | Description |
|--------|-------------|
| `getTeleportedBlocks()` | Count of solid blocks moved. |
| `getReplacedBlockCount()` | Blocks deleted at destination. |
| `getExcludedBlocks()` | Blocks skipped due to exclusion lists (e.g. Bedrock). |
| `getSourceBlockCounts()` | Map of all `BlockState` types found in source. |
| `getDestinationSolidBlocksLost()`| Solid blocks destroyed at destination. |

---

## 🛡️ Permissions and Safety
The API includes built-in protection logic:
- **Active Checks**: Verifies `BreakEvent` and `PlaceEvent` using `PermissionHelper`.
- **Integrity**: Excludes unbreakable blocks (Bedrock, Command Blocks) by default.
- **Rollback**: In case of a critical failure during the move, the API attempts to restore the original structure to prevent data loss.

### Subscribing to Events
```java
@SubscribeEvent
public void onTeleport(StructureTeleportEvent.Pre event) {
    // Cancel based on custom logic
    if (areaIsRestricted(event)) event.setCanceled(true);
}
```

## 📦 Dimension Support
Use `DimensionHelper` for easy cross-dimension operations:
```java
ServerLevel target = DimensionHelper.getServerLevel(server, "minecraft:the_nether");
```
