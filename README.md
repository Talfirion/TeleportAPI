# TeleportAPI - Developer Guide

## Overview

TeleportAPI is a library for Minecraft Forge 1.20.1 that provides functionality for selecting, copying, and teleporting structures in the world.

## Development Setup

### Prerequisites

This project requires **Java 17** to build. The project is configured to use Java 17 as specified in `build.gradle`.

### Windows Setup

1. **Install Java 17**
   
   Download and install OpenJDK 17 from one of these sources:
   - [Microsoft Build of OpenJDK 17](https://learn.microsoft.com/en-us/java/openjdk/download#openjdk-17)
   - [Adoptium Eclipse Temurin 17](https://adoptium.net/temurin/releases/?version=17)
   - [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)

2. **Set JAVA_HOME Environment Variable**
   
   After installation, set the `JAVA_HOME` environment variable:
   
   - Open **System Properties** → **Environment Variables**
   - Under **System variables**, click **New**
   - Variable name: `JAVA_HOME`
   - Variable value: Path to your JDK 17 installation (e.g., `C:\Program Files\Microsoft\jdk-17.0.x`)
   - Click **OK** to save
   
   Alternatively, using PowerShell (as Administrator):
   ```powershell
   [System.Environment]::SetEnvironmentVariable('JAVA_HOME', 'C:\Program Files\Microsoft\jdk-17.0.x', [System.EnvironmentVariableTarget]::Machine)
   ```

3. **Add Java to PATH**
   
   Add `%JAVA_HOME%\bin` to your PATH environment variable:
   
   - In **Environment Variables**, find **Path** under **System variables**
   - Click **Edit** → **New**
   - Add: `%JAVA_HOME%\bin`
   - Click **OK** to save all dialogs

4. **Verify Installation**
   
   Open a new Command Prompt or PowerShell window and verify:
   ```cmd
   java -version
   ```
   
   You should see output indicating Java 17 (e.g., `openjdk version "17.0.x"`)

5. **Configure Gradle (Optional)**
   
   If Gradle doesn't detect Java 17 automatically, you can specify it in `gradle-local.properties`:
   
   Create `gradle-local.properties` in the project root (this file is gitignored):
   ```properties
   org.gradle.java.home=C:\\Program Files\\Microsoft\\jdk-17.0.x
   ```
   
   Replace the path with your actual JDK 17 installation path.

6. **Build the Project**
   
   Run the Gradle wrapper:
   ```cmd
   gradlew.bat build
   ```

### macOS/Linux Setup

1. **Install Java 17**
   
   Using Homebrew (macOS):
   ```bash
   brew install openjdk@17
   ```
   
   Using apt (Ubuntu/Debian):
   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk
   ```

2. **Set JAVA_HOME**
   
   macOS:
   ```bash
   export JAVA_HOME=$(/usr/libexec/java_home -v 17)
   ```
   
   Linux:
   ```bash
   export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
   ```
   
   Add to your `~/.zshrc` or `~/.bashrc` to make it permanent.

3. **Verify Installation**
   ```bash
   java -version
   ```

4. **Build the Project**
   ```bash
   ./gradlew build
   ```

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

Represents a selected region in the world. This class stores the boundaries of a cuboid 
(3D box) by tracking the minimum and maximum coordinates on each axis (X, Y, Z).

**How it works internally:**
- Selection stores 6 face points (one for each side of a cube)
- Each face is identified by FaceType enum (X_MIN, X_MAX, Y_MIN, Y_MAX, Z_MIN, Z_MAX)
- When you provide points, Selection automatically finds the min/max coordinates
- It validates that min ≤ max for each axis (warns if you set them incorrectly)

---

## Method 1: From Two Corners (Most Common - Like WorldEdit)

```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromCorners(
    new BlockPos(0, 64, 0),      // First corner (any diagonal point)
    new BlockPos(10, 74, 10)     // Second corner (opposite diagonal point)
);
```

**What happens:**
1. You provide two diagonal corners of the cuboid (doesn't matter which is bigger)
2. Method compares all coordinates:
   - `Math.min(0, 10)` = 0 → stored as X_MIN
   - `Math.max(0, 10)` = 10 → stored as X_MAX
   - Same for Y and Z axes
3. Results:
   - X_MIN = 0, X_MAX = 10
   - Y_MIN = 64, Y_MAX = 74
   - Z_MIN = 0, Z_MAX = 10

**Best for:** Quick region selection with just 2 clicks

---

## Method 2: From Any Number of Points (Flexible)

```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromPoints(
    new BlockPos(5, 70, 5),      // Point 1
    new BlockPos(2, 68, 3),      // Point 2
    new BlockPos(8, 72, 9)       // Point 3 (can be any amount)
);
```

**What happens:**
1. Method iterates through ALL provided points
2. For each coordinate axis, finds minimum and maximum:
   ```
   X values: 5, 2, 8 → min=2, max=8
   Y values: 70, 68, 72 → min=68, max=72
   Z values: 5, 3, 9 → min=3, max=9
   ```
3. Creates bounding box that contains all points
4. Results:
   - X_MIN = 2, X_MAX = 8
   - Y_MIN = 68, Y_MAX = 72
   - Z_MIN = 3, Z_MAX = 9

**Best for:** When you have scattered points and need to find the smallest box containing all of them

---

## Method 3: From Exactly Six Points (Advanced/Flexible Input)

```java
Selection selection = new Selection();
selection.setWorld(level);
selection.setFromSixPoints(
    new BlockPos(10, 65, 35),    // Point 1
    new BlockPos(20, 70, 40),    // Point 2
    new BlockPos(15, 64, 38),    // Point 3
    new BlockPos(12, 80, 42),    // Point 4
    new BlockPos(18, 72, 30),    // Point 5
    new BlockPos(14, 68, 50)     // Point 6
);
```

**What happens:**
1. Requires EXACTLY 6 points (throws exception if not 6)
2. Internally calls `setFromPoints()` - same logic as Method 2
3. Finds min/max for all axes from these 6 points:
   ```
   X values: 10, 20, 15, 12, 18, 14 → min=10, max=20
   Y values: 65, 70, 64, 80, 72, 68 → min=64, max=80
   Z values: 35, 40, 38, 42, 30, 50 → min=30, max=50
   ```

**Best for:** API consistency when you know you have exactly 6 points

---

## Method 4: Set Individual Face Points (Manual/Direct Control)

```java
Selection selection = new Selection();
selection.setWorld(level);

// Manually set each side of the cuboid
selection.setFacePoint(FaceType.X_MIN, new BlockPos(10, 65, 35));  // Left face
selection.setFacePoint(FaceType.X_MAX, new BlockPos(20, 70, 40));  // Right face
selection.setFacePoint(FaceType.Y_MIN, new BlockPos(15, 64, 38));  // Bottom face
selection.setFacePoint(FaceType.Y_MAX, new BlockPos(12, 80, 42));  // Top face
selection.setFacePoint(FaceType.Z_MIN, new BlockPos(18, 72, 30));  // Front face
selection.setFacePoint(FaceType.Z_MAX, new BlockPos(14, 68, 50));  // Back face
```

**What happens:**
1. You directly assign a point to each face type
2. Each call validates coordinates (warns if min > max)
3. No automatic min/max calculation - you control everything
4. **⚠️ WARNING:** You can make invalid selections if you're not careful!
   ```java
   selection.setXMin(20);  // Left face at X=20
   selection.setXMax(10);  // Right face at X=10
   // ERROR! Left is now to the right of right!
   ```

**Best for:** Advanced use cases where you need precise manual control

---

## Working with Selections

```java
// Check if all 6 faces are defined and world is set
if (selection.isComplete()) {
    // Get the minimum corner (lowest X, Y, Z)
    BlockPos min = selection.getMin();  // e.g., BlockPos(10, 64, 30)
    
    // Get the maximum corner (highest X, Y, Z)
    BlockPos max = selection.getMax();  // e.g., BlockPos(20, 80, 50)
    
    // Calculate total volume in blocks
    int volume = selection.getVolume(); 
    // volume = (20-10+1) * (80-64+1) * (50-30+1)
    //        = 11 * 17 * 21 = 3,927 blocks
    
    // Get formatted info string for display
    String info = selection.getInfo();
    // Output example:
    // "Selection: X[10..20] Y[64..80] Z[30..50], Volume: 3927 blocks
    //  Face points:
    //    Left face (min X): 10, 65, 35
    //    Right face (max X): 20, 70, 40
    //    ..."
}

// Get count of how many faces are currently set (0-6)
int facesSet = selection.getSetFacesCount();
if (facesSet < 6) {
    System.out.println("Selection incomplete: " + facesSet + "/6 faces set");
}

// Clear everything and start over
selection.reset();  // Clears all faces and world reference
```

---

## Why These Methods Exist

| Method | Use Case | Input | Auto-calculates min/max? |
|--------|----------|-------|------------------------|
| **From Two Corners** | Quick box selection | 2 diagonal points | ✅ Yes |
| **From Any Points** | Flexible bounding box | 2-N scattered points | ✅ Yes |
| **From Six Points** | Structured input | Exactly 6 points | ✅ Yes |
| **Manual Faces** | Precise control | 6 points with face names | ❌ No (you control it) |

**Recommendation for your users:** Start with **Method 1 (Two Corners)** - it's simplest and most intuitive.

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
- All block types and their states
- Chest contents
- Sign text
- All tile entity NBT data
- Custom block properties
/
## API Reference

| Class | Purpose |
|-------|---------|
| `Selection` | Define region selections |
| `SelectionManager` | Track per-player selections |
| `StructureTeleporter` | Copy, paste, teleport structures |
| `FaceType` | Identify cuboid faces |
| `BlockData` | Internal data storage |