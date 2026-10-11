package com.moshenuch.c2reborn;

/**
 * Pure Java regression for the same model C2PhoneView persists and dispatches.
 * Android device interactions require separate emulator/handset testing.
 */
public final class NativeHomeShortcutsSmokeTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        NativeHomeShortcuts keys = new NativeHomeShortcuts();
        check(keys.assigned(NativeHomeShortcuts.LSK) == NativeHomeShortcuts.GO_TO,
                "Default left selection opens Go to");
        check(keys.assigned(NativeHomeShortcuts.RSK) == NativeHomeShortcuts.NAMES,
                "Default right selection opens Names");
        check(keys.assigned(NativeHomeShortcuts.CENTRE) == NativeHomeShortcuts.MENU,
                "Default centre opens Menu");
        for (int n = NativeHomeShortcuts.UP; n <= NativeHomeShortcuts.RIGHT; n++)
            check(keys.assigned(n) == NativeHomeShortcuts.NONE,
                    "Default directional key remains unassigned " + n);
        check("[1,2,13,13,13,13,0]".equals(keys.serialize()),
                "Original default bindings serialized unchanged");
        System.out.println("PASS: original Home key behaviour remains default");

        check(keys.assign(NativeHomeShortcuts.UP, NativeHomeShortcuts.CAMERA),
                "Set Navigation up to Camera");
        check(keys.assign(NativeHomeShortcuts.DOWN, NativeHomeShortcuts.VIDEO_RECORDER),
                "Set Navigation down to Video recorder");
        check(keys.assign(NativeHomeShortcuts.LSK, NativeHomeShortcuts.SETTINGS),
                "Set left softkey to Settings");
        check(keys.assign(NativeHomeShortcuts.RSK, NativeHomeShortcuts.MESSAGING),
                "Set right softkey to Messaging");
        check(keys.assigned(NativeHomeShortcuts.UP) == NativeHomeShortcuts.CAMERA,
                "Camera action saved");
        check(keys.assigned(NativeHomeShortcuts.DOWN) == NativeHomeShortcuts.VIDEO_RECORDER,
                "Video action saved");
        check("Settings".equals(keys.label(NativeHomeShortcuts.LSK)),
                "Rendered left softkey label derives from assigned action");
        check("Messaging".equals(keys.label(NativeHomeShortcuts.RSK)),
                "Rendered right softkey label derives from assigned action");
        check(NativeHomeShortcuts.navigationSlot(0) == NativeHomeShortcuts.UP &&
                NativeHomeShortcuts.navigationSlot(1) == NativeHomeShortcuts.DOWN &&
                NativeHomeShortcuts.navigationSlot(2) == NativeHomeShortcuts.LEFT &&
                NativeHomeShortcuts.navigationSlot(3) == NativeHomeShortcuts.RIGHT &&
                NativeHomeShortcuts.navigationSlot(4) == -1,
                "Four navigation rows select correct keys");
        System.out.println("PASS: independent Home key assignments and labels");

        // Preserve last Menu safety, then deliberately move the Menu binding
        // to another key and free the central key for an action.
        check(!keys.assign(NativeHomeShortcuts.CENTRE, NativeHomeShortcuts.PROFILES),
                "Cannot remove final Menu key");
        check(keys.assigned(NativeHomeShortcuts.CENTRE) == NativeHomeShortcuts.MENU,
                "Rejected change does not alter centre assignment");
        check(keys.assign(NativeHomeShortcuts.LEFT, NativeHomeShortcuts.MENU),
                "Assign alternate Menu action first");
        check(keys.assign(NativeHomeShortcuts.CENTRE, NativeHomeShortcuts.PROFILES),
                "Centre becomes configurable once Menu is on another key");
        check(keys.menuReachable(), "Main Menu remains reachable");
        check(!keys.assign(NativeHomeShortcuts.LEFT, NativeHomeShortcuts.NONE),
                "Cannot remove new final Menu binding");
        System.out.println("PASS: Menu access cannot be lost");

        String json = keys.serialize();
        NativeHomeShortcuts relaunched = new NativeHomeShortcuts();
        check(relaunched.restore(json), "Existing SharedPreferences value restores");
        check(json.equals(relaunched.serialize()), "Save/restart preserves all seven bindings");
        check(relaunched.assigned(NativeHomeShortcuts.CENTRE) ==
                NativeHomeShortcuts.PROFILES, "Custom centre key survives restart");
        check(relaunched.assigned(NativeHomeShortcuts.LEFT) ==
                NativeHomeShortcuts.MENU, "Custom Menu key survives restart");
        check(relaunched.restore(" [ 11,3,6,7,0,13,12 ] "),
                "Compatible with whitespace in stored JSON array");
        check(relaunched.menuReachable(), "JSON restore keeps Menu accessible");
        System.out.println("PASS: exact JSON preference roundtrip");

        String[] invalid = {
                null, "", "bad", "[]", "[1,2]", "[1,2,3,4,5,6,7,8]",
                "[1,2,13,13,13,13,13]", // Menu inaccessible
                "[1,2,13,13,13,13,99]", // Out of range
                "[1,2,13,13,13,13,-1]", // Negative binding
                "[1,2,13,13,13,13,\"MENU\"]", // Wrong JSON type
                "[1,2,13,13,13,13,0,]", // Extra empty element
        };
        for (String value : invalid) {
            check(!relaunched.restore(value), "Corrupt preference rejected: " + value);
            check(relaunched.menuReachable(), "Invalid preference keeps Menu accessible");
            check("[1,2,13,13,13,13,0]".equals(relaunched.serialize()),
                    "Invalid preference restores exact defaults");
        }
        check(!relaunched.assign(-1, NativeHomeShortcuts.MENU),
                "Invalid slot refused");
        check(!relaunched.assign(7, NativeHomeShortcuts.MENU),
                "Out of range slot refused");
        check(!relaunched.assign(NativeHomeShortcuts.LSK, 99),
                "Invalid action refused");
        check(relaunched.assigned(-1) == NativeHomeShortcuts.NONE,
                "Invalid slot read safe");
        relaunched.assign(NativeHomeShortcuts.UP, NativeHomeShortcuts.GALLERY);
        relaunched.restoreDefaults();
        check("[1,2,13,13,13,13,0]".equals(relaunched.serialize()),
                "Confirmed reset restores Nokia defaults");
        System.out.println("PASS: corrupt data, key bounds and restore defaults");

        System.out.println("NativeHomeShortcuts: all smoke tests passed");
    }
}
