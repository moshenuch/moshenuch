package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Local Nokia home-screen key assignments, independent of Android shortcuts.
 * All values are stable IDs; they are not indices into a mutable menu.
 *
 * The Menu remains reachable after every assignment and every restored
 * preference. The red End key still returns Home from anywhere.
 */
final class NativeHomeShortcuts {
    static final int LSK = 0, RSK = 1, UP = 2, DOWN = 3, LEFT = 4, RIGHT = 5, CENTRE = 6;
    static final int SLOT_COUNT = 7;

    static final int MENU = 0, GO_TO = 1, NAMES = 2, MESSAGING = 3,
            CONTACTS = 4, GALLERY = 5, CAMERA = 6, VIDEO_RECORDER = 7,
            ORGANISER = 8, CALCULATOR = 9, ALARM_CLOCK = 10,
            SETTINGS = 11, PROFILES = 12, NONE = 13;

    static final String[] ACTION_NAMES = {
            "Menu", "Go to", "Names", "Messaging", "Contacts", "Gallery",
            "Camera", "Video recorder", "Organiser", "Calculator",
            "Alarm clock", "Settings", "Profiles", "None"
    };

    private final int[] bindings = new int[SLOT_COUNT];

    NativeHomeShortcuts() {
        restoreDefaults();
    }

    void restoreDefaults() {
        Arrays.fill(bindings, NONE);
        bindings[LSK] = GO_TO;
        bindings[RSK] = NAMES;
        bindings[CENTRE] = MENU;
    }

    int assigned(int slot) {
        return slot >= 0 && slot < SLOT_COUNT ? bindings[slot] : NONE;
    }

    String label(int slot) {
        return ACTION_NAMES[assigned(slot)];
    }

    boolean menuReachable() {
        for (int action : bindings) if (action == MENU) return true;
        return false;
    }

    boolean assign(int slot, int action) {
        if (slot < 0 || slot >= SLOT_COUNT || action < 0 ||
                action >= ACTION_NAMES.length) return false;
        int old = bindings[slot];
        bindings[slot] = action;
        if (!menuReachable()) {
            bindings[slot] = old;
            return false;
        }
        return true;
    }

    String serialize() {
        StringBuilder result = new StringBuilder("[");
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (i > 0) result.append(',');
            result.append(bindings[i]);
        }
        return result.append(']').toString();
    }

    /**
     * Strict JSON-array decoding for SharedPreferences. Incorrect, future
     * or corrupted saved data automatically restores canonical defaults.
     */
    boolean restore(String json) {
        if (json == null) { restoreDefaults(); return false; }
        String trimmed = json.trim();
        if (trimmed.length() < 2 || trimmed.charAt(0) != '[' ||
                trimmed.charAt(trimmed.length() - 1) != ']') {
            restoreDefaults();
            return false;
        }
        String[] parts = trimmed.substring(1, trimmed.length() - 1).split(",", -1);
        if (parts.length != SLOT_COUNT) {
            restoreDefaults();
            return false;
        }
        int[] read = new int[SLOT_COUNT];
        for (int i = 0; i < SLOT_COUNT; i++) {
            String part = parts[i].trim();
            if (!part.matches("[0-9]+")) {
                restoreDefaults();
                return false;
            }
            try { read[i] = Integer.parseInt(part); }
            catch (NumberFormatException e) { restoreDefaults(); return false; }
            if (read[i] < 0 || read[i] >= ACTION_NAMES.length) {
                restoreDefaults();
                return false;
            }
        }
        System.arraycopy(read, 0, bindings, 0, SLOT_COUNT);
        if (!menuReachable()) {
            restoreDefaults();
            return false;
        }
        return true;
    }

    static int navigationSlot(int directionRow) {
        switch (directionRow) {
            case 0: return UP;
            case 1: return DOWN;
            case 2: return LEFT;
            case 3: return RIGHT;
            default: return -1;
        }
    }
}
