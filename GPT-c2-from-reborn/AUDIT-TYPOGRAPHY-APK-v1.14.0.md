# Font implementation audit — APK v1.14.0 / HTML v6.342 reference
Date: 11 October 2026.

## Evidence and diagnosis
The prior native `C2PhoneView.java` used `android.graphics.Typeface.DEFAULT`
and `DEFAULT_BOLD` in multiple screen components. This conflicted with
source Nokia Sans typography and altered content width, line breaking,
weight and row alignment.

The upstream source project includes
`fonts/2-Nokia-Sans-family-13-fonts.zip` and a documented firmware
S40A non-Latin fallback. The former is downloaded at pinned commit
`a8d1dab8273ff4dbdcb63cbc1ec1cac3354e10b8`.
`tools/extract_nokia_fonts.py` examines TrueType name metadata in the archive,
selects source regular/bold faces and writes assets at build time.
`NokiaFontFaces` caches typefaces, supplies safe Android fallback, and
`C2PhoneView` uses these faces rather than Android defaults.

## Verification requirements
- Inspect CI extraction logs to confirm which two original font faces were selected.
- Confirm Android compilation and generated release contain both source faces.
- Test all main menu, softkeys, Messaging, Contacts, call log and organiser titles
  visually on a 240x320 native screen against HTML screenshots with matching
  data. Font names do not by themselves guarantee exact size/weight metrics.
- Test Hebrew/Yiddish and other non-Latin glyphs: S40A fallback is not
  explicitly implemented in the native renderer. The upstream font's inclusion
  does not establish full international Unicode parity.
- Review distribution/licensing of upstream font bundle before any public
  redistribution outside this project's existing release pipeline.

This is not a claim of pixel-perfect parity or a completed route audit.
