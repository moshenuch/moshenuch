package com.moshenuch.c2reborn;

/**
 * Pure Java geometry regression for Pixel 4 / Android 11:
 * 1. "Assigned: Messaging" toast clipped its first character.
 * 2. Settings > Phone detail header overlapped body text.
 *
 * Paint alignment, opacity and actual production wiring are independently
 * checked by tools/audit_overlay_layout.py before Gradle compilation.
 */
public final class NativeOverlayLayoutSmokeTest {
    private static void check(boolean state, String explanation) {
        if (!state) throw new AssertionError(explanation);
    }

    public static void main(String[] args) {
        check(NativeOverlayLayout.LCD_WIDTH == 240f,
                "Expected Nokia 240px LCD");
        check(NativeOverlayLayout.LCD_HEIGHT == 320f,
                "Expected Nokia 320px LCD");
        check(NativeOverlayLayout.noticeTextFits(),
                "Toast text box must fit entirely inside opaque notice rectangle");
        check(NativeOverlayLayout.NOTICE_TEXT_X >
                NativeOverlayLayout.NOTICE_LEFT,
                "Notice uses an inset left text edge, not a centred anchor");
        check(NativeOverlayLayout.NOTICE_TEXT_X +
                NativeOverlayLayout.NOTICE_TEXT_WIDTH <
                NativeOverlayLayout.NOTICE_RIGHT,
                "Notice has right-side padding");
        check(NativeOverlayLayout.NOTICE_MAX_LINES == 2,
                "Notice has room for two wrapped message lines");
        check(NativeOverlayLayout.NOTICE_BACKGROUND_ALPHA == 255,
                "Opaque toast prevents underlying list from bleeding through");
        System.out.println("PASS: notice foreground box, left-edge text bounds and full opacity");

        check(NativeOverlayLayout.HEADER_BOTTOM == 41f,
                "Nokia title/status region ends at 41px");
        check(NativeOverlayLayout.detailTextFits(),
                "Information body begins below divider and ends above bottom softbar");
        check(NativeOverlayLayout.DETAIL_TEXT_BASELINE -
                NativeOverlayLayout.DETAIL_TEXT_SIZE >
                NativeOverlayLayout.HEADER_BOTTOM + 10f,
                "Phone information first glyph cannot touch title/status header");
        check(NativeOverlayLayout.DETAIL_TEXT_BASELINE >
                NativeOverlayLayout.TITLE_BASELINE + 30f,
                "Phone detail body uses a separate text baseline");
        check(NativeOverlayLayout.DETAIL_TEXT_BASELINE +
                (NativeOverlayLayout.DETAIL_MAX_LINES - 1) *
                NativeOverlayLayout.DETAIL_LINE_HEIGHT <
                NativeOverlayLayout.SOFTBAR_TOP - 20f,
                "Longest detail body does not overlap Nokia softkey bar");
        System.out.println("PASS: Settings Phone and item-detail title/body separation");

        System.out.println("NativeOverlayLayout: all smoke tests passed");
    }
}
