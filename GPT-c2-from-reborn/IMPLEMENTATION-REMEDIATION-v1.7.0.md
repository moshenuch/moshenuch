# Audit remediation checkpoint: APK v1.7.0 / HTML v6.342

These fixes were applied to source after review of the deep static audit. This is NOT a claim of completed Nokia or original HTML parity.

- A01 fixed in chooseOption Inbox/MessageRead: use activeMessageAddress() for outbound reply, not inboxName().
- A02 fixed: sentMessageAt() selects the actual SMS ID from Sent list and opens MessageRead.
- A03 clarified in confirmation/result text: SMS Delete is local-hide only; Android SMS remains.
- A04 implemented native Contacts Names Mark/Unmark, Mark all/Unmark all, Delete selected with confirmation.
- A05 clarified: call-log Clear is unsupported and reports that records are unchanged.
- A06 partially addressed: 'SMS submitted; delivery unconfirmed' replaces 'Message sent'; native SMS status callbacks are NOT implemented.
- A15 fixed: installedApkVersion() reads Android PackageManager version for home/about.
- A17 fixed at code level: selected Phone._ID / RAW_CONTACT_ID, atomic ContentProviderOperation batch for phone and name; no mass update across all aggregated contacts.

Remaining: A07 structured drafts; A08 media/gallery; A09 root Contacts operations; A10 Options placeholders; A11 authentic checkbox icons; A12 fonts; A13 nested icons; A14 bubble screenshot geometry; A16 Android SMS system state; A18 synthetic content; A19 popup geometry; A20 settings/apps fidelity.

Validation: Android GitHub Actions build to be checked after source commit. No automated instrumentation of telephony or contact operations has yet run; actual Android device results may differ by manufacturer/provider permissions. Contact deletion is real when WRITE_CONTACTS granted; selected deletes require confirmation.
