package com.moshenuch.c2reborn;

/**
 * Shared 240x320 LCD geometry for transient notices and information pages.
 * Coordinates are display pixels (not emulator Android screen pixels).
 *
 * A notice is a solid foreground surface, with left-aligned wrapped text.
 * Generic detail bodies begin clearly below the black status/title header.
 * Values are used directly in C2PhoneView and checked in Java CI.
 */
final class NativeOverlayLayout {
    static final float LCD_WIDTH = 240f;
    static final float LCD_HEIGHT = 320f;
    static final float HEADER_BOTTOM = 41f;
    static final float TITLE_BASELINE = 37f;
    static final float SOFTBAR_TOP = 286f;

    static final float NOTICE_LEFT = 35f;
    static final float NOTICE_TOP = 123f;
    static final float NOTICE_RIGHT = 205f;
    static final float NOTICE_BOTTOM = 177f;
    static final float NOTICE_TEXT_X = 48f;
    static final float NOTICE_TEXT_BASELINE = 145f;
    static final float NOTICE_TEXT_WIDTH = 144f;
    static final float NOTICE_TEXT_SIZE = 12f;
    static final float NOTICE_LINE_HEIGHT = 15f;
    static final int NOTICE_MAX_LINES = 2;
    static final int NOTICE_BACKGROUND_ALPHA = 255;

    static final float DETAIL_TEXT_X = 10f;
    static final float DETAIL_TEXT_BASELINE = 72f;
    static final float DETAIL_TEXT_WIDTH = 215f;
    static final float DETAIL_TEXT_SIZE = 13f;
    static final float DETAIL_LINE_HEIGHT = 18f;
    static final int DETAIL_MAX_LINES = 11;
    static final float DETAIL_DIVIDER_Y = 53f;

    private NativeOverlayLayout() { }

    /** A conservative text ascent/descent estimate for safety tests. */
    static boolean noticeTextFits() {
        return NOTICE_LEFT >= 0f && NOTICE_RIGHT < LCD_WIDTH
                && NOTICE_TOP > HEADER_BOTTOM
                && NOTICE_BOTTOM < SOFTBAR_TOP
                && NOTICE_TEXT_X >= NOTICE_LEFT + 8f
                && NOTICE_TEXT_X + NOTICE_TEXT_WIDTH <= NOTICE_RIGHT - 8f
                && NOTICE_TEXT_BASELINE - NOTICE_TEXT_SIZE >= NOTICE_TOP + 4f
                && NOTICE_TEXT_BASELINE +
                    (NOTICE_MAX_LINES - 1) * NOTICE_LINE_HEIGHT + 4f
                        <= NOTICE_BOTTOM - 8f
                && NOTICE_BACKGROUND_ALPHA == 255;
    }

    static boolean detailTextFits() {
        float firstLineTop = DETAIL_TEXT_BASELINE - DETAIL_TEXT_SIZE;
        float lastLineBottom = DETAIL_TEXT_BASELINE +
                (DETAIL_MAX_LINES - 1) * DETAIL_LINE_HEIGHT + 5f;
        return DETAIL_TEXT_X >= 0f
                && DETAIL_TEXT_X + DETAIL_TEXT_WIDTH < LCD_WIDTH
                && DETAIL_DIVIDER_Y > HEADER_BOTTOM
                && firstLineTop > DETAIL_DIVIDER_Y + 4f
                && lastLineBottom < SOFTBAR_TOP - 4f;
    }
}
