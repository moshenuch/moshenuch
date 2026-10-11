# Follow-up Media Player / Gallery audit — APK v1.20.0
HTML reference: v6.342. Date: 11 October 2026.

## Confirmed defects and omissions in v1.19.0
1. Deleting the currently playing/loaded track called releasePlayback()
   but left activeIndex untouched. This could make title()/index() identify
   a different track even though no MediaPlayer instance was loaded.
2. The player did not request/abandon Android audio focus on play/pause,
   so other audio and interruption scenarios were uncontrolled.
3. The new media player had no automatic completion behaviour for repeat
   modes and no executable shuffle/repeat controls.
4. Gallery > Music files still displayed "Not yet implemented in this native
   Gallery" although the user-selected music library had been implemented.
5. MediaPlayer completion or audio interruption could leave the Nokia
   Canvas screen out of date until its next refresh.
6. A playback exception during open() could leave an Equalizer instance
   attached to a released audio session.

## Code changes
- `NativeMusicPlayer.remove` resets activeIndex when removing the active
  entry, releases audio focus/session resources and notifies the UI.
- AudioManager + AudioFocusRequest (API 26+) and legacy focus fallback
  (API 23–25) govern playback. Focus losses/transient loss/duck requests
  pause playback with no surprise automatic restart. Pausing abandons focus.
- Repeat modes Off, One, All persist in music_repeat_v1; Shuffle persists
  in music_shuffle_v1. OnCompletion controls the real MediaPlayer; Off
  leaves it stopped/reset, One seeks to start and restarts, All opens the
  next track (shuffled if selected).
- `C2PhoneView` Music Options now execute Shuffle/Repeat and display the
  state; Media Player exits pause the music rather than leaving untracked
  audio audible from the Home screen.
- Gallery main list Music files routes into existing musicLibrary, so audio
  selected under Media appears in Gallery without synthetic folders/files.
- Added an onChanged redraw callback for asynchronous audio focus and
  MediaPlayer completion changes.
- Audio-setup exceptions now release any partially attached Equalizer.

## QA checklist (device testing not yet performed)
1. Import two known tracks, play both; remove the active track with
   confirmation and verify no new track name appears falsely as playing.
2. Open another music player or receive call during playback; test audio
   focus loss and explicit user-controlled restart.
3. Use Shuffle On and Next repeatedly; use Repeat One on track completion.
4. Use Repeat All and verify next-track advancement and wraparound.
5. Leave with Back/End, background app, and verify playback stops/pauses.
6. Open Gallery > Music files and verify imported files are there. Return
   through the original Music Player Options route.
7. Check Android 6/7 legacy audio-focus fallback and Android 8+ request.
8. Confirm settings persist after restart, real source audio remains intact.
9. Compare Nokia menu font, icons, softkey labels, timings, exact HTML
   UI flows and coordinates on physical screen.

Build success is a compiler/packaging check, not functional playback or
full screenshot parity. No false sound/playback success is claimed.
