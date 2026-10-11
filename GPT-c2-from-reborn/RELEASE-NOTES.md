GPT c2 from reborn — APK v1.19.0 / HTML reference v6.342

Native Java/Canvas Android recreation, NOT a WebView. Still not a complete HTML port.

Media Player and Equaliser audit and implementation:
- Removed synthetic Nokia Tune / Demo track / Acoustic sample / Voice recording
  audio entries and the fake four-minute progress bar that advanced without sound.
- Media > Media player now operates on actual Android audio files explicitly
  selected using the Storage Access Framework (ACTION_OPEN_DOCUMENT, audio/*).
  No broad READ_MEDIA_AUDIO or storage permission is requested.
- The audio library persists selected titles and content URIs across launches,
  using Android's per-document persistent read grants when available.
- Actual audio is played via native Android MediaPlayer. Nokia center Play/Pause
  and Left/Right previous/next switch real tracks and show actual playback
  position/duration. Pauses automatically if the app goes to the background.
- Music Options > Add audio files and Music library are executable; choosing
  a library item plays it. Removing one requires confirmation and only
  removes the app-local library reference, never the original audio file.
- Equaliser controls now use Android's real audiofx.Equalizer attached to the
  selected MediaPlayer audio session. Presets are queried dynamically from the
  device's audio engine, not invented. Nokia source radio icons indicate
  preset selections, and Off disables the equaliser effect.
- If no audio is loaded, the Equaliser shows "Open an audio file first"; if
  audio effects are unsupported, it states "Equaliser unavailable".
- Current track details show the actual user-selected file name and player
  state, not a fictional installed media library.
- Playback/audio resources are released when the Activity is destroyed.

Earlier v1.18 Camera/Video Camera capture, v1.17 Voice Recorder, v1.16
Gallery functions and original Nokia bitmap/font assets remain included.

REMAINING LIMITATIONS:
- Equaliser options and Music Player UI are genuine Android implementations,
  not pixel-exact equivalents of the original Nokia C2-01 HTML simulation.
- Some Android devices/players refuse audio sessions or equaliser effects;
  actions display errors rather than promising unsupported processing.
- No background service/media notification, streaming catalog, true shuffle,
  repeat, or native S40A audio visualiser.
- Live Android playback and equaliser behaviour have not been verified on a
  physical test device; GitHub compilation is a syntax and build check only.
