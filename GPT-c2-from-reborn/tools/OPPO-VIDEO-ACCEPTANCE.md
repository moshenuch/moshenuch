# Oppo hardware acceptance — C2 Reborn v2.02.0

**APK:** https://github.com/moshenuch/moshenuch/releases/download/c2-native-v2.02.0/GPT-c2-from-reborn-HTML-v6.343-APK-v2.02.0.apk

**HTML reference:** v6.343, read-only. **Status:** pending real Oppo testing. No emulator result establishes audible microphone capture.

## Test without a PC

1. Install the exact v2.02.0 APK. If Android reports a signature/install conflict, note the error. Do not uninstall before considering loss of app data.
2. Open Media > Video recorder; allow Camera and Microphone permissions as requested.
3. Record 8–12 seconds while speaking clearly and press Stop once. Record any prolonged Saving state, failure or timeout.
4. Open Gallery > Video clips > newest recording. Check whether it plays, shows an upright picture, has an expected duration and **contains audible speech**.
5. Open the same clip using a different Android video app or Files, if available. Check public Movies/C2 Reborn visibility.
6. With the in-house C2 video player open, press the Android Back button or gesture. Check that C2 returns to Video clips and remains in the foreground.
7. If recording fails, choose the Nokia **Report** control and share its diagnostic text. Do not send personal recordings unless needed and intended.

## Fill in test results

- Oppo model / Android version:
- Confirmed installed C2 APK version:
- Camera and Microphone permissions:
- Record / Stop outcome:
- Saving screen outcome and approximate time:
- Gallery video found / playable:
- Picture upright:
- **Audible recorded speech**:
- Shared MP4 playable in another application:
- Android Back while C2 player open:
- Failure stage / Report diagnostic (if any):

## Already proven in Android 11 emulator

- Video recorded, finalized, independently decoded; in-house Gallery player prepared; Android Back followed Nokia player-close path while C2 remained foreground: [run #38527802727](https://github.com/moshenuch/moshenuch/actions/runs/38527802727).
- Photo Delete > No and Delete > Yes, preservation of existing JPEGs, and foreground after both: [run #38548067223](https://github.com/moshenuch/moshenuch/actions/runs/38548067223).

**Closure condition:** real Oppo evidence establishes video Record/Stop, saved playback with audible speech and correct Back behaviour, or a reproducible device failure is addressed in a new verified downloadable numbered APK. Do not alter MediaRecorder.stop() based solely on earlier emulator hangs.
