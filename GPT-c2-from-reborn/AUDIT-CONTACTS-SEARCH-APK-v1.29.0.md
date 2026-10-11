# Nokia C2 native Contacts Search audit — APK v1.29.0
Date: 2026-10-11. Reference HTML v6.343.

## Functional gap in APK v1.28.0
Two Options entries for "Search" existed in the native Contacts root and
Names screen, but selecting them only displayed a notice. The ordinary
Names list treated digit keys as numeric row shortcuts, preventing text-based
phonebook search.

## Implemented
- `beginContactSearch` routes to native `contactSearch` page, using phone
  keypad input without a WebView, network connection or third-party search.
- Nokia-style multi-tap abc search (0-9) for case-insensitive contact names;
  `#` toggles numeric 123 mode. Numeric search checks primary and all cached
  alternate Android phone-number rows for an aggregated contact.
- Up/Down moves the highlighted result, centre OK opens Contact details,
  and Call and Send message are executable Options.
- Typing directly from Names enters Search with the typed digit.
- RSK is Backspace on a nonempty query, Back otherwise; Options offers a
  separate Clear search command.
- Empty/no-permission contacts and unmatched queries show a truthful message.
- Search and navigation do not write to Android Contacts, send SMS or place
  a call unless the user explicitly presses Call.

## Static source checks
- Search has a dedicated Canvas renderer and phone-key handler.
- Contacts Options and Names Options point to beginContactSearch.
- Numeric search uses contactNumbersById aliases loaded since APK v1.26.0.
- All result actions use the currently filtered contact index rather than
  an old unfiltered Names index.
- All queries clamp selected result index when filtering changes.
- Release config bumped to versionCode 12900, versionName 1.29.0.

## Physical handset checks still outstanding
1. Contacts root -> Options -> Search; Names -> Options -> Search.
2. Press 6/6/8 in Names and cycle multi-tap to search a name.
3. Wait >1050ms between repeated same-key letters; verify correct text.
4. Use # for number search, including a secondary saved number.
5. UP/DOWN among several matches; centre OK opens exactly highlighted contact.
6. Call and Send message Options act on highlighted result; compose must not
   auto-send.
7. RSK deletes the last character and returns when query is empty.
8. Test contacts permission denied, zero contacts, unusual Unicode names,
   long names, empty numbers, deleted contacts and hundreds of matches.
9. Compare exact pixel layout, Nokia icons, softbar and Options scroll with
   the live HTML reference; this is functional improvement, not parity proof.
10. Confirm the APK signature conflict persists until stable signing is set.

CI compilation success is not a physical-device test.
