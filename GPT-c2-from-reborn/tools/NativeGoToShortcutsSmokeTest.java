package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;

public final class NativeGoToShortcutsSmokeTest {
    private static void check(boolean state, String message) {
        if (!state) throw new AssertionError(message);
    }
    private static ArrayList<Integer> ids(Integer... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    public static void main(String[] args) {
        NativeGoToShortcuts shortcuts = new NativeGoToShortcuts();
        check(shortcuts.enabledCount() == 9, "Nine default Go to shortcuts");
        check(shortcuts.visibleId(3) == 3, "Default Camera destination");
        check(shortcuts.visibleId(4) == 4, "Default Video recorder destination");
        check(shortcuts.visualId(8) == 8, "Default Conversations destination");

        // Hiding a shortcut shifts positions but preserves action IDs.
        check(shortcuts.setEnabled(0, false), "Can hide Lock keypad");
        check(shortcuts.visibleId(0) == 1, "Profiles appears at first position");
        check(shortcuts.visibleId(2) == 3, "Camera route remains unchanged");
        check(!shortcuts.isEnabled(0), "Lock keypad must be hidden");
        check(shortcuts.setEnabled(0, true), "Can restore Lock keypad");

        check(shortcuts.move(3, -1), "Can move Camera upwards");
        check(shortcuts.visualId(2) == 3, "Camera moved to position two");
        check(shortcuts.visibleId(2) == 3, "Moving does not change camera destination");
        check(!shortcuts.move(0, -1), "Cannot move beyond first row");
        check(!shortcuts.move(8, 1), "Cannot move beyond last row");
        check(!shortcuts.move(9, -1), "Invalid index rejected");

        ArrayList<Integer> storedOrder = shortcuts.order();
        ArrayList<Integer> enabled = shortcuts.enabledIds();
        NativeGoToShortcuts restored = new NativeGoToShortcuts();
        check(restored.restore(storedOrder, enabled), "Valid preferences restored");
        check(restored.visibleId(2) == 3, "Camera order persisted");

        for (int id = 0; id < 8; id++) {
            check(restored.setEnabled(id, false), "Disable entry " + id);
        }
        check(restored.enabledCount() == 1, "One entry remains");
        check(!restored.setEnabled(8, false), "Last visible item cannot be hidden");
        check(restored.enabledCount() == 1, "Last visible item survives");

        check(!restored.restore(ids(0,1,2,3,4,5,6,7,7), ids(0,1)),
                "Duplicate order rejected");
        check(restored.enabledCount() == 9, "Corrupt order restores defaults");
        check(!restored.restore(ids(0,1,2), ids(1)), "Short order rejected");
        check(!restored.restore(shortcuts.order(), ids(1,1)),
                "Duplicate enabled ID rejected");
        check(!restored.restore(shortcuts.order(), ids()), "Empty visibility rejected");
        check(!restored.restore(shortcuts.order(), ids(9)), "Out-of-range enabled ID rejected");
        check(restored.visibleId(-1) == -1, "Invalid visual index safe");
        check(restored.visualId(9) == -1, "Invalid organisation index safe");

        restored.restoreDefaults();
        check(restored.visibleId(3) == 3 && restored.enabledCount() == 9,
                "Restore defaults resets order and selection");
        System.out.println("NativeGoToShortcuts: all smoke tests passed");
    }
}
