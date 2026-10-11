# Nokia C2-01 native SMS conversation audit — bulk read/unread

**Date:** 11 October 2026
**Build:** APK v1.37.0
**HTML reference:** v6.343
**Emulator reference:** Pixel 7 / Android 13 (previous user audits)

## Defect confirmed by source audit of v1.36.0

The Conversation Options menu implemented "Mark", "Mark all", "Unmark all",
"Mark as read", and "Mark as unread", but the two read-state actions called
`setSmsReadStateLocally(m, unread)` only for `conversationMessageAt(sel)`.
This disregarded `markedSmsIds`, despite the native Delete action already
operating on marked records. Mark all was therefore misleading.

## Correction

- In the active thread, if NOTHING is marked, apply read/unread only to the
  highlighted received SMS; sent rows have no read-state option.
- If ANY message is marked, act only on MARKED received SMS from the active
  conversation, regardless of which row is highlighted. A marked sent row
  is never a read/unread target.
- If marks contain only sent SMS (or do not belong to the open thread),
  do not silently fall back to the highlighted incoming SMS.
- Expose BOTH Mark as read and Mark as unread when multiple marked incoming
  messages can have mixed initial states.
- Call `setSmsReadStateLocally` for each selected ID and persist the app-only
  overrides once. Do not write to Android Telephony SMS provider.
- Unread status icons and red dots continue to use these local overrides.
- The action shows a count of received messages processed.

## Automated tests

The GitHub Actions build step `Test bulk SMS marking and production wiring`
runs Java model tests and Python source-wiring assertions and uploads
`GPT-c2-from-reborn-SMS-bulk-audit-v1.37.0`.

Java regression fixtures assert:
- Aaron West conversation contains two inbound and one outbound SMS.
- No marks: only highlighted incoming row changes.
- Highlight sent row + marked incoming row: incoming row is target.
- Mark all includes sent but only inbound messages get read-state changes.
- Sent-only marks do not fall back to the highlighted inbound row.
- Stale/cross-thread marks never touch another thread.
- Duplicate/null/empty selections behave safely.
- Marking Aaron's incoming rows as read leaves two other unread seeded SMS.

Python wiring assertions cover:
- Real Conversation Options exposing mixed selection actions.
- Real select/execute path calls the Java-tested utility.
- Real per-message updates persist app-local read overrides.
- Sent messages are rejected from read-state mutation.
- No Telephony provider writes through the action.

## Emulator test steps

1. Install v1.37.0 and open Settings > Demo data (debug) >
   Seed / reset sample data. Observe yellow DEMO label.
2. Open Messaging > Conversations > Aaron West. There should be THREE
   messages, one sent and two incoming (the newest received is unread).
3. Highlight the newer unread incoming SMS and choose Options > Mark.
4. Highlight the SENT reply. Options should still offer BOTH
   Mark as read and Mark as unread; choose Mark as read.
5. Return to Conversations. Aaron's unread red dot should clear if no
   other unread Aaron SMS remain; Leah and unknown-sender dots remain.
6. Reopen Aaron, choose Options > Mark all, then Mark as unread.
   BOTH received Aaron messages should become unread, sent stays sent.
7. Mark only the sent reply and choose Mark as read (if shown); there
   must NOT be any received-message read-state change from sent-only marks.
8. Use Options > Unmark all. Highlight a single incoming read SMS:
   its Options should offer Mark as unread, not a bulk set of actions.
9. Open Leah Hart: its own unread state must remain independent.
10. Re-seed demo data: the original three unread incoming SMS return.
11. Clear demo data (confirm Yes): fiction disappears; real Android
    Contacts/SMS remain untouched.

## Remaining qualification

Build and source tests establish target semantics and code wiring only.
They cannot certify actual touchscreen/keypad navigation, 240x320
Nokia icon alignment, or Android permission/provider behaviour.
