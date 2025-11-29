# TeleportAPI - Developer Guide

## Overview

TeleportAPI is a library for Minecraft Forge 1.20.1 that provides functionality for selecting, copying, and teleporting structures in the world.

## Installation

Add to your `build.gradle`:

```gradle
repositories {
    flatDir {
        dirs 'libs'
    }
}

dependencies {
    implementation files('libs/TeleportAPI-1.0.0.jar')
}
```

## Core Classes

### Selection

Represents a selected region in the world. Supports multiple definition methods:

#### Method 1: From Two Corners
```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromCorners(
    new BlockPos(0, 64, 0),
    new BlockPos(10, 74, 10)
);
```

#### Method 2: From Any Number of Points
```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromPoints(
    new BlockPos(5, 70, 5),
    new BlockPos(2, 68, 3),
    new BlockPos(8, 72, 9)
);
```

#### Method 3: From Exactly Six Points
```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromSixPoints(
    new BlockPos(10, 65, 35),
    new BlockPos(20, 70, 40),
    new BlockPos(15, 64, 38),
    new BlockPos(12, 80, 42),
    new BlockPos(18, 72, 30),
    new BlockPos(14, 68, 50)
);
```

#### Method 4: Set Individual Face Points
```java
Selection selection = new Selection();
selection.setWorld(level);

selection.setFacePoint(FaceType.X_MIN, new BlockPos(10, 65, 35));
selection.setFacePoint(FaceType.X_MAX, new BlockPos(20, 70, 40));
selection.setFacePoint(FaceType.Y_MIN, new BlockPos(15, 64, 38));
selection.setFacePoint(FaceType.Y_MAX, new BlockPos(12, 80, 42));
selection.setFacePoint(FaceType.Z_MIN, new BlockPos(18, 72, 30));
selection.setFacePoint(FaceType.Z_MAX, new BlockPos(14, 68, 50));
```

#### Working with Selections
```java
// Check if complete
if (selection.isComplete()) {
    BlockPos min = selection.getMin();
    BlockPos max = selection.getMax();
    int volume = selection.getVolume();
    String info = selection.getInfo();
}

// Get count of defined faces
int facesSet = selection.getSetFacesCount();

// Reset
selection.reset();
```

### SelectionManager

Singleton that maintains selections for each player:

```java
SelectionManager manager = SelectionManager.getInstance();

// Get player's selection
Selection playerSelection = manager.getSelection(player);

// Modify selection
playerSelection.setFromCorners(pos1, pos2);

// Clear selection
manager.clearSelection(player);
```

### StructureTeleporter

Handles structure copying, pasting, and teleportation.

#### Copy Structure
```java
Selection selection = /* ... */;
List<StructureTeleporter.BlockData> copiedStructure = 
    StructureTeleporter.copyStructure(selection);
```

#### Paste Structure
```java
BlockPos targetPos = new BlockPos(100, 64, 100);
StructureTeleporter.pasteStructure(
    copiedStructure,
    targetPos,
    level
);
```

#### Teleport Structure (Move + Paste)
```java
Selection selection = /* ... */;
BlockPos targetPos = new BlockPos(100, 64, 100);
StructureTeleporter.teleportStructure(selection, targetPos);
```

### FaceType

Enum for identifying cuboid faces:

```java
public enum FaceType {
    X_MIN, X_MAX,
    Y_MIN, Y_MAX,
    Z_MIN, Z_MAX
}
```

Usage:
```java
FaceType.X_MIN.isMinFace();      // true
FaceType.X_MIN.getAxis();        // Axis.X
```

## How to Teleport Structures

### Way 1: Using Two Corners (Simplest)

Get two opposite corners of your structure and call:

```java
Level world = /* your world */;
BlockPos corner1 = new BlockPos(0, 64, 0);
BlockPos corner2 = new BlockPos(10, 74, 10);
BlockPos targetPos = new BlockPos(100, 64, 100);

StructureTeleporter.teleportByCorners(world, corner1, corner2, targetPos);
```

**What happens:**
1. Creates bounding box from both corners
2. Copies all blocks from that region to memory (including NBT data)
3. Removes blocks from original location
4. Pastes blocks at target position

---

### Way 2: Using Six Points

Get six points that define each face and call:

```java
Level world = /* your world */;
BlockPos[] points = new BlockPos[6];
points[0] = new BlockPos(10, 65, 35);   // Point 1
points[1] = new BlockPos(20, 70, 40);   // Point 2
points[2] = new BlockPos(15, 64, 38);   // Point 3
points[3] = new BlockPos(12, 80, 42);   // Point 4
points[4] = new BlockPos(18, 72, 30);   // Point 5
points[5] = new BlockPos(14, 68, 50);   // Point 6

BlockPos targetPos = new BlockPos(100, 64, 100);

StructureTeleporter.teleportBySixPoints(world, points, targetPos);
```

**What happens:**
1. Calculates min/max coordinates from all points
2. Creates bounding box
3. Copies, removes, and pastes (same as Way 1)

---

### Way 3: Manual Selection Object

For more control, use Selection directly:

```java
Level world = /* your world */;

// Create selection
Selection selection = new Selection();
selection.setWorld(world);
selection.setFromCorners(
    new BlockPos(0, 64, 0),
    new BlockPos(10, 74, 10)
);

// Teleport
BlockPos targetPos = new BlockPos(100, 64, 100);
StructureTeleporter.teleportStructure(selection, targetPos);
```

---

### Way 4: Copy and Paste Separately

If you need to paste multiple times without removing original:

```java
Level world = /* your world */;

// Create selection
Selection selection = new Selection();
selection.setWorld(world);
selection.setFromCorners(pos1, pos2);

// Copy to memory (doesn't remove blocks)
List<StructureTeleporter.BlockData> copied = 
    StructureTeleporter.copyStructure(selection);

// Paste at first location
StructureTeleporter.pasteStructure(copied, new BlockPos(100, 64, 100), world);

// Paste at second location (same blocks)
StructureTeleporter.pasteStructure(copied, new BlockPos(200, 64, 200), world);
```

---

## Important Notes

### Always Check Selection Completion
```java
if (!selection.isComplete()) {
    return;
}
```

### Set World Before Operations
```java
selection.setWorld(level);
```

### Data Preservation

The API automatically preserves:
- Block states
- Block entity data (NBT)
- Tile entity properties

### What Gets Copied

When teleporting a structure, the API copies:
- âœ“ All block types and their states
- âœ“ Chest contents
- âœ“ Sign text
- âœ“ All tile entity NBT data
- âœ“ Custom block properties

## API Reference

| Class | Purpose |
|-------|---------|
| `Selection` | Define region selections |
| `SelectionManager` | Track per-player selections |
| `StructureTeleporter` | Copy, paste, teleport structures |
| `FaceType` | Identify cuboid faces |
| `BlockData` | Internal data storage |