# v1.25.0 — Native contextual number Options audit
11 October 2026. Upstream source: commit 6fec42d4146cd01bc59c561ceac14e340fc467ed,
HTML v6.343, numberOptions343 addition.

## Proven native defect
HTML v6.343 conditionally inserts Contact / Save a contact for number-bearing
screens, links saved numbers to contact details, pre-fills new-contact editor for
unsaved numbers, and offers Send message in-call. Native v1.24.1 only exposed
Save in dialling and Save to contacts in call lists. Inbox/conversations,
individual SMS and in-call menus had no equivalent functional actions.

Additionally contactNameFor matched only the trailing 6-8 digits,
risking wrong display-name association for unrelated full phone numbers.

## Changes
- selectedNumberForOptions() extracts the actual number for the active page
  using selected provider SMS/call, selected thread/message, active call,
  dialled number, selected speed-dial or group member.
- addContextNumberOptions() injects the appropriate action, but does not
  replace existing Save or Save to contacts controls.
- "Contact" resolves a matching existing contact and opens native detail.
- "Save a contact" pre-fills a new editable contact; pressing Save is still
  required to change app/device contacts.
- Active-call Send message opens the composer without sending.
- matchingContactIndex() uses PhoneNumberUtils E.164 canonicalization, then
  strict full-number fallback, instead of matching only 6-8 trailing digits.
- No forced contacts writes, phone calls or SMS transmissions added.

## Suggested handset tests (not performed)
1. Received SMS from saved UK +44 number with saved local 07 number: Contact.
2. Received SMS from unknown number: Save a contact -> edit Name -> Save.
3. Restart, reopen conversation, ensure known contact's real number matches.
4. Verify two contacts sharing final 8 digits are NOT conflated.
5. Marked conversations: number action targets highlighted thread, not
   arbitrary previously opened or marked thread.
6. Sent-only threads / absent SMS permission / unknown call-log number.
7. Active call menu -> Send message: prefilled recipient, no automatic send.
8. View Android contacts provider before/after opening contact editor;
   no changes until Save and successful provider permission.
9. Device locale changes, IDD 00 prefix, malformed and short SMS sender IDs.
10. Compare all menu ordering, scroll focus and 240x320 Nokia icons with
    live v6.343 HTML; visual equivalence remains unverified.

The observed source v6.343 numberOptions343 is an injected rule layered on
top of earlier core HTML menus. This audit makes only a bounded parity
claim, not a full-menu certification.
