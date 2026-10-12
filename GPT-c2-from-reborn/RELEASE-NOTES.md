GPT c2 from reborn — native APK source v1.79.0 / HTML reference v6.343

v1.79.0 — Bound Android emulator dependency installation

- The v1.78.0 production APK compiled, passed package verification and
  was published with a byte-identical download. Its post-release Gallery
  saturation test could not start because the Ubuntu runner remained in
  an unbounded apt-get update/install step for FFmpeg.
- Limit CI FFmpeg preparation to six minutes; allow at most 90 seconds
  to update package indexes and 120 seconds for installation. Check for
  already-installed ffprobe and ffmpeg before contacting package mirrors.
- Missing ffprobe/ffmpeg remains explicitly logged as a warning and
  causes the *optional* independent decoding probes to fail honestly;
  it does not skip the separate 153-index-entry + real photo capture
  capacity test (which does not require FFmpeg).
- Do not block release of an already compiled, signed, byte-verified
  native APK for an unavailable dependency mirror.
- Cap the entire CI job at 25 minutes, and cancel an older superseded
  run when a new commit is pushed. This avoids the v1.78.0 stuck
  dependency job monopolising the public-release queue indefinitely.
  Only one APK workflow occupies that concurrency group at a time.
- Preserve v1.78.0 capture-index over-capacity normalisation, Nokia
  Photo result navigation, share-receiver validation, and all camera
  source code without modification.
- The 153-entry test uses synthetic pre-existing index rows plus one
  real Android emulator JPEG; it does not certify 153 actual captures.
- Appetize/Oppo video Stop and external Oppo Gallery/Send remain
  pending independent device verification.

---

GPT c2 from reborn — native APK source v1.78.0 / HTML reference v6.343

v1.78.0 — Camera Gallery capacity recovery and Android emulator saturation audit

- v1.77.0 Android 11 CI had successfully verified native camera MP4/JPEG
  publication and decoding, and independently delivered Photo > Send to
  the separate C2 Share Probe receiver, which opened the JPEG bytes.
- Correct Gallery capture insertion if saved preferences contain more
  than the 150-row index limit (e.g., older, imported, or interrupted
  state). Repeatedly discard the oldest index rows until a new capture
  fits; persist the final index once after insertion. Do NOT delete
  photos or videos from device storage while pruning the index.
- Add a non-sensitive C2Gallery CAPTURE_RESULT marker indicating that
  Nokia's photo result actually opened, including index success and
  current image count. No media bytes or URIs are logged.
- New post-release emulator capacity audit uses 153 SYNTHETIC seeded
  content-URI Gallery entries, then captures one REAL JPEG using the
  native camera. It asserts exactly 150 final index entries, oldest
  four seeded rows evicted, all 149 newer rows retained, the new
  media:// JPEG URI indexed, a newly published shared JPEG in Pictures,
  and the Nokia Photo result page shown after capture.
- The fixture modifies only disposable CI emulator shared preferences.
  It does NOT claim to have performed 153 actual camera captures.
- Capture and share smoke tests remain separate; optional emulator
  hardware tests issue warnings and retain logs rather than blocking
  release of an otherwise signed and byte-verified APK.
- Physical Oppo Gallery visibility, real-app sharing and voice-bearing
  video capture remain independent acceptance tests. v1.78.0 is not
  proof that the former Appetize video encoder stall is resolved.

---

GPT c2 from reborn — native APK source v1.77.0 / HTML reference v6.343

v1.77.0 — Full-Gallery capture and independent Android share-recipient audit

- Preserve successful v1.76.0 Android 11 recording/MP4/JPEG/MediaStore
  tests, Camera1 video and photo orientation, and Nokia Video Options.
- Correct a capture bug when C2's private Gallery index reached its
  150-entry limit. A new photo could be saved on Android but fail to
  display its Nokia Photo result screen or appear in C2 Gallery.
- For NEW camera captures, the Gallery now rotates out its oldest
  INDEX entry when full, keeps the newly captured file indexed, and
  never deletes device media merely to make index room. Duplicate
  capture URIs are treated as already indexed.
