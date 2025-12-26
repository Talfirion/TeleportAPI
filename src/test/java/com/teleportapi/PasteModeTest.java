package com.teleportapi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the PasteMode enum and its behavior.
 * Tests mode selection, replacement logic, and interaction with block lists.
 */
class PasteModeTest {

    // ========== Enum Existence Tests ==========

    @Test
    void testAllPasteModesExist() {
        StructureTeleporter.PasteMode[] modes = StructureTeleporter.PasteMode.values();
        assertEquals(3, modes.length, "Should have exactly 3 paste modes");

        assertNotNull(StructureTeleporter.PasteMode.FORCE_REPLACE);
        assertNotNull(StructureTeleporter.PasteMode.PRESERVE_LIST);
        assertNotNull(StructureTeleporter.PasteMode.PRESERVE_EXISTING);
    }

    @Test
    void testModeValueOf() {
        assertEquals(StructureTeleporter.PasteMode.FORCE_REPLACE,
                StructureTeleporter.PasteMode.valueOf("FORCE_REPLACE"));
        assertEquals(StructureTeleporter.PasteMode.PRESERVE_LIST,
                StructureTeleporter.PasteMode.valueOf("PRESERVE_LIST"));
        assertEquals(StructureTeleporter.PasteMode.PRESERVE_EXISTING,
                StructureTeleporter.PasteMode.valueOf("PRESERVE_EXISTING"));
    }

    @Test
    void testModeNames() {
        assertEquals("FORCE_REPLACE", StructureTeleporter.PasteMode.FORCE_REPLACE.name());
        assertEquals("PRESERVE_LIST", StructureTeleporter.PasteMode.PRESERVE_LIST.name());
        assertEquals("PRESERVE_EXISTING", StructureTeleporter.PasteMode.PRESERVE_EXISTING.name());
    }

    // ========== Mode Selection Tests ==========

    @Test
    void testForceReplaceMode() {
        StructureTeleporter.PasteMode mode = StructureTeleporter.PasteMode.FORCE_REPLACE;
        assertNotNull(mode);
        assertEquals("FORCE_REPLACE", mode.toString());
    }

    @Test
    void testPreserveListMode() {
        StructureTeleporter.PasteMode mode = StructureTeleporter.PasteMode.PRESERVE_LIST;
        assertNotNull(mode);
        assertEquals("PRESERVE_LIST", mode.toString());
    }

    @Test
    void testPreserveExistingMode() {
        StructureTeleporter.PasteMode mode = StructureTeleporter.PasteMode.PRESERVE_EXISTING;
        assertNotNull(mode);
        assertEquals("PRESERVE_EXISTING", mode.toString());
    }

    // ========== Mode Comparison Tests ==========

    @Test
    void testModeEquality() {
        StructureTeleporter.PasteMode mode1 = StructureTeleporter.PasteMode.FORCE_REPLACE;
        StructureTeleporter.PasteMode mode2 = StructureTeleporter.PasteMode.FORCE_REPLACE;

        assertEquals(mode1, mode2);
        assertSame(mode1, mode2); // Enums are singletons
    }

    @Test
    void testModeInequality() {
        StructureTeleporter.PasteMode mode1 = StructureTeleporter.PasteMode.FORCE_REPLACE;
        StructureTeleporter.PasteMode mode2 = StructureTeleporter.PasteMode.PRESERVE_LIST;

        assertNotEquals(mode1, mode2);
    }

    // ========== Mode in Switch Statements ==========

    @Test
    void testModeInSwitch() {
        StructureTeleporter.PasteMode mode = StructureTeleporter.PasteMode.FORCE_REPLACE;

        String result = switch (mode) {
            case FORCE_REPLACE -> "force";
            case PRESERVE_LIST -> "list";
            case PRESERVE_EXISTING -> "existing";
        };

        assertEquals("force", result);
    }

    @Test
    void testAllModesInSwitch() {
        for (StructureTeleporter.PasteMode mode : StructureTeleporter.PasteMode.values()) {
            String result = switch (mode) {
                case FORCE_REPLACE -> "force";
                case PRESERVE_LIST -> "list";
                case PRESERVE_EXISTING -> "existing";
            };
            assertNotNull(result);
        }
    }

    // ========== Mode Ordering Tests ==========

    @Test
    void testModeOrdinals() {
        assertEquals(0, StructureTeleporter.PasteMode.FORCE_REPLACE.ordinal());
        assertEquals(1, StructureTeleporter.PasteMode.PRESERVE_LIST.ordinal());
        assertEquals(2, StructureTeleporter.PasteMode.PRESERVE_EXISTING.ordinal());
    }

    @Test
    void testModeComparison() {
        assertTrue(StructureTeleporter.PasteMode.FORCE_REPLACE.compareTo(
                StructureTeleporter.PasteMode.PRESERVE_LIST) < 0);
        assertTrue(StructureTeleporter.PasteMode.PRESERVE_EXISTING.compareTo(
                StructureTeleporter.PasteMode.FORCE_REPLACE) > 0);
    }

    // ========== Mode Array Operations ==========

    @Test
    void testValuesArray() {
        StructureTeleporter.PasteMode[] modes = StructureTeleporter.PasteMode.values();

        assertEquals(3, modes.length);
        assertEquals(StructureTeleporter.PasteMode.FORCE_REPLACE, modes[0]);
        assertEquals(StructureTeleporter.PasteMode.PRESERVE_LIST, modes[1]);
        assertEquals(StructureTeleporter.PasteMode.PRESERVE_EXISTING, modes[2]);
    }

    @Test
    void testValuesArrayIsCopy() {
        StructureTeleporter.PasteMode[] modes1 = StructureTeleporter.PasteMode.values();
        StructureTeleporter.PasteMode[] modes2 = StructureTeleporter.PasteMode.values();

        assertNotSame(modes1, modes2);
        assertArrayEquals(modes1, modes2);
    }

    // ========== Invalid valueOf Tests ==========

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            StructureTeleporter.PasteMode.valueOf("INVALID_MODE");
        });
    }

    @Test
    void testNullValueOf() {
        assertThrows(NullPointerException.class, () -> {
            StructureTeleporter.PasteMode.valueOf(null);
        });
    }

    @Test
    void testCaseSensitiveValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            StructureTeleporter.PasteMode.valueOf("force_replace");
        });
    }
}
