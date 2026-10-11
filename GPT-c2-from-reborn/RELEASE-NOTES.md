GPT c2 from reborn — APK v1.35.0 / HTML reference v6.343

Native Android Java/Canvas application; NOT a WebView.

v1.35.0 — reversible DEBUG-ONLY emulator audit data
- In debuggable builds, Settings has a new final item: "Demo data (debug)".
  This entry is absent in non-debuggable production/release builds.
- Select "Seed / reset sample data" to switch the APK to an isolated,
  fictional dataset and jump directly to Messaging > Conversations.
- 12 alphabetical sample Contacts, including Aaron West and Leah Hart with
  primary/secondary phone numbers. Useful for scrolling, name search,
  alternate-number search and contact-detail Options.
- 12 SMS in six conversations: received/sent, read/unread, saved/unknown
  senders and a service-style sender; three deliberately unread SMS trigger
  the Nokia closed envelope, status-bar icon and conversation red dots.
- The newest Aaron West SMS contains +44-formatted phone numbers, an email,
  a web link, a six-digit verification code and an ISO-formatted date. This
  supports controlled testing of Options > Use detail and parser exclusions.
- All fixtures are in-memory: no ContactsContract or Telephony inserts,
  updates or deletions. Fake IDs are in reserved high ranges and never
  mistaken for normal SMS/provider IDs.
- Both real and demo messages are NEVER mixed in one view. Demo works even
  on a fresh Android emulator without Contacts or SMS permissions.
- Explicitly block real outgoing phone calls and SMS sends and all contact
  create/edit/delete actions while demo mode is active.
- "DEMO" appears in the Nokia status bar and "DEMO DATA (DEBUG)" on Home.
- Demo mode persists over APK restarts for continuing an audit, but pressing
  "Seed / reset" restores the original fixture read/unread/hidden state.
- "Clear demo data" requires Yes/No confirmation, discards all synthetic
  records and fake SMS read/hidden overrides, returns to real provider
  records, and leaves existing Android contacts/SMS untouched.
- Dedicated pure-Java CI smoke tests verify sample counts, ordering,
  multi-number contacts, 6 threads, read/unread/sent mix, fake ID isolation,
  and phone/email/web extraction from the supplied newest SMS.

Audit steps: see EMULATOR-DEMO-AUDIT-GUIDE.md.

Earlier native changes retained:
- v1.34.0 Go to Organise/Camera/Video shortcuts.
- v1.33.0 4 Menu view modes and main-menu Organise.
- v1.32.0 SMS Use detail picker; v1.31.0 live unread indicators.
- v1.29.0 Contacts multi-tap/number search.

Verification and limitations:
- CI validates source, Java tests and APK packaging only; it does not prove
  that every Pixel 7 Android 13 screen has been physically exercised.
- Demo data is a UI/audit fixture, not real SMS delivery or contacts sync.
- User-created app drafts/templates or other app-owned changes remain
  governed by normal app persistence; seeding does not wipe those.
- The legacy GitHub runner debug signing changes certificate per release:
  uninstall the old APK first on Android package conflicts.
- The stable signing PR remains separate and unconfigured.