- Always display Photo result (Options > Save/Delete/Send) after a
  successful photo save, even if Gallery indexing failed. A warning
  is shown rather than losing the user's photo-result screen.
- Late-completing video recordings use the same bounded capture index,
  avoiding the 150-entry failure.
- Add separate CI-only shareprobe Android APK with its own package
  com.moshenuch.c2shareprobe (NOT embedded in the production C2 APK).
  It registers as an image/jpeg ACTION_SEND receiver and attempts to
  open EXTRA_STREAM from C2 using the propagated temporary URI grant.
  It verifies JPEG SOI/EOI markers and logs PASS/FAIL without saving
  or printing photo bytes.
- Extend Android 11 photo smoke: after capture and public image
  verification, open C2 Photo > Options > Send, select the independent
  receiver from the system chooser, and assert recipient file access.
  Retain share-ui.xml and share-receiver-log.txt diagnostics.
- The recipient/emulator smoke remains a nonblocking post-publication
  diagnostic. A chooser layout difference can produce an inconclusive
  failure; results and screenshots must be examined before treating
  it as an actual Android URI-permission failure.
- External Oppo Gallery presentation and Send delivery remain
  unverified on physical hardware. Camera video finalisation still
  needs an independent Oppo/Appetize test.

---

GPT c2 from reborn — native APK source v1.76.0 / HTML reference v6.343

v1.76.0 — Photo Send grants, public Gallery index verification and real JPEG smoke

- Keep the v1.75.0 Camera1 recorder and independent video MP4
  validation untouched; its GitHub Android 11 emulator produces a
  finalised H.264/AMR-NB MP4. Oppo/Appetize recording still requires
  independent confirmation.
- Correct Android Sharesheet photo dispatch: provide the same captured
  JPEG content URI via both Intent.EXTRA_STREAM and ClipData, with
  FLAG_GRANT_READ_URI_PERMISSION on both the send and chooser intents.
  This propagates temporary read access to the receiving application
  even when the JPEG is held by C2's private ContentProvider.
- After writing an Android 10+ JPEG to Pictures/C2 Reborn and clearing
  IS_PENDING, query the resulting MediaStore image row. Confirm the
  image is indexed as nonpending, size >128 bytes and MIME image/jpeg
  before dropping C2's recoverable private photo copy. If the public
  row is missing or unreadable, remove the failed public entry and
  retain the C2-private copy, with the existing warning.
- New post-release Android 11 still-photo smoke navigates native C2
  Home > Menu > Media > Camera, captures a real emulated camera JPEG,
  checks that a NEW file appears under Pictures/C2 Reborn, pulls that
  JPEG, verifies JPEG markers, decodes an actual frame with FFmpeg and
  confirms C2 logged PHOTO_INDEX indexed=true pending=0.
- The new photo and existing video probes retain separate logcat,
  screenshot and results files, and do not block release of an APK
  already built and download-verified when emulator hardware fails.
- CI statically checks both permission-grant propagation and the
  MediaStore row verification; photo smoke shell syntax is checked.
- An app-visible nonpending MediaStore row demonstrates public image
  indexing, not whether every OEM Photos/Gallery app has refreshed.
  Oppo Gallery display and recipient app opening a shared image
  remain independent manual acceptance tests.

No-PC Oppo test:
1. Install exact v1.76.0 APK from the versioned GitHub release.
2. In C2 > Media > Camera capture a photo, then press Save.
3. Open Oppo Gallery/Photos; locate it. If absent, check Files >
   Pictures > C2 Reborn. It should persist and be viewable.
4. Back in C2, on the Photo result choose Options > Send; select a
   compatible application and confirm the JPEG preview/attachment
   loads in that receiving app. Cancel sending if you only want to test.
5. Do not conclude Send works merely because Android Sharesheet opens;
   check that the receiving app can actually read the image.

---

GPT c2 from reborn — native APK source v1.75.0 / HTML reference v6.343

v1.75.0 — Enforce complete, independent emulated MP4/audio/video verification

