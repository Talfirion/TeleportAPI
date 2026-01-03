package com.teleportapi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformationTest {

    @Test
    void testPosTransformationRotation90() {
        BlockPos pos = new BlockPos(1, 0, 0);
        Vec3i size = new Vec3i(1, 0, 1); // 2x1x2 area (0 to 1)

        BlockPos result = StructureTeleporter.transformPos(pos, Rotation.CLOCKWISE_90, Mirror.NONE, size);
        // (1,0,0) -> size.z - 0 = 1, y=0, x=1 -> (1,0,1)
        assertEquals(new BlockPos(1, 0, 1), result);
    }

    @Test
    void testPosTransformationRotation180() {
        BlockPos pos = new BlockPos(1, 0, 0);
        Vec3i size = new Vec3i(1, 0, 1);

        BlockPos result = StructureTeleporter.transformPos(pos, Rotation.CLOCKWISE_180, Mirror.NONE, size);
        // (1,0,0) -> size.x - 1 = 0, y=0, size.z - 0 = 1 -> (0,0,1)
        assertEquals(new BlockPos(0, 0, 1), result);
    }

    @Test
    void testPosTransformationMirror() {
        BlockPos pos = new BlockPos(1, 0, 0);
        Vec3i size = new Vec3i(1, 0, 1);

        BlockPos result = StructureTeleporter.transformPos(pos, Rotation.NONE, Mirror.FRONT_BACK, size);
        // (1,0,0) -> size.x - 1 = 0, y=0, z=0 -> (0,0,0)
        assertEquals(new BlockPos(0, 0, 0), result);
    }

    @Test
    void testEntityRotationMirror() {
        float yRot = 90.0f; // East
        float result = StructureTeleporter.mirrorRotation(Mirror.LEFT_RIGHT, yRot);
        // LEFT_RIGHT: (180 - yRot) % 360 -> (180 - 90) = 90
        assertEquals(90.0f, result);

        result = StructureTeleporter.mirrorRotation(Mirror.FRONT_BACK, yRot);
        // FRONT_BACK: -yRot % 360 -> -90
        assertEquals(-90.0f, result);
    }
}
