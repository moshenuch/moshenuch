GPT c2 from reborn — APK v1.29.0 / HTML reference v6.343

Native Android Java/Canvas application (NOT a WebView).

New in v1.29.0 — functional Nokia Contacts Search
- Replaces inert "Search" items in Contacts Options and Names Options with
  an executable native contact search screen.
- Users can start searching directly in Names by pressing a digit key.
- Nokia multi-tap 0-9 text input searches contact names case-insensitively;
  repeated taps cycle a key's characters until the 1050 ms interval expires.
- # switches between abc name search and 123 number search; number search
  checks primary and secondary Android contact numbers.
- UP/DOWN scrolls matching contacts, centre OK opens details and physical/
  on-screen green Call starts the selected contact's primary number.
- Search Options provide Open, Call, Send message, Clear search and Back.
  Send message opens the compose editor without sending anything.
- RSK removes one query character; when query is empty it returns Back.
  If no matching contacts exist, the list displays "No matches".
- All search operations are read-only and use the native Contacts list.

Previous native functionality retained:
- v1.28.0 exact Nokia Contact portrait/phone sprites from HTML v6.343.
- v1.27.0 scrollable contact phone numbers and per-number actions.
- v1.26.0 secondary-number contact aliases.
- v1.24.x SMS read/unread states and red dots, and other implemented native
  SMS, speed dial, Gallery, music and Organiser functions.

Limitations:
- Search implementation uses a native multi-tap input screen; screen geometry
  and options ordering have not been certified pixel-identical to HTML.
- Searches use the loaded contacts snapshot; external edits may need Refresh.
- CI success verifies compilation / APK packaging, not physical-device input.
- Signing still uses ephemeral debug keys until signing PR #2 is configured:
  uninstall the previous APK if installation reports a package conflict.
