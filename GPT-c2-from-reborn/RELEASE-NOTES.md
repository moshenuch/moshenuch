GPT c2 from reborn — APK v1.12.0 / HTML reference v6.342

NATIVE Android Java/Canvas, not a WebView; partial HTML recreation.

This release preserves the separately committed APK v1.11.0 CALL-LOG improvements:
- Call details, Send SMS and Save-to-Contacts actions.
- Confirmed app-local deletion/clear, plus Restore hidden calls.
- Real visible call counts/durations.

New Messaging audit and implementation:
- Removed invented delivery reports marked "Delivered"/"Sent" for demo contacts.
- Removed fictional saved-message rows; Saved items shows empty until implemented.
- Delivery reports now explicitly say carrier delivery outcomes are not tracked.
- Messaging > Options > Message log counts actual app-visible SMS data (up to
  800 provider records, excluding app-local hidden messages), incoming/unread,
  outgoing/outbox/queued, conversations and saved drafts.
- Messaging > Options > Folder details shows scoped counts for selected folder.
- Messaging > Options > Memory status shows app draft/hidden SMS counts,
  loaded phone records, and unknown real storage capacity.
- Messaging > Options > SIM messages no longer claims the SIM is empty: this
  app cannot read or establish SIM-stored message contents.
- Log > Message log links to the same actual SMS-counting view.
- Compose > Options > Preview now displays current recipient, body and length
  and can return to the editor with Back.
- Other unimplemented Messaging/Compose Options are explicitly labelled.

Other limitations remain: source-exact nested Options, phone-delivery results,
SMS system roles, fonts, media/gallery, and per-screen handset visual parity.
HTML reference 6.342 does NOT establish full behaviour parity.
