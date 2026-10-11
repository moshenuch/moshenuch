# Native APK v1.30.0 — SMS Saved items / Copy as template audit

11 October 2026 · HTML reference v6.343.

## Observed gap in v1.29.0
- Inbox and opened-message Options showed "Copy as template" but dispatched
  it to "Not implemented". Conversation options did not expose it.
- Messaging > Saved items was always an empty array and opening the list
  fell through to a generic placeholder ("Saved message.").
- Inbox opening and some Inbox Options could fall back to the previously
  opened SMS when SMS permission or current selection was unavailable.

## Implemented in native Android Java
- `savedSmsTemplates` is persisted to `SharedPreferences` as a JSON array
  under `sms_templates_v1`; it is loaded at application launch.
- "Copy as template" in Inbox, opened SMS (including Sent) and per-SMS
  conversation Options copies only the actual selected message's body.
- Saved items displays real user-copied text; no fabricated messages.
- Selecting a saved text opens Compose with the body, but not the recipient.
  This never calls `SmsManager` until Send is explicitly requested.
- Saved items Options offer Details, Use template, New message, Memory
  status, and destructive Delete / Delete all with Yes/No confirmation.
- Deletions remove app-local copies, not Android phone SMS. The source
  SMS remains in its original Inbox or Sent items database location.
- Saved items folder and memory statistics reflect actual stored copies.
- Inbox selection guard prevents accidentally opening the last SMS ID
  from a different screen when the selected Inbox row is invalid.
- Invalid/blank source texts and invalid selection are rejected with
  explicit notices. Template count capped at 100.

## Static checks before CI
1. All three copy-action paths (Inbox/messageRead/conversation) reach
   `saveSmsAsTemplate`.
2. All app-local templates survive app restart through
   `saveStringList` / `loadStringList`.
3. All saved item list indices refer to `savedSmsTemplates`, not current
   provider SMS ID.
4. Selecting a template uses `go("compose")`, and SMS transmit is only
   triggered by explicit Send inside the compose code.
5. Yes/No confirm actions only remove entries from app-local array.
6. Empty Inbox and missing SMS permission never reuse
   `activeMessageId` to navigate to an unrelated SMS.

## Remaining verification and limits
- Run real Android phone test copying incoming, outgoing and conversation
  SMS; check templates persist after relaunch and without SMS permission.
- Test multiline and Unicode messages, very long texts, 100 item capacity,
  no messages, and out-of-range selections.
- Verify cancelling Delete doesn't alter templates, and accepted Delete
  removes only selected template.
- Verify backing out of a prefilled unsent Compose saves a draft under
  existing draft semantics, not a sent message.
- Native `Saved items` uses app-local copied-text templates, not an exact
  Nokia Series 40 provider folder implementation.
- SMS "Move", "Use detail", and other advertised Options still need
  independently verified actions. HTML v6.343 sprite/Options parity
  remains incomplete.
- No physical-device verification performed; CI will only verify
  Java compilation and successful APK publishing.

## Signing
Latest build is still a disposable GitHub debug APK (separate draft
PR #2 has not enabled stable signing), so a prior installed C2 package
may need a one-time uninstall due to signature mismatch.
