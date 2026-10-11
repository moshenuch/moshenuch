# Native C2-01 menu parity: view modes and Organise
Date: 11 October 2026. APK v1.33.0. HTML reference v6.343.

## Starting discrepancy
v1.32.0 Main menu was always a 3x3 grid with captions, even though
Main menu Options displayed "Main menu view" and "Organise". Selecting
those options merely showed a notice. The HTML source includes List,
Grid, Grid with labels, Tab, and menu rearrangement.

## Code changes
- Four independent Java/Canvas menu renderers using pinned Nokia main-menu
  bitmap icons (existing native RebornAssets resources).
- The view picker is a Nokia 240x320 list with radio options. Selected view
  is persisted in SharedPreferences and restored on each launch.
- NativeMenuLayout independently models navigation and the visual item
  order, with strict permutation validation and bounds checks.
- Reordering moves the selected icon from its old visual position to the
  selected destination; intermediate items shift, but the selected icon's
  stable underlying destination ID is unchanged.
- Menu > Options > Organise enters a two-step Pick / Place workflow.
  RSK cancels an in-progress move; RSK again leaves organiser.
- Home screen HTML reference label now matches v6.343.
- New pure-Java NativeMenuLayoutSmokeTest runs in GitHub CI.

## Static and Java regression cases
1. Mode labels and fallback match List / Grid / Grid with labels / Tab.
2. Grid UP/DOWN and LEFT/RIGHT wrap as 3x3 rows.
3. List and Tab navigation remain sequential with end wrap.
4. Reorder visual slot 0 -> 4 shifts entries instead of overwriting.
5. After rearranging, moved Contacts still routes to Contacts and shifted
   Messaging still routes to Messaging.
6. Invalid/duplicate/truncated persisted order resets to canonical 0..8.
7. Selecting menu options does not change Android contacts/messages
   or other external phone data.
8. Radio list and menu organisation use actual existing Nokia softkey input.

## Remaining manual real-device/browser comparisons
1. Compare each of the four modes against the v6.343 HTML screenshots.
   Tab geometry is only approximate pending real screen measurements.
2. Verify 9 icon bitmaps, text placement, LCD clipping and scrolling.
3. Organise a changed icon order in List, Grid, and Tab; restart APK and
   verify order and selected view persist.
4. Open each icon after reorder and verify correct destination.
5. Cancel Pick with RSK, then exit Organise; confirm order remains unchanged.
6. Press Android system Back during Organise and after View mode picker.
7. Try a corrupted preference value; app should show canonical layout.
8. Confirm no crashes when tapped on icons and when phone screen rotates
   (manifest currently requests portrait only).

GitHub compilation/tests do not certify physical UI parity.
