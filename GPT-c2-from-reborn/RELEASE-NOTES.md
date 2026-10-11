GPT c2 from reborn — APK v1.27.0 / HTML reference v6.343

Native Android Java/Canvas (NOT a WebView).

v1.27.0 — Selecting individual contact phone numbers
- Contact details now renders a scrollable Nokia-style list of phone numbers.
  UP/DOWN selects the highlighted number; four numbers display at once
  and the screen shows the selected row's position.
- Green Call and centre OK use the highlighted phone number, including
  secondary numbers that were previously not individually callable.
- Contact Options "Call", "Send message", "Copy number", "Use number",
  "Speed dial", "Add to group" and filter-list actions use the highlighted
  number. The composer opens addressed but no SMS is sent automatically.
- Opening the saved Contact from an SMS or call now selects the exact
  matching secondary number rather than automatically choosing the primary.
- The contact Edit operation remains separate; it continues to edit only
  the primary number so selecting a secondary number will not overwrite it.
- Handles missing contact/number without crashing or silently calling.

v1.26.0 retained:
- One Contacts entry per aggregated Android contact ID.
- Recognises and caches all of a contact's phone-number rows.
- Full-number matching and safe handling of revoked Contacts permission.

Older native functions retained:
- v1.25.0 Contact / Save a contact contextual Options.
- v1.24.x per-message/per-thread app-local read/unread state and indicators.
- Speed dial via on-screen Nokia keypad, Groups, drafts and media.

Verification:
- GitHub Actions compiles and publishes native Android debug APK.
- Actual device interactions and exact HTML visual parity are NOT verified.
- APKs still use GitHub runner debug signatures; Android may require
  uninstalling the older version before installing v1.27.0.
- The separate signing-workflow PR is still draft and NOT merged.
