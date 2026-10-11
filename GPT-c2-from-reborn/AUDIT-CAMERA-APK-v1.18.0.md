# Camera and Video Camera audit — APK v1.18.0 / HTML v6.342 reference
Date: 11 October 2026.

## Source issues
- On APK v1.17.0, both Media > Camera and Video camera opened a generic
  explanatory details screen; no executable Android capture workflow.
- Native Gallery v1.16 provided images/videos via explicit Android file-picker,
  but no way to capture a new photo or video and import it into the APK.

## Implementation
- A non-WebView native `MainActivity.launchCameraCapture(video)` delegates
  camera operation to an installed Android camera via system MediaStore actions.
- A non-exported, per-URI-granted `C2MediaProvider` writes temporary files into
  app cache and serves permanent private `camera_photos` / `camera_videos`.
  The provider validates authority, category, basename and extension; library
  entries are read-only even to a granted external consumer.
- Output uses `EXTRA_OUTPUT` + ClipData and narrow URI read/write grants;
  Camera permission and broad storage permission are not requested.
- `C2MediaProvider.complete()` imports successful output (or content-URI
  fallback, or returned JPEG bitmap thumbnail), with caps of 24 MiB for
  pictures and 150 MiB for videos. Cancel discards cache output.
- `C2PhoneView.onCameraCaptureCompleted()` indexes the resulting URI through
  the existing JSON-backed `NativeGalleryStore`, opens the appropriate
  Nokia Canvas Gallery list, and highlights the captured asset.
- The camera Activity result and pending output location survive Activity
  state save/restore when Android preserves the app's instance state.
- Launch and save failures produce explicit notices; no fake photo/gallery
  data is presented as actual captures.

## Limitations / device test plan
1. On two Android versions, try Camera with an installed capture app. Check
   full-resolution output and fallback thumbnail, gallery preview, restart,
   persistent file access and app-only deletion.
2. Record a short video through Video camera. Check that output is playable
   in Gallery > Video clips and per-URI read grants work in external players.
3. Cancel capture: ensure no new Gallery item and no permanent media file.
4. Test no camera app, unavailable camera hardware, failed capture and
   camera app that ignores EXTRA_OUTPUT.
5. Ensure both photo/video data live only in the app's private directory;
   confirm other apps have no blanket access through ContentProvider.
6. Test low storage and oversized video rejection. URI grants should apply
   only to explicitly chosen capture/playback applications.
7. Test app/background lifecycle and pending capture state restoration.
8. Recompare Nokia HTML Camera and Video Camera menus, frames, icons,
   animations and softkeys—these remain unimplemented; an external camera
   UI is NOT a source-faithful Nokia firmware viewfinder.

A successful GitHub Actions compilation is not evidence of functional
camera-device or full HTML parity.
