# Voice Recorder follow-up audit — APK v1.17.0 / HTML v6.342

## Native issues observed before this change
- Media > Voice recorder (root index 4) opened a generic `itemDetail`
  placeholder instead of a recorder.
- Gallery > Recordings (root index 7) also opened a static folder-empty
  placeholder, even though user-selected media was supported for Images/Videos.
- The app lacked a microphone permission and a real voice recording state,
  saved-file index, playback and delete-confirmation route.

## Implementation
- `NativeVoiceRecorder` encapsulates Android `MediaRecorder` with AAC/MPEG-4
  mono capture into `getFilesDir()/voice_notes`. It reads its own files with a
  predictable `rec_<millis>.m4a` naming scheme, sorts them newest first and
  plays them through an app-owned `MediaPlayer`.
- Added `RECORD_AUDIO` permission, optional microphone feature and an on-demand
  runtime permission request, deliberately separate from the startup phone-data
  permission prompt.
- `C2PhoneView` now has `voiceRecorder` and `voiceRecordings` native routes,
  Canvas status and microphone indicator, centre softkey Record/Stop and Play/
  Stop, and Options for Recordings/Details/Delete/New recording/Memory status.
- The recording session is capped to 300 seconds while the foreground LCD is
  active. `onPause`, Back and End stop/save the current session; deletion is
  confirmed and deletes only the app's own private recording file.
- The saved recordings list is accessible both from Media and Gallery.

## Safety and remaining QA
- Permission denial leaves recording inactive. No app microphone access until
  explicit user request; no background capture and no external audio scraping.
- Audio is **private app data**: clearing/uninstalling the APK may delete it.
  Delete is irreversible after confirmation, and UI labels this clearly.
- MediaRecorder.stop() can fail for extremely short audio; partial files are
  removed and user sees a failure notice.
- Device test: microphone deny/grant, two successful recordings, playback
  completion, record duration cap, exiting via Back and End, app background/
  foreground transitions, physical microphone unavailable, and delete Yes/No.
- Android compilation and release publishing are separate from device QA.
- Exact upstream Voice Recorder geometry, Nokia animations, icons and all
  Options behaviours have not yet been independently screenshot compared.

## Next candidates
1. Working Camera / Video camera through scoped Android capture intents.
2. Audio export/share through a secure content provider with explicit consent.
3. Expand remaining Gallery categories, then pixel-compare against HTML.