Observed GitHub v1.74.0 Android 11 emulator test:
- C2 recorded a video and published an MP4 in Movies/C2 Reborn.
- Independent ISO-BMFF parser confirmed a complete 171,063-byte MP4
  with ftyp, moov, free and mdat top-level atoms.
- ffprobe was unavailable on the CI runner, so the v1.74 test skipped
  audio/video streams and actual decode checks. It DID NOT establish
  playable video/audio despite its pass status.

v1.75.0 test corrections:
- Install ffprobe and ffmpeg in post-publication GitHub Android 11 job.
- Independent recorded-MP4 validation now FAILS rather than skips if
  either tool is missing. Verify at least one video AND audio stream,
  positive width/height and duration, decode one video frame and one
  second of audio samples using FFmpeg.
- Ensure the MP4 discovered after Record > Stop is NEW compared with
  the list of videos before recording; pre-existing shared Movies
  files cannot satisfy the acceptance check.
- Emulator smoke test retains video-mp4-validation.txt, the pulled
  recorded MP4, its own result summary, video logcat and screenshot.
- Video recording probe remains a post-publication diagnostic; if it
  fails, the workflow emits a warning and retains evidence instead
  of hiding an independently signed and downloadable APK.
- Do not infer that audio samples contain audible speech on a virtual
  emulator. Oppo/Appetize functional tests remain decisive.

Native app behavior:
- No unverified Camera1 encoder changes are introduced in v1.75.0.
- Maintain the Nokia Recording failed screen with "Stage:", 12-second
  timeout, Retry/Back, exception/worker stack logcat, and all existing
  photo and Video recorder Options behavior.
- Still awaiting a v1.73/v1.74 Oppo/Appetize test that reports whether
  native MediaRecorder.stop returns, throws, or remains blocked.
- Photo Send and external Gallery visibility remain unverified.

---

GPT c2 from reborn — native APK source v1.74.0 / HTML reference v6.343

v1.74.0 — Independent MP4 playback checks and no-PC video failure stage

- The published v1.73.0 APK has two completed GitHub Android 11
  emulator runs which passed in-house Record > Stop, MP4 duration/dimension
  checks, and shared Movies/C2 Reborn publication.
- This evidence does not establish that Appetize or an Oppo can finalise
  video; an independent v1.70.0 Appetize audit found stalled Stop and a
  partial MP4 missing its moov atom.
- Post-release emulator smoke now pulls the saved MP4 from shared storage,
  checks its ISO BMFF top-level ftyp, mdat, and moov atoms, and uses ffprobe
  (when available) to require video AND audio tracks plus duration.
  Where ffmpeg is available it decodes a video frame and audio sample.
  Emulator audio may be silent, so this is track/decodability evidence,
  not proof of a working physical microphone.
- Add script syntax checks for the video probe and MP4 validator before
  building so test-harness mistakes are identified early.
- Capture the last failed video-save stage in MainActivity and display
  it directly on the in-house Nokia Recording failed screen as
  'Stage: recorder.stop', 'Stage: private-copy', etc. This allows
  first-pass evidence to be collected using only an Oppo screenshot.
- Preserve the v1.73 full Java exception stack, 12-second Stop worker
  stack trace, Nokia Retry/Back recovery, and working photo handling.
- User/device acceptance still requires a full video clip that saves to
  Movies/C2 Reborn, appears in Gallery, plays with moving picture,
  and contains audible spoken words. Send and external photo Gallery
  discovery remain unverified.

Oppo first-pass acceptance (no PC):
1. Install the exact v1.74.0 APK from its versioned GitHub release.
2. Open C2 > Media > Video camera, allow camera and microphone.
3. Record ~10 seconds while speaking; press Stop once.
4. Check that saving finishes and Nokia buttons respond within 15 seconds.
5. If 'Recording failed' appears, photograph the full screen,
   including 'Stage:' and any 'Encoder stop error:' text.
6. Check Gallery, then Files > Internal storage > Movies > C2 Reborn;
   play the MP4 and check picture movement plus audible words.

---

GPT c2 from reborn — native APK source v1.73.0 / HTML reference v6.343

