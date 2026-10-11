# Audit continuation: real Messaging status — APK 1.12.0

**Reference:** upstream HTML v6.342 and published firmware ICON-CATALOGUE.
**Base:** previous native v1.11.0 call-log implementation, retained without
regressing its recently committed provider and menu actions.

## Fixes confirmed in native source
- Fake "Delivered" example records eliminated from Delivery reports; app does
  not currently track telephony sent/delivered callbacks.
- Fake Saved messages removed. No provider-backed saved-items implementation
  is claimed.
- Messaging/Log "Message log" now computes actual counts from `deviceSms` with
  `isSmsUnread` local-read overlay and `drafts`.
- Memory status and Folder details now compute visible app counts rather than
  making unsupported total-capacity or SIM-content assertions.
- Compose Preview option now opens a live preview and Back returns to Compose.
- Unsupported Messaging/Compose options show "Not implemented" instead of a
  generic option-name toast.
- Synthetic menu descriptions for bulk *system* SMS deletion now say explicitly
  that provider deletion has not been implemented.

## Important data limits
- `backend.loadSms(800)` only reads up to 800 current provider records. Hidden
  SMS IDs are filtered locally, so app-visible totals are *not* whole-device totals.
- The Android `READ_SMS` permission does not imply access to SIM SMS storage.
- Android outgoing Sent records do **not** establish successful carrier delivery.
- Status bar, thread/message icon state mapping and the original Nokia bitmap
  selection icons remain from previous builds.

## Verification
Compilation/publish to be checked via GitHub Actions for APK v1.12.0. No
real-device, screenshot-diff, carrier delivery or system-role test was run.
Remaining Options, media/gallery and font parity should be reviewed next.
