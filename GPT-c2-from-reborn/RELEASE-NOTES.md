GPT c2 from reborn — native APK source v1.67.0 / HTML reference v6.343

v1.67.0 — Android 11 install/launch verification in APK CI

- Before publishing, the GitHub Actions build now boots an Android 11
  (API 30) x86_64 emulator using the documented Android emulator runner.
- Installs the exact Gradle-produced APK with adb; checks that the package
  exists, starts the native MainActivity and confirms the app process survives.
- Captures launch output, installation output, a PNG screenshot and Android
  logcat in an uploadable CI audit artifact.
- APK publication is blocked if this emulator install/launch smoke step fails,
  in addition to the existing ZIP/manifest/DEX/signature/version/zipalign
  checks and release-download byte comparison.
- The CI emulator launch verifies installation and basic startup, not Camera
  viewfinder operation, video recording, audio, photo orientation, Save/Send,
  external Android Gallery visibility or the third-party upload interface.
- GitHub workflow run result and downloadable APK must still be checked.
  Do not infer a successful build or live release from source commits alone.

---

GPT c2 from reborn — native APK source v1.66.0 / HTML reference v6.343

v1.66.0 — Independent v1.63 camera-audit follow-up

Confirmed by the user's v1.63.0 Android emulator audit:
- Embedded camera Options display is properly layered over the viewfinder.
- Captured Photo shows Options/Save/Back; Save/Delete/Send options appear,
  and Delete shows a Yes/No confirmation.
- Camera writes captured photos to /sdcard/Pictures/C2 Reborn.

Corrections in native source:
- Camera1 still-photo capture now sets JPEG rotation on Camera.Parameters
  separately from SurfaceView display orientation.
- Photo result preview decodes EXIF orientation and rotates/mirrors the
  sampled Bitmap before drawing (Android 7.0+). Android 6.0 lacks the
  platform InputStream ExifInterface; still-photo hardware rotation applies.
- The grey rounded Confirm box is replaced with a dark rectangular Nokia
  Canvas prompt. Confirmation continues to use C2's Yes/No softkeys; no
  Android AlertDialog is involved.
- Save acknowledges a previously committed photo only after confirming it
  is still readable. Send checks readability before presenting Android's
  external sharing chooser. Capture itself continues writing the file to
  MediaStore; Save does not move or rewrite it.
- A read-only APK packaging test verifies ZIP/DEX/manifest, versionName,
  debug signing, zip alignment, file size, SHA-256 before GitHub publishes.
  Publishing then downloads the release APK and compares its bytes against
  the built output. There is no re-compression step in the project workflow.
- Expand CI static checks for orientation, Nokia confirmation, and Save/Send.

Unverified / external constraints:
- The user observed that v1.62.0 and v1.64.0 were rejected by their
  emulator upload interface, whereas v1.59.0 and v1.63.0 uploaded.
  Binary asset content and the failing upload response were unavailable
  for analysis, so their exact cause cannot yet be established.
- v1.63.0 Save-to-Pictures persistence after session loss, actual Send
  operation and C2 Gallery reopening remain to be verified.
- Native v1.64+ video recording on a device remains unverified until an
  installable APK is independently tested.
- The current source version does not by itself establish that a release APK
  built, uploaded, downloaded, or installed successfully.

---

GPT c2 from reborn — native APK source v1.65.0 / HTML reference v6.343

v1.65.0 — Captured-photo Gallery consistency and camera retry reliability

- Deleting a captured photo removes its exact URI from the persistent C2
  Gallery index after the Android media provider confirms deletion, preventing
  a stale/broken entry on the next launch. No unrelated media is removed.
- Photo result Save now reports whether a photo was actually published to
  Android Pictures or retained privately following a shared-storage failure.
- After failed photo/video capture, the live camera preview can restart
  on the same screen. Restarts are deferred and prevented while the activity
  is paused to avoid fighting SurfaceView teardown.
- Add build-side static regression checks for native camera, Gallery deletion,
  shared/private media fallback, and recording finalisation.
- Camera/video encoding, real audio, device permissions, camera rotation and
  Gallery playback must still be tested on an actual Android device.
- Source version alone does not verify a built or downloadable APK.

---

GPT c2 from reborn — APK source v1.64.0 / HTML reference v6.343

v1.64.0 — Embedded video recording (source implementation, device audit pending)

- Video recorder now routes Record/Stop into an in-app MediaRecorder using the
  live legacy Camera preview, rather than launching a stock camera application.
- Request microphone permission on first video recording attempt. The user
  retries Record after granting permission so the preview can resume safely.
- Record to a temporary private MPEG-4 file, validate/finalise it on Stop,
  Back, End, backgrounding or preview surface loss, then publish the MP4 to
  Android Movies/C2 Reborn via MediaStore on Android 10+.
- Retain the app-private video if Android shared Movies publication fails.
  Never claim a corrupt or too-short video was successfully saved.
- Show Record/Stop on the Nokia centre softkey and navigate to C2 Gallery
  after successful recording.
- Gate builds with tools/audit_native_video_wiring.py. This checks source
  integration only; it cannot prove camera/audio encoding or gallery playback.
- Android 6-9 shared video publication, actual device compatibility, audio,
  capture orientation, play-back and recording under phone-call interruption
  remain to be tested or implemented.
- Debug APK is built and published by the GitHub Actions workflow after push;
  do not infer a working/downloadable APK solely from this source version.
- Native Android Java/Canvas application; no WebView.

---

GPT c2 from reborn — APK v1.42.0 / HTML reference v6.343

v1.42.0 — Contacts-first reliability

- Device Contacts creation now uses one Android ContactsProvider applyBatch transaction for raw contact, structured name and mobile number. The name and number refer to the newly inserted raw contact via back-reference, rather than three independent inserts.
- Reject empty or whitespace-only contact names and phone numbers before touching Android contacts.
- An exception during the batch returns failure instead of leaving a successfully inserted raw contact without the remaining rows (provider transactional semantics).
- Contacts, SMS and Phone are now the priority for subsequent functional parity and edge-case checks. Non-core UI polish is deferred.
- Android/emulator acceptance testing remains necessary; versioning and source changes do not themselves establish on-device correctness.

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
