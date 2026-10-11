package com.moshenuch.c2reborn;

import java.util.ArrayList;

/**
 * Regression for long Nokia Settings/My shortcuts notices at a real
 * bounded text width. A predictable synthetic measurer tests the wrapping
 * algorithm; Android Paint supplies actual glyph widths in production.
 */
public final class NativeNoticeTextSmokeTest {
    private static final float GLYPH_WIDTH = 7f;
    private static float measure(String text) {
        return text.length() * GLYPH_WIDTH;
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    private static ArrayList<String> layout(String message, float boxWidth) {
        return NativeNoticeText.wrap(message, boxWidth,
                NativeOverlayLayout.NOTICE_MAX_LINES,
                NativeNoticeTextSmokeTest::measure);
    }

    private static void verifyFits(String message, float width, boolean fullyVisible) {
        ArrayList<String> lines = layout(message, width);
        check(!lines.isEmpty(), "Nonempty notice has visible text: " + message);
        check(lines.size() <= 2, "Notice never exceeds two lines");
        for (String line : lines)
            check(measure(line) <= width,
                    "Notice line cannot extend past right edge: " + line);
        if (fullyVisible) {
            check(message.equals(String.join(" ", lines)),
                    "Every character preserved, including first/last: " + message
                        + " -> " + lines);
        }
    }

    public static void main(String[] args) {
        check(NativeOverlayLayout.noticeTextFits(),
                "Widened notice stays within Nokia 240px display");
        check(NativeOverlayLayout.NOTICE_TEXT_WIDTH >= 175f,
                "Notice has enough room for long assignment feedback");

        verifyFits("Assigned: Messaging",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH, true);
        verifyFits("Assigned: Gallery",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH, true);
        verifyFits("Assigned: Video recorder",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH, true);
        verifyFits("Keep Menu on at least one home key",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH, true);
        verifyFits("Keep Menu on at least one home key", 144f, true);
        System.out.println("PASS: complete assignment and Menu protection messages");

        verifyFits("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH, false);
        ArrayList<String> longToken = layout(
                "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                NativeOverlayLayout.NOTICE_TEXT_WIDTH);
        check(longToken.get(0).endsWith("…"),
                "Unbroken long token is ellipsized safely");

        String veryLong = "A much longer message that cannot fit inside "
                + "the two lines in the Nokia dialog under any circumstances "
                + "and therefore needs a visible trailing ellipsis";
        ArrayList<String> truncated = layout(veryLong,
                NativeOverlayLayout.NOTICE_TEXT_WIDTH);
        check(truncated.size() == 2, "Long notice bounded to two lines");
        check(truncated.get(1).endsWith("…"),
                "Overflowing final line signals that text was shortened");
        verifyFits(veryLong, NativeOverlayLayout.NOTICE_TEXT_WIDTH, false);
        check(NativeNoticeText.wrap("", 180, 2,
                NativeNoticeTextSmokeTest::measure).isEmpty(),
                "Empty notice produces no visible text");
        check(NativeNoticeText.wrap(null, 180, 2,
                NativeNoticeTextSmokeTest::measure).isEmpty(),
                "Null notice is safe");
        check(NativeNoticeText.wrap("Hi", 0, 2,
                NativeNoticeTextSmokeTest::measure).isEmpty(),
                "Invalid text width produces no drawing");
        System.out.println("PASS: no left/right clipping, long tokens, overflow and null safety");
        System.out.println("NativeNoticeText: all smoke tests passed");
    }
}
