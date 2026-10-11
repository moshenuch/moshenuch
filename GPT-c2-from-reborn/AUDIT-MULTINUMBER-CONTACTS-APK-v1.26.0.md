# Native multi-number contact audit — APK v1.26.0
11 October 2026. HTML reference v6.343 (commit 6fec42d4146cd01bc59c561ceac14e340fc467ed).

## Defect
AndroidBackend.loadContacts() discarded a second Phone data row with the same
preceding CONTACT_ID, and C2PhoneView only held one number per aggregated
contact. v1.25.0 introduced contextual Contact / Save a contact on SMS/call
screens; a number saved as a secondary contact field could therefore be
misidentified as unknown. The prior lastContact dedup also depended on sorted
rows being adjacent.

## Corrections
- Aggregate Android Phone rows using a CONTACT_ID keyed map.
- Retain one visible Names row per contact, and all nonblank phone aliases
  in the DeviceContact.numbers collection for matching.
- Populate a C2PhoneView contact ID -> numbers cache after each provider refresh.
- Match known contacts against primary and additional full-number normalized
  aliases; retain strict matching to avoid coincidental short suffix matches.
- Draw primary plus two secondary numbers on the detail view, with
  overflow indicator when more exist.
- Clear provider aliases and device contact rows if READ_CONTACTS permission
  is revoked while the APK is open.
- Guard detail rendering, center key, Call key and Options when contact is
  no longer available instead of indexing an empty list.

## Verification still required
1. Contact with two numbers: exactly one Names row.
2. Select a received SMS from the secondary number -> Contact action should
   open the saved person, not Save a contact.
3. Two different contacts sharing last eight digits must not conflate.
4. Contacts provider rows out of order and contacts with several raw IDs.
5. Local and +44 formats; 00 international prefix; ambiguous local country.
6. Revoke Contacts permission mid-screen and press Call / Options.
7. Check display of 3+ numbers, right-to-left names, long numbers, font clipping.
8. Confirm direct calls and edits still target primary number.
9. Full HTML screenshot comparison and native touchscreen/emulator regression.

## Limitations
The underlying editor still exposes a single primary phone number, and the
green Call action does not pick secondary numbers individually. This change
improves matching and display, not complete source parity or provider sync.
No device SMS, telephony, or contact provider writes were added by this patch.
CI success is a build result, not evidence that physical phone interactions
passed.
