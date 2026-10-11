# Speed dial long-press and Call settings audit — APK v1.23.0
11 October 2026, source reference HTML 6.342.

## Proven defects before this pass
- Native `onTouchEvent` reacted only to ACTION_UP and had no hold timer,
  drag cancellation, multi-touch handling or protection against a second
  digit event after a long press.
- App-local speed-dial assignments existed in slots 2–9, but there was
  no way to trigger them by holding an on-screen number key from Home.
- Settings > Call > Speed dialling was a static entry that displayed
  "Not implemented in native APK". Source ground-up reference stores a
  `speedDial` boolean, initially false, which gates Long2–Long9.
- `startCall` entered a native screen labelled as an active call before
  verifying whether Android accepted ACTION_CALL/ACTION_DIAL.
- When only ACTION_DIAL was available, a native active-call display
  incorrectly implied the actual call had already started.

## Implemented
- Keypad touch state tracks ACTION_DOWN/UP/MOVE/CANCEL and active pointer,
  with a 700ms delayed action for the digits 2–9 only while the Home
  screen is unlocked, without menus or confirmation dialogs.
- A drag off the key, multi-touch interruption, activity pause/destroy
  or event cancellation removes the scheduled callback. A completed
  long press is consumed, preventing a second digit on release.
- `speedDiallingEnabled` is persisted under `speed_dialling_enabled_v1`,
  default Off. Call settings > Speed dialling opens Off/On with firmware
  RADIO_OFF and RADIO_ON Nokia bitmap sprites; Options > Select works.
- Assigned and enabled long-pressed digits pre-fill the native dialling
  screen and wait for an explicit Call press. When disabled/unassigned
  they report the actual reason and do not place a call.
- Android launch failures no longer open a simulated successful call.
  ACTION_DIAL fallback reports that the Android dialler is open, and
  that a call was not automatically placed.
- Existing 2–9 slot persistence and app-local ownership are unchanged.

## HTML source comparison scope
A readable ~158 KB `ground-up/screen.js` reference from the upstream repo
defines Long2–Long9 on idle and checks `phoneState.speedDial`; when assigned
and enabled it opens an offline dial action. This informed the safe
pre-filled-number behaviour. The upstream `index.html` on main is
3.7 MB; it could not be thoroughly compared within this specific pass.
These are not claims of byte-for-byte or pixel-level parity with the
large main HTML.

## QA requiring physical Android device
1. With Speed dialling Off, hold digit 2 for >=700ms, verify warning
   and no unexpected call or entered digit.
2. Turn On via Settings > Call > Speed dialling; check Off/On source
   radio bitmap, persisted state across restart and selected value.
3. Assign slot 2 through Contacts > Speed dials. Short-tap 2 from
   Home; verify the normal native dial screen contains a single "2".
4. Hold slot 2; verify pre-filled real number and explicit Call step,
   then release finger; no additional "2" should be entered.
5. Test finger drag away, Android touch cancellation, multi-touch,
   keypad lock and screen change mid-gesture. No delayed call action.
6. Exercise unassigned slot 3; verify no calling and proper notice.
7. Deny CALL_PHONE and explicitly Call after a speed-dial prefill:
   ACTION_DIAL opens the external Android dialler without claiming a call.
8. With CALL_PHONE allowed, confirm Android telephony launch is not
   mistaken for a verified connected call in the audit.
9. Compare rendered Nokia fonts, icons and key geometry with source
   reference screenshots. Still not pixel verified.

GitHub build can verify Java compilation/package; cannot verify physical
touch events, telephony intent results or Nokia display parity.