v1.73.0 — Explicit MediaRecorder.stop exception type on Nokia failure screen

Oppo first-pass acceptance test (no PC / adb required):
1. On the Oppo, download exactly GPT-c2-from-reborn-HTML-v6.343-APK-v1.73.0.apk
   from https://github.com/moshenuch/moshenuch/releases/download/c2-native-v1.73.0/GPT-c2-from-reborn-HTML-v6.343-APK-v1.73.0.apk
   and install/update C2; allow browser/Files "Install unknown apps" if asked.
2. Open C2 > Media > Video camera. Grant Camera/Microphone permission.
3. Record for ~10 seconds while speaking a few words, then press Stop once.
4. Allow up to 15 seconds. Check if Saving clears and Nokia keys still work;
   if Recording failed appears, capture the exact on-screen error text.
5. Check Oppo Photos/Gallery for the new clip. If absent, open Files >
   Internal storage > Movies > C2 Reborn and look for the MP4 there.
6. Play the clip and verify visible motion and audible spoken words.
Report: screen after Stop, whether controls responded, MP4 present in
Movies/Gallery, whether it plays with picture/audio, and any error screenshot.
This is a user-facing smoke test, not a substitute for logcat debugging.


- Preserve v1.72.0 full VIDEO_STOP_THROW stack trace, 12-second
  VIDEO_STOP_WORKER_STACK timeout diagnostics and in-house Retry/Back screen.
- If MediaRecorder.stop() throws RuntimeException, remember its actual
  exception class and display "Encoder stop error: <exception type>" in
  the Nokia Video failure screen. The complete Java exception, including
  stack and message, is emitted to Android logcat with tag C2Camera.
- Distinguish an immediate stop() exception from a blocked native stop()
  that never returns (VIDEO_STOP_TIMEOUT and worker stack).
- Do not share or claim a corrupt/missing-moov temporary file as playable.
- v1.73.0 requires a fresh Android APK build and independent Appetize/Oppo
  reproduction; do not treat source changes as a proven media fix.
- This version is separate from already published v1.72.0 binary.

---

GPT c2 from reborn — native APK source v1.72.0 / HTML reference v6.343

v1.72.0 — Record/Stop exception tracing and Nokia failure/retry screen

Independent Appetize v1.70.0 audit:
- Video-specific Options works and Stop no longer causes UI-thread ANR.
- On Appetize, Stop hangs indefinitely in Saving video; a ~27 KB temporary
  MP4 lacks its moov atom and Movies remains empty.
- Previous Camera1 preview-race change did not fix Appetize recording.
- Photo Send and Android Gallery photo visibility remain unverified.

Root-cause evidence:
- A separate GitHub Android 11 emulated-camera test for v1.70.0 DID
  complete Stop, metadata validation, and public Movies MP4 publication.
  This proves video can complete in that emulator, but not on Appetize.
- Appetize's actual video Stop stack trace has NOT been captured.
  A missing moov atom alone does not prove whether stop() throws or blocks.

Changes:
- Log VIDEO_STOP_BEGIN; record a full throwable stack as VIDEO_STOP_THROW
  if MediaRecorder.stop() throws.
- At the 12-second deadline, log VIDEO_STOP_TIMEOUT, the current stage
  and VIDEO_STOP_WORKER_STACK (up to 30 Java frames from the encoder thread).
- Log VIDEO_STOP_UNCAUGHT if a worker exception escapes its normal handler.
- Surface failed/blocked capture as an in-house dark Nokia Video recorder
  failure screen with Retry and Back softkeys, not an Android AlertDialog.
- Retry reopens native Video recorder only after the previous stop worker
  has returned. If still blocked, inform the user C2 must be restarted
  to retry; never reuse a camera owned by an outstanding MediaRecorder.
- Error handling no longer returns to a stale live camera viewfinder.
- Keep v1.71's preview configuration and already verified still photo and
  video Options behaviour unchanged.
- Add CI source assertions for Stop exception/stack and Nokia error UI.

