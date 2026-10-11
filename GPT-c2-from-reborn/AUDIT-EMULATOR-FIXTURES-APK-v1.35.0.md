# Emulator audit fixture implementation — native APK v1.35.0
2026-10-11. Upstream HTML pinned to v6.343.

## Context
The independent Pixel 7 / Android 13 emulator audit of native APK v1.33.0
reported no crashes and confirmed:
- Multiple phone-number contact detail selection and Call/Back softkeys
- Multitap Contacts name search, # numeric search and no-match handling
- Messaging root: Create message, Conversations, Drafts and Outbox
- Create-message 15-item Options menu with Symbol insertion
- Settings list
But the emulator had one seeded Contact and no SMS, preventing verification
of SMS Use detail, message threads, red-dot/envelope state or Contacts list
sorting/scrolling across many contacts.

## Implementation in v1.35.0
A debuggable-only Settings > Demo data (debug) entry allows:
- Seed / reset fictional records (12 alphabetical contacts, 12 SMS, 6 threads)
- Clear all fiction with Yes/No confirmation
- Review the dataset and safety restrictions
- Persist chosen demo state across APK restarts
- Re-seed restores all fixture records and clears only fake SMS read/hidden IDs

The 12 contacts include two with multiple numbers; the messages include
incoming unread/read, outgoing, an unsaved sender, sender "NOKIA", and a
newest SMS with phone numbers, email, HTTPS URL, OTP and date. Fake
provider IDs use 9_300/9_400/9_500 billion ID prefixes, never actual
Android provider IDs. All records are constructed in app memory and
replace—not merge with—real contacts/SMS while demo mode is enabled.

## Data and safety invariants
1. Debug gate uses ApplicationInfo.FLAG_DEBUGGABLE. Non-debuggable builds
   ignore a previously persisted demo flag and never expose its settings UI.
2. Active fixture mode does not call AndroidBackend.loadContacts() or
   AndroidBackend.loadSms() for Contact and SMS screens.
3. Demo record construction never uses ContactsContract, Telephony or
   Android provider insert/update/delete APIs.
4. Calls and outgoing SMS are explicitly disabled while demo mode is on.
5. Contact create/edit/delete and bulk contact deletion are disabled
   while demo mode is on, so fixtures cannot accidentally be written into
   Android contacts, nor result in editing/deleting a genuine contact.
6. App-local read/unread and hidden fixture SMS flags use high fake IDs.
   Seed and clear remove only IDs from the fixture ranges.
7. The fixture mode has an obvious DEMO label in the native Nokia status
   bar and home screen.
8. Clearing switches back to actual Contacts/SMS provider snapshots,
   and restores app-local contacts from prefs when READ_CONTACTS is denied.
9. App-owned drafts, saved SMS templates, notes and original phone
   Contacts/SMS are not wiped by fixture seed/clear.

## Automated Java tests
Standalone NativeAuditFixturesSmokeTest runs in GitHub Actions, verifying:
- 12 distinct alphabetically sorted contacts
- At least two contacts with multiple numbers
- 12 distinct fictional message IDs across exactly six threads
- Three unread received SMS, several read received and outgoing SMS
- Newest message addresses Aaron West's secondary number
- Date and six-digit OTP do not become phone-number detail entries
- Rich Use detail extraction returns phone/email/web categories
- Real provider IDs fall outside fixture reserved ID predicates

## Emulator manual checklist (not automatically verified)
1. Install debuggable v1.35.0; Settings > Demo data (debug).
2. Seed fixture mode, verify DEMO indicator appears in status and Home.
3. Open Conversations: six rows, red dots for three unread conversations.
4. Aaron West > latest SMS > Options > Use detail: phone/email/web items.
5. Confirm OTP/date are excluded by parser, copy details only when requested.
6. Mark as read/unread; verify envelope/dot/status changes and persistence.
7. Contacts > Names: verify twelve alphabetic names, scroll positions.
8. Aaron contact: Up/Down selects two numbers; use Secondary phone search.
9. Try Call, Send SMS and Save Contact: confirm NO real outgoing
   telephony/provider writes occur while demo is active.
10. Re-seed: all 12 SMS reappear with three unread rows.
11. Clear demo data with No then Yes; No retains fixtures, Yes removes them.
12. Verify original actual emulator SMS/Contacts reappear if available.
13. Relaunch after seed and after clear to verify persisted activation state.
14. Compare screenshot and Options ordering to original v6.343 HTML.

## Known limits
This is app-local mock data, not a mutation of the emulator's Android SMS
or Contacts databases. Android notification delivery, actual SMS reception,
provider observers and real Contacts sync still require separate integration
tests. CI passing Java fixtures and Android Gradle compilation does not prove
all Pixel 7 touch interactions or pixel-perfect original Nokia icon parity.
