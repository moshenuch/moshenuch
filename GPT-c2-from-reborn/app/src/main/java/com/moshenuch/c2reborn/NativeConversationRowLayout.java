package com.moshenuch.c2reborn;

/**
 * Pixel coordinates of the Nokia 240x320 Conversations list row.
 *
 * Icon at x=3..29 must not overlap the text. The right side holds the
 * unread red dot at x=226 or, while using Mark/Mark all, a checkbox
 * at x=198..216. Text is END-ellipsized inside its allotted span.
 *
 * These coordinates are used by C2PhoneView and verified in Java CI.
 */
final class NativeConversationRowLayout {
    static final float LCD_WIDTH = 240f;
    static final float ICON_LEFT = 3f;
    static final float ICON_RIGHT = 29f;
    static final float LABEL_X = 37f; // 8px after the icon's right edge

    static final float MARK_CHECKBOX_LEFT = 198f;
    static final float MARK_CHECKBOX_RIGHT = 216f;
    static final float UNREAD_DOT_CENTRE_X = 226f;
    static final float UNREAD_DOT_RADIUS = 4f;

    static final float TEXT_RIGHT_NORMAL = 218f; // 4px before unread dot
    static final float TEXT_RIGHT_MARKING = 192f; // 6px before checkbox

    private NativeConversationRowLayout() { }

    static float labelMaxWidth(boolean showingMarkCheckbox) {
        return (showingMarkCheckbox ? TEXT_RIGHT_MARKING : TEXT_RIGHT_NORMAL)
                - LABEL_X;
    }

    static boolean nonOverlapping() {
        return ICON_LEFT >= 0f
                && LABEL_X > ICON_RIGHT
                && labelMaxWidth(false) > 0f
                && labelMaxWidth(true) > 0f
                && TEXT_RIGHT_NORMAL < UNREAD_DOT_CENTRE_X - UNREAD_DOT_RADIUS
                && TEXT_RIGHT_MARKING < MARK_CHECKBOX_LEFT
                && MARK_CHECKBOX_RIGHT < UNREAD_DOT_CENTRE_X - UNREAD_DOT_RADIUS
                && UNREAD_DOT_CENTRE_X + UNREAD_DOT_RADIUS < LCD_WIDTH;
    }
}
