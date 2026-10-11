# Nokia C2-01 — Conversations sender-name overlap

**Issue:** SMS envelope icon obscures the first character of every sender.
**User evidence:** v1.37.0 seeded data, Pixel 4 / Android 11 emulator.
**Fix:** Native APK v1.39.0; HTML comparison reference v6.343.

## Confirmed root cause

In the native Canvas `drawListPage()`, the SMS thread icon uses the
rectangle x=3..29. Conversation title text previously started at x=20.
Thus the envelope bitmap covered part of the initial glyph on every row,
including the L in Leah Hart, the + in unsaved +447700900799, and the N
in NOKIA. This was an actual overlap in the fixed 240px screen, not a
Contacts mapping or text-data truncation issue.

## Production changes

- Conversation name and preview start at x=37 (8px after icon right).
- Icon remains at x=3..29 and vertically y+11..y+37.
- Unread dot retains centre x=226, radius 4; name/preview end no
  later than x=218, 4px before the dot's left edge x=222.
- During Mark/Mark all mode, the existing checkbox occupies x=198..216;
  names and previews end by x=192 (6px before checkbox left).
- Both name and subtitle use Android TextUtils END truncation, preserving
  the leading contact name/phone number characters.
- The coordinates are shared with CI-tested NativeConversationRowLayout.
- The underlying SMS data and icons, thread order, read/unread logic,
  marked selection logic, and Use detail are not modified.

## Build-side checks

GitHub Actions step `Test Conversations sender/icon spacing` compiles and
executes NativeConversationRowLayoutSmokeTest and the Python production
renderer audit audit_conversation_row_layout.py. Assertions cover:
1. The label never intersects x=3..29 envelope bitmap.
2. At least 6px separation remains, in fact 8px.
3. Unmarked label leaves room for right-side unread red dot.
4. Marked label leaves room for a selection checkbox.
5. Unread red dot stays inside the 240px Nokia LCD.
6. Demo fixtures contain Leah Hart, sender +447700900799 and NOKIA.
7. The actual native drawListPage uses the shared tested dimensions.
8. The actual sender and message-preview text are end-ellipsized.

Build report artifact:
`GPT-c2-from-reborn-Conversations-layout-audit-v1.39.0`

Passing CI demonstrates mathematical non-overlap and correct source
integration, not an independently captured emulator screenshot.

## Re-test on Pixel 4 / Android 11

1. Install v1.39.0 and navigate to Settings > Demo data (debug) >
   Seed / reset sample data.
2. Open Messaging > Conversations. Verify the FULL label **Leah Hart**,
   not "eah Hart". Check the unknown sender's leading **+** and
   the service sender's leading **N** in **NOKIA**.
3. Check unread message indicators remain unchanged: exactly three
   unread conversation dots immediately after reseeding.
4. Open Conversations > Options > Mark all. Verify the envelope, entire
   left edge of each title, right-side checkbox and red dot do not overlap.
5. Try longer sender names: they should be truncated at the **end**,
   not the first character. Preview text should also fit within the row.
6. Open a thread and select Options > Use detail; confirm the old
   phone/email/web parsing still works, then Back to the thread.
7. If found, attach screenshot with APK version, row label, selection
   state and emulator model/Android version.

## Scope of this release

This fix is specifically for thread-list icon vs text layout. It does not
claim that unrelated Nokia screens, other icons or the entire HTML
reference have been audited or visually matched.
