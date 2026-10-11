GPT c2 from reborn — APK v1.17.0 / HTML reference v6.342

Native Android Java/Canvas app, NOT a WebView. Partial source recreation.

Native Voice Recorder audit and implementation:
- Media > Voice recorder now opens a real native Nokia-like recording LCD,
  rather than a placeholder. The centre key toggles Record / Stop and save.
- Android RECORD_AUDIO permission is requested only upon explicit Record
  activation; granting access does not silently start recording.
- Audio capture uses Android MediaRecorder, MPEG-4/AAC mono in private app
  storage. Maximum foreground recording session is five minutes.
- Recordings are saved under the app's private voice_notes directory, without
  access to external storage or the system recording library.
- Options > Recordings opens saved recordings. Gallery > Recordings also links
  to this same list instead of reporting a fake empty folder.
- Center Play/Stop, Options > Play/Stop, Details, New recording and a live
  Memory status are executable from the saved-recordings list.
- Recorded files can be deleted permanently only after explicit Yes/No
  confirmation. The deletion does not touch unrelated device audio.
- Exiting the recording screen via Back/End, moving to another screen,
  or switching apps automatically stops/saves the current recording.
  Playback stops when the app leaves the foreground.
- Aborted or too-short microphone captures report failure and remove the
  invalid partial file. Microphone-denied and no-device conditions are shown
  explicitly. The LCD displays a recording indicator and elapsed time.

Source and safety limitations:
- This is app-private native Android voice recording, not a verified exact
  Nokia C2-01 6.342 firmware Voice Recorder UI/Options recreation.
- Background recording, pause/resume during a clip, renaming, exporting
  and direct file sharing are not yet implemented.
- Android MediaRecorder behaviour and microphone permission flows must be
  device tested; GitHub compilation does not verify sound capture quality.
- Remaining Media placeholders: camera, FM radio, equaliser; nested Options,
  Gallery categories and whole-screen pixel comparison remain outstanding.

Earlier v1.16.0 Gallery sort, v1.14.1 original Nokia fonts, source icon
assets and messaging/call-log/organiser features remain included.
