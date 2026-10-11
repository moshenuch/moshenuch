# Media Player / Equaliser audit — APK v1.19.0, HTML reference v6.342
Date: 11 October 2026.

## Confirmed source findings before changes
- Native `drawMusic` painted four demo tracks even though none were audio
  assets or an Android audio-provider recording. The timer advanced using
  SystemClock rather than playback progress; center Play toggled a boolean,
  producing no audio.
- Media > Equaliser opened a generic "Not implemented" details screen.
- Media Player Options > Equaliser merely displayed the fixed word "Normal"
  but applied no signal processing.
- "Music library", "Shuffle", "Repeat" and related Options returned notices
  without corresponding audio behaviour.

## Remediation in code
- `NativeMusicPlayer` stores a small user-selected `content://` track library
  in JSON SharedPreferences. It uses `MediaPlayer` for actual playback and
  `Equalizer(0,audioSessionId)` only with a real created audio session.
- `MainActivity.requestAudioFiles` opens ACTION_OPEN_DOCUMENT with audio/*
  and persists granted read access when the provider supports it. No broad
  media permission and no background recording is required.
- Nokia center softkey and arrows play/pause/skip actual track items. The
  progress bar and times are read from MediaPlayer current/duration values.
- `musicLibrary` menu supports selecting tracks, adding more files,
  confirming app-local removal, and file details.
- `musicEqualiser` uses device-provided preset names and Nokia radio glyphs,
  and directly calls `usePreset` and `setEnabled` on the active Equalizer.
  Off disables the audio effect. No fake or universal EQ settings offered.
- Moving the app to the background pauses playback; Activity destroy releases
  media and audio-effect resources. Invalid or revoked URIs fail clearly.

## Device QA checklist (not yet performed)
1. Import two audio files via Media > Media player > Options > Add audio files;
   check persistent library, names, and duplicate suppression.
2. Play, pause, seek via skip, reach end-of-track; verify elapsed bar follows
   real playback and does not claim nonexistent duration.
3. On a device with Equalizer, choose the device-provided presets, toggle Off
   and verify audible effect changes. On unsupported devices verify clear
   "Equaliser unavailable" message.
4. Leave app and return; verify audio paused. Revoke one URI grant; opening
   it should display an error rather than continuing a fictional timer.
5. Remove an imported track. Check the original audio file still exists in
   the Android document provider and persistence is updated on relaunch.
6. Compare actual per-screen Nokia menu layout and Option strings against
   the source HTML; current functionality is not full Nokia firmware parity.

## Risk and limitations
This is original native Android music playback and Android-supported DSP
only. Audio compatibility, provider grants and device equaliser availability
are outside the CI compiler's scope. No synthetic audio entries remain.
