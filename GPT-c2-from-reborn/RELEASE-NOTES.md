GPT c2 from reborn — APK v1.16.0 / HTML reference v6.342

Native Android Java/Canvas. It is not a WebView and does not yet fully match the HTML.

Gallery follow-up audit implementation:
- Gallery > Images or Video clips > Options > Sort is now executable.
  Nokia-style radio icons choose Name A-Z, Name Z-A, or Newest added.
- Gallery root Sort works for selected Images/Video clips categories; other
  categories prompt to open a supported collection rather than acting falsely.
- Gallery ordering persists between launches in the existing app-data JSON
  (legacy records safely read with an optional addedAt timestamp).
- Newly selected media use the folder's saved sort mode.
- Gallery file Details now shows the actual selected name, MIME type and
  file size when Android's document provider permits reading that metadata.
  Unavailable metadata is explicitly shown as Unknown, not fabricated.
- Memory status continues to show app selection counts, never made-up device
  storage capacities.
- Nokia photo preview now browses previous/next photos with Left/Right keys.
  Back returns to the highlighted picture after browsing, and the bitmap
  is recycled upon exit.
- Importing additional pictures from within Preview returns to the Images list
  rather than leaving the user stranded on the previous photo.
- Sort > Options > Select executes the same action as the center Select key.

Retains v1.15.0 scoped Gallery image/video picker, original-content-safe
remove/clear, original Nokia icons and font assets, and earlier SMS/Contacts fixes.

LIMITATIONS: Sorting and Gallery browsing are native enhancements, not proof of
exact original Nokia Gallery menu parity. This is still an app-local selection
library; Camera, Radio, Voice recorder, full folder creation, Media settings,
and full HTML matching remain outstanding. No automated emulator or physical
handset screenshot comparisons were performed in this release.
