package com.teleportapi;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for FaceType enum.
 * Tests all enum values, helper methods, and axis calculations.
 */
class FaceTypeTest {

    @Test
    void testAllFaceTypesExist() {
        // Verify all 6 face types exist
        FaceType[] faceTypes = FaceType.values();
        assertEquals(6, faceTypes.length, "Should have exactly 6 face types");

        // Verify each specific type exists
        assertNotNull(FaceType.X_MIN);
        assertNotNull(FaceType.X_MAX);
        assertNotNull(FaceType.Y_MIN);
        assertNotNull(FaceType.Y_MAX);
        assertNotNull(FaceType.Z_MIN);
        assertNotNull(FaceType.Z_MAX);
    }

    @Test
    void testIsMinFace() {
        // Test MIN faces return true
        assertTrue(FaceType.X_MIN.isMinFace());
        assertTrue(FaceType.Y_MIN.isMinFace());
        assertTrue(FaceType.Z_MIN.isMinFace());

        // Test MAX faces return false
        assertFalse(FaceType.X_MAX.isMinFace());
        assertFalse(FaceType.Y_MAX.isMinFace());
        assertFalse(FaceType.Z_MAX.isMinFace());
    }

    @Test
    void testIsMaxFace() {
        // Test MAX faces return true
        assertTrue(FaceType.X_MAX.isMaxFace());
        assertTrue(FaceType.Y_MAX.isMaxFace());
        assertTrue(FaceType.Z_MAX.isMaxFace());

        // Test MIN faces return false
        assertFalse(FaceType.X_MIN.isMaxFace());
        assertFalse(FaceType.Y_MIN.isMaxFace());
        assertFalse(FaceType.Z_MIN.isMaxFace());
    }

    @Test
    void testGetAxis() {
        // Test X axis faces
        assertEquals(FaceType.Axis.X, FaceType.X_MIN.getAxis());
        assertEquals(FaceType.Axis.X, FaceType.X_MAX.getAxis());

        // Test Y axis faces
        assertEquals(FaceType.Axis.Y, FaceType.Y_MIN.getAxis());
        assertEquals(FaceType.Axis.Y, FaceType.Y_MAX.getAxis());

        // Test Z axis faces
        assertEquals(FaceType.Axis.Z, FaceType.Z_MIN.getAxis());
        assertEquals(FaceType.Axis.Z, FaceType.Z_MAX.getAxis());
    }

    @Test
    void testGetDescription() {
        // Verify all face types have non-null, non-empty descriptions
        for (FaceType faceType : FaceType.values()) {
            String description = faceType.getDescription();
            assertNotNull(description, faceType + " should have a description");
            assertFalse(description.isEmpty(), faceType + " description should not be empty");
        }

        // Verify specific descriptions
        assertTrue(FaceType.X_MIN.getDescription().contains("min X"));
        assertTrue(FaceType.X_MAX.getDescription().contains("max X"));
        assertTrue(FaceType.Y_MIN.getDescription().contains("min Y"));
        assertTrue(FaceType.Y_MAX.getDescription().contains("max Y"));
        assertTrue(FaceType.Z_MIN.getDescription().contains("min Z"));
        assertTrue(FaceType.Z_MAX.getDescription().contains("max Z"));
    }

    @Test
    void testAxisEnum() {
        // Verify Axis enum has all three axes
        FaceType.Axis[] axes = FaceType.Axis.values();
        assertEquals(3, axes.length, "Should have exactly 3 axes");

        assertNotNull(FaceType.Axis.X);
        assertNotNull(FaceType.Axis.Y);
        assertNotNull(FaceType.Axis.Z);
    }
}
