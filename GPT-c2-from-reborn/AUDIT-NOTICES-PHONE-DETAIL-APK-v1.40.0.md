# Native Nokia C2-01 — notice clipping and Phone detail title/body overlap

**Date:** 11 October 2026
**APK:** v1.40.0
**HTML reference:** v6.343
**Reported on:** Pixel 4 emulator, Android 11, while auditing v1.38.0.

## Independent audit observations

The user's emulator found My shortcuts functional and verified that
"Assigned: Messaging" and "Assigned: Gallery" updated Home softkey labels.
However, transient notification text was clipped on the left
("ssigned: Messaging") and blended over list labels behind the toast.
Settings > Phone, an unimplemented detail page, also overlapped its own
Phone title and descriptive body.

## Confirmed native causes

### Notice
`drawNotice` previously drew a rounded rectangle at x=35..205 with
`Color.argb(225,20,25,27)`, leaving the list visible underneath.
It set Paint.Align.CENTER, then passed x=48 to `drawWrappedText`, which
interprets x as the left edge of its text lines. A long line extending
left from this anchor could be visibly cut off by the LCD edge.

### Settings > Phone
`drawStatus` reserves y=0..41 and draws the title baseline at y=37.
`drawItemDetail` used the first body baseline y=48 with a 13px font,
which put body-text ascenders above y=41, intersecting the header title.
The Phone stub also asserted that the setting was available, although
its contents had no executable Phone settings.

## v1.40.0 correction

- The notice now uses an opaque Color.rgb(20,25,27) background at
  x=35..205, y=123..177. Text is left-aligned with x=48, width=144,
  baseline y=145, 15px line spacing and two-line maximum.
- Its text is explicitly clipped to an interior rectangle, ensuring
  no title glyph renders outside the notice box.
- Generic item-detail text begins at baseline y=72, after a separator
  line at y=53; the first letter's top is safely below the y=41 header.
- Details permit 11 lines with 18px spacing and do not intrude on
  the Nokia softkey bar beginning at y=286.
- Settings > Phone now explicitly states that Phone settings are not
  implemented and Android settings have not been changed.
- Shared NativeOverlayLayout constants are consumed by the Java Canvas
  renderer and verified by a standalone pure-Java test suite.

## CI gates and evidence

The build step `Test Nokia notice and detail-page layout` runs:
- NativeOverlayLayoutSmokeTest.java: 240x320 geometry, full-opacity,
  inset text bounds, two notice lines, detail-header clearance,
  detail/softbar clearance and correct position of the separator.
- tools/audit_overlay_layout.py: confirms actual drawNotice is opaque
  and left-aligned, uses clipRect and shared measured coordinates;
  confirms actual drawItemDetail uses divider and safe body baseline;
  confirms Phone stub wording and My shortcuts notice string.

CI uploads a plain-text report:
`GPT-c2-from-reborn-Overlay-layout-audit-v1.40.0`

Passing these checks and Android compilation does not constitute a
visually captured emulator screenshot.

## Pixel 4 / Android 11 manual retest

1. Install v1.40.0. Go to Menu > Settings > My shortcuts.
2. Assign Left selection key = Messaging. Verify the notice begins
   with the entire **Assigned: Messaging** string, including **A**.
3. Observe that the notification has a solid background, without
   underlying settings list text bleeding through.
4. Set Right selection key = Gallery; repeat notice test.
5. Settings > Phone: title and description must be on separate lines
   and visually separated by a horizontal divider.
6. Check Settings > My shortcuts > Help and other item-detail pages
   to ensure the general title/body renderer remains readable.
7. Compare any remaining spacing differences with the HTML reference.
8. Continue outstanding tests for centre-key Menu protection,
   Restore defaults, saved SMS templates and Organise persistence.

## Scope

No changes were made to Contacts/SMS data, key assignment logic,
unread-dot state or menu reordering. This is a focused UI-rendering
and accurate placeholder wording patch only.
