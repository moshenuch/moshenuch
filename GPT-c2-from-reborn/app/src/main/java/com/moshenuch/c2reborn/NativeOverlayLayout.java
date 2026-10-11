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

    static final float NOTICE_LEFT = 18f;
    static final float NOTICE_TOP = 123f;
    static final float NOTICE_RIGHT = 222f;
    static final float NOTICE_BOTTOM = 177f;
    static final float NOTICE_TEXT_X = 30f;
    static final float NOTICE_TEXT_BASELINE = 145f;
    static final float NOTICE_TEXT_WIDTH = 180f;
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

    // Calculator/Loan display panels must begin below the Nokia title
    // baseline at y=37; their old y=30/35 panels covered title glyphs.
    static final float CALC_PANEL_TOP = 48f;
    static final float CALC_PANEL_BOTTOM = 106f;
    static final float CALC_VALUE_BASELINE = 88f;
    static final float CALC_FIRST_OPERATION_BASELINE = 130f;
    static final float CALC_OPERATION_LINE_SPACING = 24f;
    static final float LOAN_FIRST_FIELD_TOP = 48f;

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

    static boolean calculatorPanelFits() {
        float lastOperation = CALC_FIRST_OPERATION_BASELINE +
                5f * CALC_OPERATION_LINE_SPACING;
        return CALC_PANEL_TOP >= HEADER_BOTTOM + 6f
                && CALC_PANEL_BOTTOM < CALC_FIRST_OPERATION_BASELINE - 11f
                && CALC_VALUE_BASELINE - 28f > CALC_PANEL_TOP + 6f
                && CALC_VALUE_BASELINE + 7f < CALC_PANEL_BOTTOM
                && lastOperation < SOFTBAR_TOP - 12f
                && LOAN_FIRST_FIELD_TOP >= HEADER_BOTTOM + 6f
                && LOAN_FIRST_FIELD_TOP + 2f * 62f + 48f < SOFTBAR_TOP;
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
