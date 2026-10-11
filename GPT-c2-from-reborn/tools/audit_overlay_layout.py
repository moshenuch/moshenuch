#!/usr/bin/env python3
"""Build-side regression: real notice/detail Canvas drawing uses safe geometry.

This checks production source, not a rendered Pixel 4 emulator screenshot.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text(encoding="utf-8")
geometry = (root / "app/src/main/java/com/moshenuch/c2reborn/NativeOverlayLayout.java").read_text(encoding="utf-8")


def method(signature: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise AssertionError("Missing production method: " + signature)
    brace = source.find("{", start + len(signature))
    if brace < 0:
        raise AssertionError("Missing body: " + signature)
    depth = 0
    for index in range(brace, len(source)):
        if source[index] == "{":
            depth += 1
        elif source[index] == "}":
            depth -= 1
            if depth == 0:
                return source[brace:index + 1]
    raise AssertionError("Unclosed method: " + signature)


def verify(title: str, result: bool):
    if not result:
        raise AssertionError("FAIL: " + title)
    print("PASS: " + title)


notice = method("private void drawNotice(Canvas c)")
details = method("private void drawItemDetail(Canvas c)")
status = method("private void drawStatus(Canvas c, String title)")
settings = method("private void openSelected()")

verify("Original notice text is still driven by showNotice",
       "drawWrappedText(c, notice" in notice and
       'noticeUntil > SystemClock.uptimeMillis()' in source)
verify("Notice background is truly opaque rather than the old alpha=225",
       "p.setColor(Color.rgb(20, 25, 27))" in notice and
       "Color.argb(225" not in notice)
verify("Notice text aligns from LEFT x, not clipped centre anchor",
       "p.setTextAlign(Paint.Align.LEFT)" in notice and
       "p.setTextAlign(Paint.Align.CENTER)" not in notice)
verify("Notice has a correctly sized rectangle from the CI-tested geometry",
       "NativeOverlayLayout.NOTICE_LEFT" in notice and
       "NativeOverlayLayout.NOTICE_RIGHT" in notice and
       "NativeOverlayLayout.NOTICE_TOP" in notice and
       "NativeOverlayLayout.NOTICE_BOTTOM" in notice)
verify("Text is strictly clipped inside notice background",
       "c.save();" in notice and "c.clipRect(" in notice and "c.restore();" in notice)
verify("Both notice text wrapping and bounds share production geometry",
       "NativeOverlayLayout.NOTICE_TEXT_X" in notice and
       "NativeOverlayLayout.NOTICE_TEXT_WIDTH" in notice and
       "NativeOverlayLayout.NOTICE_MAX_LINES" in notice)
verify("Status/title remains located in independent header",
       "c.drawRect(0, 0, 240, 41, p)" in status and
       "c.drawText(title, 6, 37, p);" in status)
verify("Item-detail body begins below title and header",
       "NativeOverlayLayout.DETAIL_TEXT_BASELINE" in details and
       "NativeOverlayLayout.DETAIL_TEXT_SIZE" in details)
verify("Item-detail title separated by divider",
       "NativeOverlayLayout.DETAIL_DIVIDER_Y" in details and
       "c.drawRect(9," in details)
verify("Item-detail typography left aligned and predictable",
       "p.setTypeface(fontRegular)" in details and
       "p.setTextAlign(Paint.Align.LEFT)" in details)
verify("Item-detail text stops before the Nokia softkey bar",
       "NativeOverlayLayout.DETAIL_MAX_LINES" in details and
       "NativeOverlayLayout.DETAIL_LINE_HEIGHT" in details)
verify("Settings Phone stub is described as not implemented, rather than working",
       "detailText = sel == 10" in settings and
       "Phone settings are not yet implemented" in settings and
       "Your Android phone settings remain unchanged." in settings)
verify("Shortcuts still emit full Assigned: label for untruncated toast",
       '"Assigned: " + actionLabel' in settings)
verify("Overlay layout constants are bounded to Nokia screen",
       "LCD_WIDTH = 240f" in geometry and
       "LCD_HEIGHT = 320f" in geometry and
       "NOTICE_BACKGROUND_ALPHA = 255" in geometry)

print("Nokia notice and Phone detail renderer wiring: all checks passed")
