# Nokia C2-01: My shortcuts native audit

**Date:** 11 October 2026
**Native APK:** v1.38.0
**HTML reference:** v6.343
**Target emulator:** Pixel 7 / Android 13

## Confirmed source gap in v1.37.0

Settings > My shortcuts displayed four headings but each opened an inert
"Shortcut assignment is simulated natively" page, with no capability to
choose or retain a Home key action. The native Home hardcoded Left=Go to,
Right=Names, Centre=Menu and ignored all four navigation-direction keys.

## Functional implementation

- NativeHomeShortcuts is a production-used, pure-Java model with seven stable
  key slots: Left/Right softkey; navigation Up/Down/Left/Right; centre OK.
- Left/Right/centre remain assigned to Go to/Names/Menu by default.
  Navigation directions are initially unassigned, retaining v1.37 behaviour.
- Settings > My shortcuts > Left/Right/Home screen key opens an assignment
  picker. Navigation key opens four directional rows, each assignable.
- Available actions: Menu, Go to, Names, Messaging, Contacts, Gallery,
  Camera, Video recorder, Organiser, Calculator, Alarm clock, Settings,
  Profiles and None.
- The existing native Nokia radio glyphs identify the currently assigned
  action. Softkey text derives from saved assignments.
- The Home key handler now dispatches all seven bindings through a single
  application-local action handler; the Android global Home/Back keys are
  unchanged.
- Changing a slot is rejected if it would remove the last way to reach Menu
  from Home; other bindings remain unchanged.
- Settings > My shortcuts > Options > Restore defaults requires Yes/No
  confirmation; only confirmed Yes resets the seven bindings.
- Preferences use a strictly validated JSON integer array stored under
  `home_shortcuts_v1`, and corrupt/missing values fall back to safe defaults.
- No Android launcher, contacts, SMS or call-log provider is changed.

## Automated build-side audit

GitHub Actions step `Test assignable home-screen shortcut keys and app wiring`
runs:
- NativeHomeShortcutsSmokeTest (production-used Java model)
- audit_home_shortcuts_wiring.py (live Settings, Home handler, SharedPreferences,
  Camera/Video, radio picker, confirm/reset and Menu invariant)
- Audit artifact `GPT-c2-from-reborn-Home-shortcuts-audit-v1.38.0`
- Human-readable Job Summary; any failure stops the APK publication.

Regression test cases include original v1.37 Home defaults, all four
direction-key slots, softkey labels, Camera/Video choice, assigning Menu
to another key before changing centre, persistence after simulated restart,
malformed JSON, invalid actions/slots, and Restore defaults.

## Pixel 7 / Android 13 audit procedure

1. Install the v1.38.0 debug APK and open **Menu > Settings > My shortcuts**.
2. Observe Left selection key, Right selection key, Navigation key,
   Home screen key. Open Left selection key: a radio list of 14 destinations.
3. Select Messaging. Press End to return Home. Left softkey must now read
   Messaging and open the native Messaging screen.
4. Return and assign Right selection key=Gallery. Right softkey must read
   Gallery and open the native Gallery.
5. Navigate into Navigation key: set Up=Contacts, Down=Calculator,
   Left=Alarm clock, Right=Profiles. On Home, each corresponding direction
   must open the selected destination.
6. Try centre OK=Camera while no other key has Menu. The app must reject it
   and leave centre OK=Menu. Assign Menu to Navigation Right, then centre
   OK=Camera. Both keys must work at Home.
7. Assign Navigation Up=Video recorder. Press Up at Home and ensure it
   requests the actual Android VIDEO recorder (not a fake detail page).
   A compatible emulator camera/video handler is needed to finish capture.
8. Check My shortcuts > Options > Restore defaults; choose No. Bindings
   must be unchanged. Repeat, choose Yes; original defaults must return.
9. Restart APK without deleting app data and verify assignments persist.
   Emulator sessions that reset app data cannot establish this device test;
   use the CI JSON roundtrip result as code-level evidence instead.
10. In the debugger, filter logcat with
    `adb logcat -s C2HomeKeys:I '*:S'`; values contain only action IDs.
11. Verify other screens' Nokia softkeys, Back and End remain unchanged.
12. Compare settings/option positioning and bitmap glyphs with HTML v6.343;
    the implementation is functional, not certified pixel-identical.

## Evidence boundaries

The Java model and static wiring tests validate key assignments and
routing in code, not physical emulator key presses, Android intent handlers,
or screenshot-exact Nokia formatting. The video recorder still requires
an emulator equipped with a compatible Android recording activity.
