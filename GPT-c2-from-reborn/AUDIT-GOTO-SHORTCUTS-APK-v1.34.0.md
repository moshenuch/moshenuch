# Nokia C2-01 — Go to organiser and camera shortcut audit
Date: 11 October 2026. APK v1.34.0; pinned HTML v6.343.

## Verified deficiency in v1.33.0
Go to had a fixed nine-entry shortcut list. Go to > Options > Organise only
displayed a notice and no changes were possible. The Camera and Video recorder
shortcuts displayed inert placeholder detail screens even though real native
camera/video capture was implemented and already used in Media.

## Changes
- New NativeGoToShortcuts maintains a permutation of nine stable shortcut IDs
  plus a separate enabled/hidden state; order and visibility persist.
- Go to > Organise now shows all available shortcut destinations with the
  actual CHECK_ON/CHECK_OFF firmware icons.
- Select/Toggle changes visibility, Move up/Move down reorders, and a
  confirmation dialog protects Restore defaults.
- Disallow hiding the last visible shortcut, keep consistent checked state,
  reject malformed/duplicate/out-of-range preferences, and clamp selected
  visible row after exiting the organiser.
- Route actions using stable visibleId rather than visual array index.
- Camera and Video recorder shortcuts use startCameraCapture(false/true),
  the existing Android native provider path. They no longer fake completion.
- No system launcher or Android contact/SMS database mutations occur.

## Java regression tests
- Nine defaults; default Camera=3 and Video=4.
- Hiding the first entry makes Profiles first without altering Camera route.
- Reordering Camera moves its visual position without changing destination.
- Cannot move outside list or enable invalid IDs.
- Saved order and enabled set restore accurately.
- Invalid, duplicate or truncated saved values reset safely.
- Cannot hide the final shortcut.
- Restore defaults resets both order and visibility.

## Remaining actual-handset checks
1. Go to > Options > Organise opens nine rows with checked firmware icons.
2. Hide Profiles; return to Go to; ensure missing from menu while other
   shortcuts still navigate to their original destinations.
3. Move Camera upward; exit and restart; same visual order/visibility.
4. Tap shortcut Camera and Video recorder; grant camera app permission as
   appropriate; verify capture returns into native Gallery.
5. Cancel capture; verify no fake Gallery entry is created.
6. Hide eight entries then try hiding the ninth; explanatory notice.
7. Restore defaults confirm No vs Yes and persistence after restart.
8. Press Back from organiser after hiding the previously selected Go to row;
   ensure the highlight stays within the shortened list.
9. Compare Go to icon geometry, text, checkbox sprites and scroll behavior
   pixel-by-pixel with live HTML v6.343.
10. Ensure no unexpected contact writes, SMS transmissions or system launcher
    changes occur from organising shortcut tiles.

Passing GitHub Java tests and Gradle compilation is not a physical device
test or proof of complete visual matching.