Request for independent test:
- Run v1.72.0 in Appetize, record ~10 seconds and press Stop. Inspect
  C2Camera logcat for VIDEO_STOP_BEGIN, VIDEO_STOP_THROW, VIDEO_STOP_TIMEOUT
  and VIDEO_STOP_WORKER_STACK; capture the full stack, including cause.
- If Appetize remains different from GitHub emulated camera, install on
  an Oppo Android device, enable CAMERA/RECORD_AUDIO permissions, record
  10 seconds, press Stop, and capture an adb logcat of tag C2Camera.
- Verify whether the MP4 is finalised and visible under Movies/C2 Reborn.
- Do NOT mark video recording fixed without playable MP4 and sound on-device.

---

GPT c2 from reborn — native APK source v1.71.0 / HTML reference v6.343

v1.71.0 — Appetize v1.69 video-finalisation blocker

Independent v1.69.0 audit reported:
- Native video Options menu correct; pressing Stop no longer ANRs.
- After Stop, "Saving video..." never finished and Nokia controls locked up.
- Only a ~27 KB incomplete cache MP4 existed, without a moov atom.
  No playable MP4 was published to /sdcard/Movies.
- Photo Send and external Gallery visibility remain unverified.
- Prior APK "Upload failed" rejections were emulator-account-specific,
  not established packaging problems.

Native fixes in this release:
- Camera1 recorder preview size is now set to the selected hardware
  CamcorderProfile video dimensions when that exact preview size is
  supported, before camera.unlock()/MediaRecorder.prepare().
- MediaRecorder no longer replaces the camera's already configured preview
  surface; doing so can interrupt camera frames feeding the encoder.
- MediaRecorder.stop() continues on a background finaliser, never the UI.
  If it stalls beyond 12 seconds, C2 exits "Saving..." back to native Media
  menu and its physical/on-screen keys become responsive again.
- The stalled worker retains camera ownership and blocks new recordings;
  it is not unsafely terminated. The user may need to force-close/restart
  C2 if the underlying native camera encoder remains stuck.
- A late successful video is indexed in Gallery without forcing navigation
  away from the user's current screen.
- Camera preview SurfaceView is hidden on timeout so it cannot overlay
  native Nokia menus.
- Regression CI checks verify the stop timeout, race guards, matched
  video preview size, and exclusive camera surface.
- Fix the Android emulator CI runner invocation to use one shell-script
  entrypoint rather than a multiline if/fi script that was split per line.

Still pending independent verification:
- Native video Stop returns and produces complete MP4 with video/audio.
- /sdcard/Movies/C2 Reborn persistence and external Gallery discovery.
- Photo Send, Gallery visibility, real camera/audio hardware compatibility.
- CI build/release status must be established separately from these source
  commits. Android 11 emulator recordings may differ from Appetize.

---

GPT c2 from reborn — native APK source v1.70.0 / HTML reference v6.343

v1.70.0 — Video finalisation race and automated Record/Stop diagnostics

- The confirmed v1.69.0 GitHub Actions pipeline successfully built a native
  APK, verified both release downloads, installed it on Android 11 and launched
  the in-house Activity. This did NOT exercise video recording.
- Fix a camera SurfaceView lifecycle race: when MediaRecorder.stop() runs on
  the video finalisation worker, the SurfaceHolder.surfaceChanged callback can
  no longer restart camera preview while the recorder is still stopping.
  This avoids interrupting encoder shutdown on surface resize/recreation.
- Add a source regression assertion for the !recording && !stopping guard.
- Add post-publication Android 11 emulated-camera smoke diagnostics that
  navigate Home > Menu > Media > Video camera, tap Record, record ~8 seconds,
  tap Stop and check for an MP4 in Movies/C2 Reborn.
- Record video-scope logcat, Nokia emulator screenshot and an independent
  video-results.txt as GitHub Actions artifacts. The video probe emits a CI
  warning if it fails; it does not prevent release of a compile/signature-
  verified, downloadable APK, since emulated camera hardware may differ
  from real camera hardware.
- Maintain separate Android 11 install/launch smoke as a required CI step.
- The source still uses native Java/Canvas and Android Camera1/MediaRecorder;
  the video camera is not a WebView or external capture Activity.
