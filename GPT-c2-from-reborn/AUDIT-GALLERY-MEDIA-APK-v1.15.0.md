# Native Gallery/Media audit and implementation — APK v1.15.0
HTML source reference: v6.342. 11 October 2026.

## Confirmed pre-fix defects
- `Gallery > Images`, `Gallery > Video clips` and other folders all opened
  the same synthetic "Folder is empty" `itemDetail` message regardless of
  actual device/user media.
- Memory card displayed invented `Free memory: 1.8 GB / Used: 214 MB`
  values, with no genuine storage query.
- Gallery Options > Type of view, Sort, Add folder, Memory status, etc.
  did not act on media; most commands simply displayed their own labels.
- Media > Camera/Radio/Voice recorder showed placeholder success-like text.

## Implemented native route and safety model
- `NativeGalleryStore` holds up to 150 images and 150 videos selected by
  the user, using persistent JSON `content://` URIs and display names.
- The Android Storage Access Framework (ACTION_OPEN_DOCUMENT) supplies
  explicitly selected pictures/videos without broad gallery permission.
- `MainActivity.onActivityResult` forwards selected URIs to `C2PhoneView`
  and requests persisted read grants where available.
- Image preview is native Canvas, using sampled BitmapFactory decoding rather
  than decoding original multi-megapixel files at full resolution.
- Video files are opened by an Android ACTION_VIEW viewer with a temporary
  read grant; the APK itself remains a native application, never a WebView.
- Image and video collections have Options: Open, Add from phone, Details,
  Remove from gallery, Clear folder and Memory status.
- Remove and Clear are confirmed app-index operations only. Source images,
  videos and the device's MediaStore are never deleted or edited.
- Original Nokia firmware Gallery menu icon mappings are unchanged.
- Unavailable or unsupported original Gallery/Media actions are explicitly
  labeled instead of claiming to work.

## Unverified/known divergences
- Gallery is not a full device album scanner. Requires choosing content in the
  Android document picker, by design to avoid broad permissions.
- Preserved content URIs may be revoked by providers/users; shows an
  unavailable image on opening if this happens.
- No Gallery file rename, create folder, sort, thumbnails, landscape
  transforms, category playback, camera or audio editing.
- No screenshot diff against HTML v6.342 in this pass; source-exact Nokia
  typography and icon assets do not guarantee full screen parity.
- Multi-document picker and external Android video player require device tests.

## Test checklist
1. Gallery > Images, select two photos; verify both appear, open native image,
   back returns to the same list selection and no unnecessary memory growth.
2. Relaunch app: both entries survive; no new media permission prompt.
3. Remove first image > No preserves it; Yes removes only the app reference.
4. Confirm original photos are still visible in Android's Photos app.
5. Gallery > Video clips: choose and open a video. If no player is installed,
   verify honest "No compatible video player" instead of false success.
6. Clear folder with confirmation, then re-add files from Android picker.
7. Test Android 8, 12, 13, 14+ provider variations and revoked URI grants.
8. Compare on-screen list/popup geometry against HTML reference.

## Release condition
GitHub workflow must show success for icon/font extraction, Android build and
release upload. Compilation is not a substitute for live-device QA.
