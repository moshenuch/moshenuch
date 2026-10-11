GPT c2 from reborn — APK v1.39.0 / HTML reference v6.343

Native Android Java/Canvas (NOT a WebView).

v1.39.0 — correct SMS Conversations icon/name overlap

Reported in the Pixel 4 / Android 11 seeded-demo audit of native
v1.37.0: the yellow SMS envelope icon visually hid the first character
of every sender's display name (Leah Hart rendered "eah Hart", unknown
+44 sender lost its leading "+", NOKIA sender lost "N").

Confirmed root cause in the native Java Canvas renderer:
- Conversation-thread icon occupied x = 3..29 pixels, but its sender
  label started at x = 20, overlapping the bitmap by 9 pixels.
- The shared list renderer did not constrain long conversation labels
  before drawing a right-side unread dot or Mark/Mark all checkbox.

Corrections:
- The sender name and SMS preview now start at x = 37, leaving 8 pixels
  of separation from the SMS icon's right edge.
- End-ellipsize conversation sender names and preview text to a maximum
  of x = 218 in normal mode, stopping 4 pixels before unread-dot edge.
- With conversation Mark/Mark all selection active, names and previews
  end by x = 192 to preserve the checkbox starting at x = 198.
- The envelope icon, unread red dot and checkbox retain their previous
  positions, sizes and unread/marked state behaviour.
- Isolated all these dimensions in production-used
  NativeConversationRowLayout, instead of repeating scattered numbers.
- Added pure-Java CI geometry tests for the 240px Nokia screen, and
  fixture coverage for Leah Hart, +447700900799 and NOKIA sender.
- A production renderer source-wiring audit now fails CI if the icon/name
  overlap or unbounded conversation title drawing is reintroduced.
- CI publishes a plain-text report named
  GPT-c2-from-reborn-Conversations-layout-audit-v1.39.0.

Verified features from the user's v1.37.0 Pixel 4 audit remain unchanged:
- Debug seed shows 12 Contacts and 12 fictional SMS records with DEMO tags.
- Six Conversations and the correct three unread indicator dots.
- Newest-first conversation items and secondary-number sender matching.
- Use detail extraction: sender, e-mail, web URL, second number, excluding
  six-digit verification codes and ISO dates.
- Bulk Mark all/Mark as unread respects incoming/Sent icon state.
- Back from Use detail returns to the conversation.

Other features retained:
- v1.38.0 actual Settings > My shortcuts home-key assignments.
- v1.37.0 conversation bulk Mark as read/unread correction.
- v1.36.0 Go to model and emulator audit self-tests.
- v1.35.0 reversible demo Contacts/SMS seed.

Audit and limitations:
- Run Settings > Demo data (debug) > Seed/reset; Messaging >
  Conversations. Check that "Leah Hart", "+447700900799" and "NOKIA"
  show ALL their leading characters with and without Mark/Mark all.
  Full Pixel 4 / Android 11 screenshot verification remains to be done.
- This update addresses spacing only; it does not claim complete icon,
  UI interaction or pixel-perfect HTML v6.343 fidelity.
- Native debug APK signing remains ephemeral on GitHub Actions, so
  uninstalling the previous C2 app may be necessary on package conflict.
