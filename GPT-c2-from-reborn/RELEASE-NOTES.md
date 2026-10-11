GPT c2 from reborn — APK v1.37.0 / HTML reference v6.343

Native Android Java/Canvas APK (NOT a WebView wrapper).

v1.37.0 — correct SMS Conversation bulk read/unread actions

Audit finding from the debug-seeded SMS path:
- In APK v1.36.0, Conversation > Options > Mark / Mark all selected SMS
  but "Mark as read" and "Mark as unread" still applied ONLY to the
  highlighted SMS. The marked received messages were ignored.
- Conversation now applies Mark as read/unread to ALL marked incoming SMS
  within that conversation; if no items are marked it applies to the
  highlighted incoming message as before.
- Sent/outgoing SMS are deliberately excluded from read/unread overrides,
  even if they are part of a Mark all selection. No phantom unread envelope
  is assigned to outgoing messages.
- When the selected row is a SENT message but incoming messages are marked,
  both "Mark as read" and "Mark as unread" appear and work on the marked
  received messages. This also handles mixed existing unread/read states.
- Marked sent-only rows or stale marks from another conversation do not
  accidentally fall back to the highlighted received message.
- Existing app-local Delete uses its prior mark-selection semantics.
  Android's real SMS content provider is NOT modified by mark/read/delete.
- The action displays the count of received SMS whose app-local read state
  was set; the Nokia unread indicator and conversation red dot derive from
  the same persisted local state overrides.

Regression evidence:
- New NativeSmsBulkSelection is pure Java and is called by the actual
  Conversation Options and action handler, not only a test-only copy.
- Java tests consume the v1.35.0 fictional SMS fixtures and verify
  single message, Mark all with mixed sent/received, marked selections
  while a sent row is highlighted, mixed threads, sent-only marks,
  unknown/empty/duplicate IDs and unread-count isolation.
- Python source-wiring checks fail CI unless the real Conversation Options,
  marked bulk target loop, provider-safe read-state setter and preference
  persistence call this tested selection logic.
- Passing tests are published to GitHub Actions Job Summary and a
  separate artifact GPT-c2-from-reborn-SMS-bulk-audit-v1.37.0.

Prior features retained:
- v1.36.0 Go to shortcut persistence/dispatch self-tests and debug logcat.
- v1.35.0 reversible debug seed: 12 Contacts, 12 SMS, six conversations.
- v1.34.0 Go to Organise / native Camera and Video shortcuts.
- v1.33.0 native main menu layout views and Organise.
- v1.32.0 SMS Use detail extraction with CI tests.
- v1.31.0 live unread SMS refresh; v1.29.0 Contacts name/number search.

Recommended Pixel 7 emulator manual check:
Settings > Demo data (debug) > Seed/reset; Messaging > Conversations;
open Aaron West (two received, one sent). Mark all; open Options and
choose Mark as read, then Mark as unread; check envelope and red-dot state
and count. Highlight the sent reply with received messages marked.
Select individual incoming messages, test Mark/Unmark and re-seeding.
See AUDIT-SMS-BULK-READ-APK-v1.37.0.md for detailed test cases.

Limitations:
- CI validates the Java target-selection model, app source wiring and
  Android compilation. Touch interaction, exact Nokia icons and Android
  SMS provider behaviour still require real-device/emulator testing.
- Stable APK signing has not been enabled; Android may require uninstalling
  the older debug-signed APK before installing this version.
