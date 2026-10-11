GPT c2 from reborn — APK v1.7.1 / HTML reference v6.342

Native Android Java, not a WebView. Reference HTML v6.342 is a version provenance label, not a full parity guarantee.

Corrections after the deep code audit:
- Inbox Options now binds the highlighted message before Reply/Delete/Call; Sent Options includes a functional Open action.
- SMS Reply now addresses the actual sender number rather than the sender's contact display name.
- Sent items are linked to their real Android SMS IDs and can be opened.
- Contact edits target the exact selected Phone Data._ID and raw contact (atomic provider batch) rather than overwriting all numbers.
- Contact Names Options can Mark/Unmark, Mark all, Unmark all, and delete explicitly selected contacts behind a confirmation.
- App-local SMS/conversation hide operations clearly say Android's SMS provider remains unchanged.
- Call-log Clear options no longer falsely claim that Android records were deleted.
- SMS submission no longer claims successful delivery before Android confirms it.
- About and home version labels are obtained from the installed Android package metadata.
- Existing Nokia bitmap icon and conversation rendering fixes are retained.

Limitations: no full screenshot-by-screenshot/device parity certification. Sent/received status callbacks, structured drafts, true Android SMS deletion/default-SMS role, full icons/options on nested screens, contact groups, and media/gallery features remain on the outstanding audit list. Real-device testing must use demo numbers and avoid destructive testing on personal data.
