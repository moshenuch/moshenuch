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

    private static final class Probe implements NativeGoToRouter.Handler {
        String result = "";
        @Override public void lockKeypad() { result = "LOCK"; }
        @Override public void openPage(String page) { result = "PAGE:" + page; }
        @Override public void capture(boolean video) {
            result = video ? "CAMERA:VIDEO" : "CAMERA:PHOTO";
        }
        @Override public void browser() { result = "BROWSER"; }
    }

    private static String routed(int stableId) {
        Probe probe = new Probe();
        check(NativeGoToRouter.launch(stableId, probe), "Known route " + stableId);
        return probe.result;
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
        System.out.println("PASS: legacy shortcut navigation, safe ordering and corruption cases");

        String[] expected = {
                "LOCK", "PAGE:profiles", "PAGE:alarm", "CAMERA:PHOTO",
                "CAMERA:VIDEO", "PAGE:calculator", "BROWSER",
                "PAGE:music", "PAGE:conversations"
        };
        for (int id = 0; id < expected.length; id++)
            check(expected[id].equals(routed(id)), "Correct dispatch ID " + id);
        check(!NativeGoToRouter.launch(-1, new Probe()), "Invalid ID rejected");
        check(!NativeGoToRouter.launch(9, new Probe()), "Out of range ID rejected");
        check(!NativeGoToRouter.launch(0, null), "Null handler rejected");
        System.out.println("PASS: all nine real app dispatch routes, including VIDEO=true");

        // Serialize into exact JSON integer-array strings that C2PhoneView
        // persists as goto_order_v1 and goto_visible_v1. Reconstruct a new
        // instance to simulate process shutdown/restart.
        NativeGoToShortcuts beforeRestart = new NativeGoToShortcuts();
        check(beforeRestart.move(4, -1), "Move video earlier");
        check(beforeRestart.setEnabled(0, false), "Hide lock shortcut");
        check(beforeRestart.setEnabled(2, false), "Hide alarm");
        String savedOrder = beforeRestart.serializedOrder();
        String savedShown = beforeRestart.serializedEnabled();
        check(savedOrder.startsWith("[") && savedOrder.endsWith("]"),
                "Android preference order encoded as a JSON array");
        check(savedShown.startsWith("[") && savedShown.endsWith("]"),
                "Android preference visible IDs encoded as a JSON array");
        NativeGoToShortcuts afterRestart = new NativeGoToShortcuts();
        check(afterRestart.restoreSerialized(savedOrder, savedShown),
                "Restore serialized SharedPreferences into a new instance");
        check(savedOrder.equals(afterRestart.serializedOrder()),
                "Persisted visual order unchanged");
        check(savedShown.equals(afterRestart.serializedEnabled()),
                "Persisted hidden shortcuts unchanged");
        for (int position = 0; position < afterRestart.enabledCount(); position++) {
            int stableId = afterRestart.visibleId(position);
            check(expected[stableId].equals(routed(stableId)),
                    "Reordered/hidden shortcut keeps original route " + stableId);
        }
        int videoVisiblePosition = afterRestart.enabledIds().indexOf(4);
        check(videoVisiblePosition >= 0, "Video must remain enabled");
        check("CAMERA:VIDEO".equals(routed(afterRestart.visibleId(videoVisiblePosition))),
                "Moved video opens VIDEO capture, never photo");
        int cameraVisiblePosition = afterRestart.enabledIds().indexOf(3);
        check(cameraVisiblePosition >= 0 &&
                "CAMERA:PHOTO".equals(routed(afterRestart.visibleId(cameraVisiblePosition))),
                "Camera stays PHOTO after hiding other rows");
        System.out.println("PASS: actual SharedPreferences JSON roundtrip and stable destinations");

        // This tests model reset semantics. The Android Yes/No softkey hookup
        // is separately verified by the build-side source-wiring check.
        String beforeConfirmation = afterRestart.serializedOrder();
        check(!beforeConfirmation.equals(new NativeGoToShortcuts().serializedOrder()),
                "Test state changed before confirmation");
        check(beforeConfirmation.equals(afterRestart.serializedOrder()),
                "No confirmation does not mutate saved state");
        afterRestart.restoreDefaults(); // Used only by the confirmed-Yes app branch.
        check(afterRestart.serializedOrder().equals(new NativeGoToShortcuts().serializedOrder())
                && afterRestart.serializedEnabled().equals(
                        new NativeGoToShortcuts().serializedEnabled()),
                "Confirmed Restore defaults resets order AND hidden flags");
        System.out.println("PASS: confirmed Restore defaults resets order and visibility");

        NativeGoToShortcuts invalid = new NativeGoToShortcuts();
        check(!invalid.restoreSerialized("[1,1,2,3,4,5,6,7,8]", "[0,1]"),
                "Reject duplicate JSON IDs");
        check(!invalid.restoreSerialized("[1,2]", "[0]"), "Reject truncated JSON");
        check(!invalid.restoreSerialized("garbage", "[]"), "Reject corrupted JSON");
        check(!invalid.restoreSerialized("[0,1,2,3,4,5,6,7,8]", "[]"),
                "Reject zero visible shortcuts");
        check(!invalid.restoreSerialized("[0,1,2,3,4,5,6,7,8]", "[0,0]"),
                "Reject repeated selected item");
        check(!invalid.restoreSerialized("[0,1,2,3,4,5,6,7,8]", "[9]"),
                "Reject invalid selected ID");
        check(invalid.enabledCount() == 9, "Invalid prefs restore safe default");
        check(invalid.restoreSerialized(" [ 0,1,2,3,4,5,6,7,8 ] ",
                " [ 1, 3, 4, 8 ] "), "Accept legacy JSON whitespace");
        check(invalid.enabledCount() == 4, "Correct visible number after legacy parse");
        System.out.println("PASS: malformed and legacy SharedPreferences compatibility");

        System.out.println("NativeGoToShortcuts: all smoke tests passed");
    }
}
