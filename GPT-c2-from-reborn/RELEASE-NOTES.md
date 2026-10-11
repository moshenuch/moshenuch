GPT c2 from reborn — APK v1.41.0 / HTML reference v6.343

Native Android Java/Canvas application — NOT a WebView.

v1.41.0 — fix clipped Calculator title and improve long My shortcuts notices

Reported in the Pixel 4 / Android 11 emulator audit of v1.39.0:
- Shortcut assignment success notices and the longer Menu-protection
  rejection notice could appear clipped on the left/right.
- Calculator title rendered only its bottom half beneath the status bar.
- Navigation-key rows that are unassigned correctly select None in the
  picker; pressing OK explicitly retains None. That is intended and
  remains unchanged to avoid silently assigning an action.

Confirmed source-level cause of Calculator title issue:
- drawStatus() paints a black status/title header at y=0..41 and its
  Calculator title at baseline y=37.
- drawCalculator() subsequently painted its display panel beginning
  at y=30, covering most of the title. Loan calculator used a similar
  first-panel position y=35, also overlapping its title.
- The Calculator display now begins at y=48, below the complete Nokia
  header. Its value and operation labels are placed safely below
  the title; Loan calculator's first input box also starts at y=48.

Notice improvements:
- v1.40.0 had corrected left-alignment and made the notice background
  opaque. In v1.41.0 the box also widens from x=35..205 to x=18..222,
  preserving readable horizontal margins on the 240px Nokia LCD.
- NativeNoticeText now measures text via the actual Android Paint
  width, wraps up to two lines, and END-ellipsizes genuinely overlong
  content (including long unbroken words). Both text and notice box
  share the same coordinates, with hard clipping as a final safeguard.
- Key labels such as Assigned: Messaging, Assigned: Video recorder and
  Keep Menu on at least one home key are explicitly tested to retain
  their complete first and final characters whenever two lines fit.
- Neither the actual shortcut assignment model nor the Menu-protection
  rule is changed.

Audit and tests:
- Production-used NativeOverlayLayout now also defines Calculator/Loan
  panel dimensions with an invariant that no panel overlaps the title.
- Pure Java NativeNoticeTextSmokeTest verifies complete short/medium
  notices, wrapped Menu rejection, two-line limit, END ellipses,
  unbreakable long tokens, and empty/null safety.
- NativeOverlayLayoutSmokeTest verifies Calculator/Loan header clearance
  in addition to the existing notice/Phone detail geometry tests.
- The Python production source-wiring audit now validates the real
  Calculator/Loan renderer and width-measured notice drawing.
- GitHub Actions publishes a readable artifact named
  GPT-c2-from-reborn-Overlay-layout-audit-v1.41.0.
  Passing Java tests and Android compilation are not a screenshot test.

User-confirmed working features unchanged:
- v1.39.0 Conversations sender text spacing verified on Pixel 4/Android 11:
  Leah Hart, leading +447700900799, and NOKIA all display correctly in
  standard and Mark mode, with unobstructed indicators.
- v1.38.0 Home Left/Right softkeys open assigned applications.
- Force-stop and relaunch retain shortcut keys, confirmed by logcat.
- Menu protection correctly rejects removal of the final Menu binding.
- All four navigation assignments open the expected destination screens;
  assigned Up=Video recorder launches the real Android video camera.
- v1.35.0 reversible demo seed still offers 12 Contacts / 12 SMS.

Recommended Pixel 4 / Android 11 audit checks:
1. Settings > My shortcuts > Navigation key > unassigned row: None has
   the selection indicator; pressing OK keeps the key unassigned.
2. Assign a Home key to Messaging or Video recorder; the complete
   Assigned: ... notice should be visible with no list-text bleed-through.
3. Attempt to change the only Menu key to Camera: the entire
   Keep Menu on at least one home key warning should fit (two lines),
   with all leading and trailing characters visible.
4. From Home, open Calculator; its full title should be visible above
   the display panel. Also check Loan calculator's title.
5. Continue testing Restore defaults, Mark subset, templates/Saved,
   and Menu/Go to Organise persistence.

Limitations:
- The Android emulator screenshot check of v1.41.0 is outstanding.
- Stable APK signing is not configured; replacing previous ephemeral
  debug-signed APKs may require an uninstall first.
- The HTML v6.343 reference remains a functional/visual target; complete
  pixel parity is not yet established.
