GPT c2 from reborn — APK v1.20.0 / HTML reference v6.342

Native Android Java/Canvas port — NOT a WebView and NOT full HTML parity.

Media and Gallery reliability pass, building on v1.19.0:
- Fixed active-track deletion leaving a stale index and displaying a different
  track's title while the audio player had been released.
- Added Android audio-focus handling. Music asks for audio focus only when
  playing; pauses on calls/competing audio interruptions and does not silently
  resume. Pause, navigation Back/End and Activity background relinquish focus.
- Real Shuffle toggle: influences Next/forward skip and automatic next track
  when Repeat All is enabled (does not claim to shuffle nonexistent tracks).
- Real Repeat Off / One / All modes: at completion, Off returns to start
  without playing, One restarts the same track and All advances (respecting
  Shuffle). Modes are saved and restored from local preferences.
- Nokia Media Player LCD shows current Shuffle and Repeat values.
- Music > Options > Shuffle and Repeat now perform actual operations instead
  of displaying the option name or a fake success notice.
- Gallery > Music files now opens the same real user-selected audio library as
  Media > Media player > Music library (rather than a static empty placeholder).
- Player position/progress and track names continue to come from real
  MediaPlayer state and user-selected documents, not simulations.
- An explicit redraw callback keeps Play/Pause state correct when a track
  naturally finishes or audio focus is lost.
- Audio/EQ resources are cleaned up if media preparation or playback fails.
- Existing original Nokia fonts/icons, Camera, Voice Recorder, Gallery and
  messaging improvements are carried forward.

LIMITATIONS AND UNVERIFIED:
- Audio focus and repeat/shuffle behaviour require an Android device test.
- Repeat Off currently plays just the selected track and then stops; Shuffle
  affects manual Next and automatic advancement when Repeat All is enabled.
- Does not implement a background media service, notifications, queue editing,
  universal hardware FM radio or pixel-perfect Nokia media screens.
- Source HTML v6.342 is a comparison reference, not a certification of parity.
