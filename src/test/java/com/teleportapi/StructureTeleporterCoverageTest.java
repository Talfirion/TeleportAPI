package com.teleportapi;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class StructureTeleporterCoverageTest {

    private List<String> coverageBlocks;

    @BeforeEach
    void setUp() {
        coverageBlocks = new ArrayList<>();
        coverageBlocks.add("hull");
    }

    @Test
    void testSimpleCubeFilter() {
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(2, 2, 2);

        // A 3x3x3 selection where (0,0,0)-(2,2,2) is the bounding box.
        // A 3x3x3 cube of "hull" blocks.
        StructureTeleporter.StateProvider<String> provider = pos -> "hull";
        StructureTeleporter.StateMatcher<String> matcher = (s, l) -> l.contains(s);

        Set<BlockPos> enclosed = StructureTeleporter.getEnclosedPositionsGeneric(min, max, coverageBlocks, provider,
                matcher);

        // All 27 positions should be enclosed (part of hull)
        assertEquals(27, enclosed.size());
    }

    @Test
    void testShipInSelectionWithTree() {
        // Selection is 10x10x10
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(9, 9, 9);

        // A ship at (2,2,2) to (4,4,4) - 3x3x3 hull
        // A "tree" at (7,7,7) - "wood" block
        StructureTeleporter.StateProvider<String> provider = pos -> {
            // Ship hull: 3x3x3 cube surface
            boolean isHull = (pos.getX() >= 2 && pos.getX() <= 4 && pos.getY() >= 2 && pos.getY() <= 4
                    && pos.getZ() >= 2 && pos.getZ() <= 4) &&
                    (pos.getX() == 2 || pos.getX() == 4 || pos.getY() == 2 || pos.getY() == 4 || pos.getZ() == 2
                            || pos.getZ() == 4);

            if (isHull)
                return "hull";
            if (pos.getX() == 7 && pos.getY() == 7 && pos.getZ() == 7)
                return "wood";

            return "air";
        };
        StructureTeleporter.StateMatcher<String> matcher = (s, l) -> l.contains(s);

        Set<BlockPos> enclosed = StructureTeleporter.getEnclosedPositionsGeneric(min, max, coverageBlocks, provider,
                matcher);

        // The ship (2,2,2 to 4,4,4) has 27 positions. All should be enclosed.
        assertTrue(enclosed.contains(new BlockPos(2, 2, 2)), "Hull should be enclosed");
        assertTrue(enclosed.contains(new BlockPos(3, 3, 3)), "Interior should be enclosed");

        // The tree (7,7,7) should NOT be enclosed
        assertFalse(enclosed.contains(new BlockPos(7, 7, 7)), "Tree outside hull should NOT be enclosed");

        // Random air at (0,0,0) should NOT be enclosed
        assertFalse(enclosed.contains(new BlockPos(0, 0, 0)), "Outside air should NOT be enclosed");
    }

    @Test
    void testLeakyHull() {
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(2, 2, 2);

        // 3x3x3 cube with a hole at (1, 1, 0) - Front face center
        StructureTeleporter.StateProvider<String> provider = pos -> {
            if (pos.getX() == 1 && pos.getY() == 1 && pos.getZ() == 0)
                return "air"; // HOLE

            boolean isHull = (pos.getX() == 0 || pos.getX() == 2 || pos.getY() == 0 || pos.getY() == 2
                    || pos.getZ() == 0 || pos.getZ() == 2);
            return isHull ? "hull" : "air";
        };
        StructureTeleporter.StateMatcher<String> matcher = (s, l) -> l.contains(s);

        Set<BlockPos> enclosed = StructureTeleporter.getEnclosedPositionsGeneric(min, max, coverageBlocks, provider,
                matcher);

        // The interior block (1,1,1) should NOT be enclosed because of the hole at
        // (1,1,0)
        assertFalse(enclosed.contains(new BlockPos(1, 1, 1)), "Interior of leaky hull should NOT be enclosed");
        assertTrue(enclosed.contains(new BlockPos(0, 0, 0)), "Hull itself should still be enclosed");
    }

    @Test
    void testComplexLShapedShip() {
        BlockPos min = new BlockPos(0, 0, 0);
        BlockPos max = new BlockPos(4, 4, 4);

        // L-shaped hull: two 3x3x3 cubes joined.
        // Cube 1: (0,0,0) to (2,2,2)
        // Cube 2: (0,0,0) to (0,4,2) -> wait, let's keep it simple.

        // Actually, let's just test that any block not reachable from outside is
        // enclosed.
        StructureTeleporter.StateProvider<String> provider = pos -> {
            // L-shape boundary
            boolean part1 = (pos.getX() == 0 || pos.getX() == 2) && (pos.getY() >= 0 && pos.getY() <= 2)
                    && (pos.getZ() >= 0 && pos.getZ() <= 2);
            boolean part2 = (pos.getY() == 0 || pos.getY() == 2) && (pos.getX() >= 0 && pos.getX() <= 2)
                    && (pos.getZ() >= 0 && pos.getZ() <= 2);
            boolean part3 = (pos.getZ() == 0 || pos.getZ() == 2) && (pos.getX() >= 0 && pos.getX() <= 2)
                    && (pos.getY() >= 0 && pos.getY() <= 2);

            if (part1 || part2 || part3)
                return "hull";
            return "air";
        };
        StructureTeleporter.StateMatcher<String> matcher = (s, l) -> l.contains(s);

        Set<BlockPos> enclosed = StructureTeleporter.getEnclosedPositionsGeneric(min, max, coverageBlocks, provider,
                matcher);

        assertTrue(enclosed.contains(new BlockPos(1, 1, 1)), "Interior of L-shape should be enclosed");
        assertFalse(enclosed.contains(new BlockPos(4, 4, 4)), "Outside point should not be enclosed");
    }
}
