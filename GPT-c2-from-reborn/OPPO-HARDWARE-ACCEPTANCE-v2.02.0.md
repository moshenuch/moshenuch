# C2 Reborn v2.02.0 — Oppo hardware acceptance

**Baseline APK:** [v2.02.0](https://github.com/moshenuch/moshenuch/releases/tag/c2-native-v2.02.0)  
**HTML reference:** v6.343. **Status:** Real-hardware acceptance pending. This checklist does not modify application behaviour.

## Independently verified on Appetize (Android 11)

- [x] Immersive Nokia appearance on Home, Menu, Gallery, Video clips and player
- [x] Nokia font and in-house Gallery/video controls
- [x] Gallery lists and plays an existing clip
- [x] Pause/Play toggles its status
- [x] Nokia right softkey Back returns to Video clips
- [ ] Actual **Android system Back** — not tested; earlier apparent failures used taps on a painted handset instead of an Android system control
- [ ] Camera photo Save in the latest Appetize pass — not tested (verified in earlier audits)

## Oppo test procedure — no PC required

1. Install the **exact v2.02.0 APK** and note the Oppo model and Android version.
2. Open **Media → Video camera**, record a short 3–5 second clip, press **Stop**.
3. Confirm the Saving screen completes and C2 remains responsive. Note any timeout/error text.
4. Go to **Gallery → Video clips**. Confirm the new clip is listed.
5. Open it and verify **moving picture and audio**, then Pause/Play.
6. Verify the Nokia **right softkey** returns to Video clips.
7. Open it again and use the **real Android system Back button or gesture**. Do not tap the drawn handset's Back key. Record the result as passed, failed, or untestable.
8. Capture a photo, confirm Save feedback, view it upright in Gallery, and verify **Send** shares the correct image.

## Evidence and interpretation

If a check fails, record exact screen text, action sequence, Android version, and whether it reproduces on repeat. With adb available, filter logcat tags `C2Back`, `C2Camera`, and `C2Gallery`. The emulator's known MediaRecorder stop hang is **not** evidence of failure on the Oppo.

**Do not** change Back handlers, video rendering, MediaStore import or camera paths without a confirmed new defect. Distinguish `UNTESTED` from `FAILED`. The v2.02.0 release remains the stable baseline until acceptance evidence changes that assessment.
