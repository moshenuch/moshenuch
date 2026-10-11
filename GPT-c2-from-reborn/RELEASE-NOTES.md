GPT c2 from reborn — APK v1.14.0 / HTML reference v6.342

Native Java/Canvas Android app. Not a WebView. Still a partial port.

Typography audit and changes:
- The APK's Nokia screens were painted entirely with Android's default sans-serif
  fonts, creating visibly different glyph shapes, line wraps and text widths.
- A pinned upstream Nokia Sans family archive is now inspected in CI for its
  embedded TrueType font names. The selected regular/bold faces are placed in
  Android assets during the build (rather than bundled from unverified filenames).
- The native Canvas renderer consistently uses the two source Nokia font faces
  for status bar, lists, menus, selected rows, SMS, Contacts and most other screens.
- The native renderer has Android fallback if a font is missing at runtime.
- Existing Nokia bitmap state icons, SMS fixes, organiser and call-log actions remain.

IMPORTANT: This is a typography-resource implementation pass, NOT calibrated
pixel-level font matching. Original Nokia font stacks and per-screen sizes and
spacing need tests against the HTML and handset. Non-Latin scripts may use
different firmware fallback faces not yet ported.

All other remaining work (nested Options, Gallery/Media, Android SMS roles and
full screenshot comparison) is still outstanding.
