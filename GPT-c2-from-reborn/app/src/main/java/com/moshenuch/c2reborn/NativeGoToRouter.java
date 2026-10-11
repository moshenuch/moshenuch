package com.moshenuch.c2reborn;

/**
 * Single source of truth for Nokia Go to shortcut action dispatch.
 * Shortcuts are addressed by stable destination ID, never by their current
 * visual position. Android callbacks live in C2PhoneView and MainActivity.
 * This pure Java class is also invoked directly by build-side regression
 * tests with a recording handler, including the Video recorder flag.
 */
final class NativeGoToRouter {
    interface Handler {
        void lockKeypad();
        void openPage(String page);
        void capture(boolean video);
        void browser();
    }

    private NativeGoToRouter() { }

    static boolean launch(int destinationId, Handler handler) {
        if (handler == null) return false;
        switch (destinationId) {
            case 0: handler.lockKeypad(); return true;
            case 1: handler.openPage("profiles"); return true;
            case 2: handler.openPage("alarm"); return true;
            case 3: handler.capture(false); return true;
            case 4: handler.capture(true); return true;
            case 5: handler.openPage("calculator"); return true;
            case 6: handler.browser(); return true;
            case 7: handler.openPage("music"); return true;
            case 8: handler.openPage("conversations"); return true;
            default: return false;
        }
    }
}
