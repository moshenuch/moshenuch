GPT c2 from reborn — APK v1.18.0 / HTML reference v6.342

Native Android Java/Canvas application. NOT a WebView or fully verified HTML clone.

Camera / Video Camera audit remediation:
- Media > Camera opens an installed Android camera using the standard native
  ACTION_IMAGE_CAPTURE intent. Media > Video camera uses ACTION_VIDEO_CAPTURE.
- The APK grants each camera app access to only a temporary capture content URI;
  it does NOT request CAMERA permission or broad device storage permissions.
- A non-exported ContentProvider enforces path allowlisting and read-only
  access to permanent app-private media, with per-URI grants for capture.
- After an explicit successful capture, photo/video bytes are copied into
  the app's private media library and indexed automatically in Gallery >
  Images or Gallery > Video clips. The captured entry is highlighted.
- Image capture prefers a full-size EXTRA_OUTPUT photo. Camera apps that
  return only a small bitmap instead may produce thumbnail-resolution
  photos; the result depends on the installed external camera app.
- Video capture requests a 2-minute duration limit, but support depends
  on the external camera. Video files are capped at 150 MiB on import.
- A cancelled capture discards the temporary output and does not add it to
  Gallery. Invalid/oversized files are not retained. Camera failures are
  displayed as explicit notices rather than fake success.
- Private gallery files can be previewed or opened from existing v1.16
  Gallery routes, with app-only removal. They are not uploaded or copied
  into public Photos/MediaStore by the APK.
- Pending capture URI is preserved across Activity state restoration.

Previously released features remain, including native Voice Recorder v1.17.0,
Nokia Sans fonts and bitmap icons, SMS and call log actions.

KNOWN LIMITATIONS:
- Camera capture is handled by an external Android camera activity, not a
  pixel-exact Nokia C2-01 viewfinder or embedded native Camera2 renderer.
- There is no continuous internal video preview or Nokia shutter animation.
- Some Android camera apps may ignore EXTRA_OUTPUT or fail to support the
  private content URI. The APK reports these cases without claiming success.
- Source HTML v6.342 visual parity, camera device testing, manual privacy/
  URI-grant validation, other Media controls and nested menu accuracy remain.
