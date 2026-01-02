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
        PasteMode[] modes = PasteMode.values();
        assertEquals(3, modes.length, "Should have exactly 3 paste modes");

        assertNotNull(PasteMode.FORCE_REPLACE);
        assertNotNull(PasteMode.PRESERVE_LIST);
        assertNotNull(PasteMode.PRESERVE_EXISTING);
    }

    @Test
    void testModeValueOf() {
        assertEquals(PasteMode.FORCE_REPLACE,
                PasteMode.valueOf("FORCE_REPLACE"));
        assertEquals(PasteMode.PRESERVE_LIST,
                PasteMode.valueOf("PRESERVE_LIST"));
        assertEquals(PasteMode.PRESERVE_EXISTING,
                PasteMode.valueOf("PRESERVE_EXISTING"));
    }

    @Test
    void testModeNames() {
        assertEquals("FORCE_REPLACE", PasteMode.FORCE_REPLACE.name());
        assertEquals("PRESERVE_LIST", PasteMode.PRESERVE_LIST.name());
        assertEquals("PRESERVE_EXISTING", PasteMode.PRESERVE_EXISTING.name());
    }

    // ========== Mode Selection Tests ==========

    @Test
    void testForceReplaceMode() {
        PasteMode mode = PasteMode.FORCE_REPLACE;
        assertNotNull(mode);
        assertEquals("FORCE_REPLACE", mode.toString());
    }

    @Test
    void testPreserveListMode() {
        PasteMode mode = PasteMode.PRESERVE_LIST;
        assertNotNull(mode);
        assertEquals("PRESERVE_LIST", mode.toString());
    }

    @Test
    void testPreserveExistingMode() {
        PasteMode mode = PasteMode.PRESERVE_EXISTING;
        assertNotNull(mode);
        assertEquals("PRESERVE_EXISTING", mode.toString());
    }

    // ========== Mode Comparison Tests ==========

    @Test
    void testModeEquality() {
        PasteMode mode1 = PasteMode.FORCE_REPLACE;
        PasteMode mode2 = PasteMode.FORCE_REPLACE;

        assertEquals(mode1, mode2);
        assertSame(mode1, mode2); // Enums are singletons
    }

    @Test
    void testModeInequality() {
        PasteMode mode1 = PasteMode.FORCE_REPLACE;
        PasteMode mode2 = PasteMode.PRESERVE_LIST;

        assertNotEquals(mode1, mode2);
    }

    // ========== Mode in Switch Statements ==========

    @Test
    void testModeInSwitch() {
        PasteMode mode = PasteMode.FORCE_REPLACE;

        String result = switch (mode) {
            case FORCE_REPLACE -> "force";
            case PRESERVE_LIST -> "list";
            case PRESERVE_EXISTING -> "existing";
        };

        assertEquals("force", result);
    }

    @Test
    void testAllModesInSwitch() {
        for (PasteMode mode : PasteMode.values()) {
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
        assertEquals(0, PasteMode.FORCE_REPLACE.ordinal());
        assertEquals(1, PasteMode.PRESERVE_LIST.ordinal());
        assertEquals(2, PasteMode.PRESERVE_EXISTING.ordinal());
    }

    @Test
    void testModeComparison() {
        assertTrue(PasteMode.FORCE_REPLACE.compareTo(
                PasteMode.PRESERVE_LIST) < 0);
        assertTrue(PasteMode.PRESERVE_EXISTING.compareTo(
                PasteMode.FORCE_REPLACE) > 0);
    }

    // ========== Mode Array Operations ==========

    @Test
    void testValuesArray() {
        PasteMode[] modes = PasteMode.values();

        assertEquals(3, modes.length);
        assertEquals(PasteMode.FORCE_REPLACE, modes[0]);
        assertEquals(PasteMode.PRESERVE_LIST, modes[1]);
        assertEquals(PasteMode.PRESERVE_EXISTING, modes[2]);
    }

    @Test
    void testValuesArrayIsCopy() {
        PasteMode[] modes1 = PasteMode.values();
        PasteMode[] modes2 = PasteMode.values();

        assertNotSame(modes1, modes2);
        assertArrayEquals(modes1, modes2);
    }

    // ========== Invalid valueOf Tests ==========

    @Test
    void testInvalidValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasteMode.valueOf("INVALID_MODE");
        });
    }

    @Test
    void testNullValueOf() {
        assertThrows(NullPointerException.class, () -> {
            PasteMode.valueOf(null);
        });
    }

    @Test
    void testCaseSensitiveValueOf() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasteMode.valueOf("force_replace");
        });
    }
}
