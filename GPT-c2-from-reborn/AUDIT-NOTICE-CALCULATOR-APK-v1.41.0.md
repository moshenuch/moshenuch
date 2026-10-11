# Pixel 4 / Android 11 — v1.41.0 notice and Calculator audit

**Native APK:** v1.41.0. **Pinned HTML reference:** v6.343.
**Reported regression:** v1.39.0 emulator screenshot and logcat.

## Confirmed by the user

- The v1.39.0 Conversations spacing correction is physically verified:
  Leah Hart, unsaved +447700900799 and NOKIA show complete leading glyphs,
  while unread dots and Mark checkboxes stay to their right.
- Home-key assignments survive force-stop and relaunch (the restored
  array was [3,2,13,13,13,13,0]).
- The Menu protection check rejects unassigning the final Menu key.
- Left/Right selection actions and all four navigation-key shortcuts work,
  including actual Android Video recorder launch.
- A new finding: Calculator title appears half-obscured; the long
  rejection notice and shortcut assignment notices can clip at the edges.

## Native source diagnosis

The old Calculator Canvas renderer did:
```java
drawStatus(c, "Calculator");
c.drawRect(8, 30, 232, 95, p);
```
The title baseline was y=37 in the black header y=0..41. The
Calculator panel started at y=30, which literally painted over the title
in the next operation. Loan calculator similarly started its first
field rectangle at y=35.

v1.41.0 moves Calculator display top to y=48, bottom to y=106,
value baseline to y=88, and operation baselines to y=130 + 24*i,
well below the title and above the bottom softkey bar.
Loan calculator's first panel now also begins at y=48.

The previous v1.40.0 release changed the notices from centre-aligned
at a left margin to left-aligned and fully opaque. This release hardens
the longer notices further: wider x=18..222 foreground background,
interior text x=30..210, actual Paint-measured word wrapping, a
two-line limit, and end ellipsis for text that cannot fit. Canvas
clipping remains as a final containment safeguard.

## CI evidence

GitHub workflow step:
`Test Nokia notice wrapping, Calculator and detail-page layout`

- Java production-used NativeNoticeText layout tests cover:
  Assigned: Messaging; Assigned: Gallery; Assigned: Video recorder;
  Keep Menu on at least one home key (complete text preserved);
  the narrower former notice width; very long unbroken words; >2-line
  messages and trailing ellipsis; empty/null cases.
- NativeOverlayLayoutSmokeTest verifies notice coordinates, fully
  opaque backing, title-safe Calculator/Loan rectangles and Nokia
  softkey-bar clearance.
- Python audit_overlay_layout.py checks the actual C2PhoneView
  drawNotice/drawCalculator/drawLoan source uses the tested geometry,
  the real Paint glyph measure function and the safe text renderer.
- Full report is uploaded by the CI build under
  `GPT-c2-from-reborn-Overlay-layout-audit-v1.41.0`.

Passing these checks and Gradle compilation is NOT equivalent to
physically seeing the corrected screen on an Android emulator.

## Emulator manual confirmation

1. Open Settings > My shortcuts, assign a selection key to Messaging.
   Verify toast shows the entire **Assigned: Messaging** string.
2. Assign Video recorder; verify full **Assigned: Video recorder** message.
3. With centre OK still the only Menu key, try changing it to Camera.
   The full **Keep Menu on at least one home key** notice should be
   visible and the assignment unchanged.
4. Verify notice backgrounds obscure the list behind them, without
   text bleeding through.
5. Return Home, use Calculator shortcut. The full **Calculator**
   header should be visible; display panel must not obscure letters.
6. Inspect Organiser > Calculator > Loan calculator (if available);
   its title should likewise be unobstructed.
7. The radio picker for an unassigned direction should select **None**;
   pressing OK assigns None explicitly and should not open an app.
8. Continue tests of Restore defaults, Mark subset, templates/Saved,
   and Organise persistence without resetting emulator app data.
