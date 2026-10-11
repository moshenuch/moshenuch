GPT c2 from reborn — APK v1.36.0 / HTML reference v6.343

Native Android Java/Canvas APK (NOT WebView).

v1.36.0 — build-side Go to audit and privacy-safe Android logs

Requested by the Pixel 7 / Android 13 emulator audit of APK v1.34.0,
which verified Go to selection, Organise Toggle, Move up/down and
Camera launch but could not verify process-persistent order/visibility,
Restore defaults confirmation, reordered shortcut routes, or Video capture.

- Go to model now contains the EXACT JSON-array serialization/deserialization
  used by C2PhoneView Android SharedPreferences keys goto_order_v1 and
  goto_visible_v1. No preference migration or data loss is intended.
- CI creates a fresh NativeGoToShortcuts instance after simulated "restart"
  and verifies saved hidden entries and order restore identically.
- Go to actions are dispatched through new NativeGoToRouter, which is used by
  the REAL C2PhoneView and Java regression tests, not a separate dummy mapping.
- CI checks all nine stable destinations, including Photo (capture(false))
  and VIDEO RECORDER (capture(true)), before and after hide/rearrange.
- CI tests Restore defaults resets BOTH ordering and enabled flags; a
  source-wiring audit checks the Nokia Yes/No softkey confirmation and
  ensures a rejected confirmation does not invoke the restore handler.
- Build-failing source checks trace Video recorder from selected shortcut
  to NativeGoToRouter, MainActivity.launchCameraCapture(true),
  MediaStore.ACTION_VIDEO_CAPTURE, its unique request code, callback and
  Gallery handoff. They do NOT make Android actually capture video.
- Debuggable APKs emit safe `C2GoToAudit` Android logcat lines for:
  restored saved shortcut IDs at startup, writes to SharedPreferences,
  Toggle, Move up/down, confirmed Restore defaults, shortcut launch,
  Android camera/video intent acceptance or failure, and capture results.
  No contacts, numbers, messages, file paths or media content are logged.
- CI publishes a separate "GPT-c2-from-reborn-GoTo-audit-v1.36.0"
  downloadable workflow artifact and readable workflow Job Summary.
  Any failed Go to model, routing or static wiring check fails the APK build.

The debug fixture feature requested by the v1.33.0 emulator audit was
already delivered in v1.35.0 and remains available:
  Settings > Demo data (debug) > Seed / reset sample data
  12 sample contacts and 12 fictional SMS in 6 conversations, including
  Use detail phone/email/web, read/unread status and multi-number contacts.

Auditor instructions: GOTO-SELFTEST-AUDIT-GUIDE-v1.36.0.md

Limitations:
- Unit tests validate the EXACT serialization/dispatch code used by app.
  They do not constitute persistence testing across genuine Android process
  death or an emulator session that clears data; only a retained-data
  emulator or real phone can validate those.
- Source-wiring checks do not prove the device provides a video recorder
  or that capture returns valid media. Debug logcat can show attempted
  launches and Android callbacks on that specific device.
- Stable APK signing is still pending; a previously installed debug APK
  may require uninstalling before installing v1.36.0.
- Other native UI and pixel-accurate HTML v6.343 parity remain incomplete.

Previous features retained:
v1.35.0 reversible isolated emulator Contacts/SMS seed; v1.34.0 Go to
Organise and native camera/video actions; v1.33.0 4 main-menu modes;
v1.32.0 SMS Use detail; v1.31.0 foreground unread SMS update; v1.29.0
keypad Contacts search.