- The independent Appetize acceptance test remains decisive for runtime
  camera/MP4 playback and sound. Do not claim the v1.70.0 recording
  functionality passed until its probe and Appetize results have been read.

---

GPT c2 from reborn — native APK source v1.69.0 / HTML reference v6.343

v1.69.0 — MP4 integrity checks and slow-save diagnostics

- Keep the v1.68.0 worker-thread video Stop/Save implementation and the
  dedicated Nokia Video recorder Options menu.
- After the recorder finalises but before publishing to shared Movies storage,
  inspect the saved private MP4 using Android MediaMetadataRetriever.
  Require readable duration (at least 500 ms) and positive video dimensions.
  An invalid/unplayable output is removed from the private media library and
  reported as a failed recording rather than appearing in C2 Gallery.
- Record MP4 duration, dimensions and audio-track presence in C2Camera logcat.
  No captured media or private URI is written to debug logs.
- If background finalisation lasts more than 15 seconds, report that saving
  is still in progress without freezing C2 or deleting the pending recording.
  Clear the delayed warning on completion or Activity destruction.
- Update CI source checks to enforce video validation and the slow-save path.
- APK CI now builds, validates, publishes and downloads/verifies both
  release assets BEFORE running its Android 11 emulator startup check.
  This keeps the APK accessible for Appetize testing if the post-release
  emulator runner fails or times out, while recording that failed smoke
  step and preserving its diagnostic artifact. The workflow can therefore
  report failure even if both release assets were published successfully.

On-device tests still required:
- Video recorder Record ~10 seconds > Stop: check no ANR, confirm playable
  MP4 appears in /sdcard/Movies/C2 Reborn and contains audio.
- Review logcat for C2Camera VIDEO_STOP, VIDEO_VALIDATE and VIDEO_SAVE.
- Retest in-house Video recorder Options and existing v1.67 photo controls.
- Source version does not guarantee build success or a downloadable release.

---

GPT c2 from reborn — native APK source v1.68.0 / HTML reference v6.343

v1.68.0 — Fix reported video Stop ANR and Nokia Video Options

Independent Appetize audit of v1.67.0 confirmed:
- Still photo result upright, matching the in-house viewfinder.
- Nokia-styled dark Question/Delete? confirmation works.
- Delete > Yes removes captured media from shared storage.
- Photos remain in Pictures/C2 Reborn after pressing Save.

Reported blocker in v1.67.0:
- Video recorder > Record works, but pressing Stop makes the app stop
  responding (ANR), and no MP4 appears in Movies/C2 Reborn.

Changes in v1.68.0 source:
- Video Stop now schedules MediaRecorder.stop(), MP4 finalisation,
  private-file completion and Android MediaStore publication on a worker
  thread. The Nokia UI is not blocked by MP4 encoder shutdown or copying.
- Android Camera1 ownership remains on the main thread. Its release waits
  until MediaRecorder is stopped and released; surface destruction and
  Activity.onPause cannot synchronously stop the recorder or race the worker.
- While saving, the softbar displays Saving... and repeated Stop, Back,
  End and Record actions are blocked. Failed finalisation reports an error.
- Successful recordings publish to Movies/C2 Reborn on Android 10+ and
  are added to C2 Gallery; if publishing fails, the private copy is kept.
- Video recorder Options is now Record / Switch to camera / Video clips /
  Help. Camera keeps separate Capture / Switch mode / Help.
- Add structured non-sensitive C2Camera logcat markers VIDEO_STOP and
  VIDEO_SAVE (outcome and elapsed time, never media bytes or URI).
- CI camera and video wiring audits now assert asynchronous finalisation
  and video-specific Options rather than the former synchronous Stop path.

Outstanding:
- Whether v1.68.0 eliminates the ANR, writes an MP4 to Movies/C2 Reborn,
  records audio and plays video MUST be tested on Appetize/handset.
- Android 6-9 shared Movies publishing is not implemented; such files are
  retained privately. Gallery visibility and photo Send remain unverified.
- Source commits do not prove Gradle build, release publication, or APK
  download status.

---

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
