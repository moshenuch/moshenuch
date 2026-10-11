#!/usr/bin/env python3
"""Build-gate that ensures the 240px Conversations renderer uses tested geometry.

This is an actual production Java source-wiring check, not screenshot proof.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
view = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text(
    encoding="utf-8")
geometry = (root / "app/src/main/java/com/moshenuch/c2reborn/NativeConversationRowLayout.java").read_text(
    encoding="utf-8")

start = view.find("private void drawListPage(Canvas c)")
stop = view.find("private void drawConversation(Canvas c)", start)
if start < 0 or stop < 0:
    raise AssertionError("Missing native Conversations row renderer")
renderer = view[start:stop]

def check(label, ok):
    if not ok:
        raise AssertionError("FAIL: " + label)
    print("PASS: " + label)

check("Conversation label starts after icon, not at old x=20",
      'if ("conversations".equals(page)) textX = NativeConversationRowLayout.LABEL_X;' in renderer
      and 'if ("conversations".equals(page)) textX = 20;' not in renderer)
check("SMS thread icon uses shared left/right geometry",
      "NativeConversationRowLayout.ICON_LEFT, y + 11" in renderer and
      "NativeConversationRowLayout.ICON_RIGHT, y + 37" in renderer)
check("Unread red dot uses tested shared position and radius",
      "NativeConversationRowLayout.UNREAD_DOT_CENTRE_X" in renderer and
      "NativeConversationRowLayout.UNREAD_DOT_RADIUS" in renderer)
check("Marked checkbox uses tested shared position",
      "NativeConversationRowLayout.MARK_CHECKBOX_LEFT" in renderer)
check("Contact/number/NOKIA title uses END ellipsize not START ellipsize",
      'if ("conversations".equals(page)) {' in renderer and
      "rowText = android.text.TextUtils.ellipsize(rowText" in renderer and
      "android.text.TextUtils.TruncateAt.END" in renderer)
check("Title width changes when Mark / Mark all checkboxes are shown",
      "NativeConversationRowLayout.labelMaxWidth(!markedThreadIds.isEmpty())" in renderer)
check("Conversation SMS preview uses same left margin",
      "c.drawText(sub, textX, y + 40, p);" in renderer)
check("Preview also end-ellipsized before right-side indicators",
      'sub = android.text.TextUtils.ellipsize(sub,' in renderer and
      renderer.count("NativeConversationRowLayout.labelMaxWidth(!markedThreadIds.isEmpty())") >= 2)
check("Layout is constrained to Nokia 240 pixel LCD",
      "LCD_WIDTH = 240f" in geometry and
      "LABEL_X = 37f" in geometry)
print("Native Conversations row production wiring: all checks passed")
