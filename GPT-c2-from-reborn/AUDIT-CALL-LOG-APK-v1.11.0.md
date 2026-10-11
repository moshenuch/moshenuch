# Native call-log Options audit — HTML v6.342 / APK v1.11.0
Date: 11 October 2026. Scope: live Android call-log display/action semantics.

## Verified from native source before fixes
- The original call-list OK action just copied selected phone number to `dial`
  and showed "Ready to call"; the user couldn't view call metadata.
- Call-list Options offered Call/Send message/Save to contacts/Delete/Clear list,
  but only Call performed its advertised operation; Clear list was explicitly
  unsupported; others were placeholder notices.
- Log > Call timers previously displayed ungrounded static text.
- The Android backend reads CallLog.Calls; changing the real Android call log
  generally requires elevated/default-dialer privileges, not present here.

## Code changes (APK 1.11.0)
- `showCallDetails` derives call category, name/number/date/duration directly
  from the selected `AndroidBackend.DeviceCall` record.
- `composeToCall` safely fills native SMS recipient (does not auto-send SMS).
- `saveCallAsContact` pre-fills contact editor; requires explicit Save.
- `callFilterForPage` scopes 'missed', 'received', and 'dialled' lists by type.
- `callRecordKey` persists app-local hidden call records in SharedPreferences.
  A Yes/No confirmation precedes Hide one / Hide list / Hide all.
- `restoreCalls` clears the app-only hidden filter, then refreshes Android
  records. It does NOT reconstruct records physically deleted by another app.
- All hidden call operations clearly state "Android call log unchanged."
- Call-timer values are computed from currently visible provider call records.

## Remaining tests and risks
- On a test phone, verify each action with multiple incoming/outgoing/missed
  records and duplicates; confirm only the highlighted record is used.
- Verify Android provider retains records after app-local hide and that
  restoring repopulates all provider records still available.
- Confirm permissions and prefill editor for private/unknown callers.
- With deviceCall records loaded up to 300, the visible total/counters represent
  only the loaded subset, not lifetime phone history.
- No device screenshot/pixel parity testing performed.
- Call-log providers and permissions differ by device; Android compilation alone
  does not verify call/SMS/contact integrations or source HTML parity.
