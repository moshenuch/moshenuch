GPT c2 from reborn — APK v1.13.0 / HTML reference v6.342

Native Android Java/Canvas recreation; NOT a WebView and still not full HTML parity.

Native organiser Options repair:
- Notes: Delete and Delete all notes now require Yes/No confirmation before
  persistent app-data mutation (previously deleted immediately).
- Notes > Send note now opens SMS Compose with the actual complete selected text
  and a blank recipient; it does NOT auto-send.
- Notes > Use detail shows selected note text and character count.
- Notes > Memory status displays actual number of local notes and text characters
  rather than a fake phone memory capacity.
- To-do Options > Delete requires confirmation and shows an accurate result.
- To-do > Memory status shows actual saved entry count and text characters.
- Calendar > Options > Edit opens the selected note in the native editor; saving
  now UPDATES that item, not inserts a duplicate at the start.
- Calendar > Delete and Delete all notes require confirmation.
- Calendar > Memory status reports local saved data, and Go to today shows the
  device-local date without pretending notes have scheduled dates.
- Week view and Go to date remain explicitly unsupported, rather than
  reporting fake success.
- All changes preserve the existing JSON strings in app preferences.

The preceding v1.12.0 and v1.11.0 fixes remain included: truthful SMS reporting,
live message preview, executable call-log actions with reversible local hiding,
source-exact Nokia choice icons, and structured SMS drafts.

Remaining: handset screen pixel geometry, Nokia fonts, gallery/media controls,
calendar date metadata, and nested Options routes still require work and
real-device verification. The original HTML v6.342 tag is a reference label.
