# Continued Gallery/Media audit — HTML reference v6.342 vs APK v1.16.0

## Static findings addressed
- `Gallery > Options > Sort` was offered but fell through to a no-op notice.
- `Gallery Images / Videos` did not offer Sort, even though the app had persistent
  content lists needing a stable user-controlled ordering.
- Gallery image preview had no Left/Right navigation between pictures.
- Back after navigating several photos would restore the originally opened item,
  not the currently viewed item.
- Gallery Details displayed only global counts, even when an individual
  image/video was selected.
- Importing files while on the Preview page could leave the preview selection stale.

## Implemented
- `NativeGalleryStore.Entry` now includes `addedAt`; old JSON records migrate
  by treating the omitted timestamp as zero.
- `NativeGalleryStore.sort` persists chosen A-Z/Z-A/Newest order and rewrites
  app-local JSON in stable order; import reapplies the selected sort.
- `gallerySort` is an executable native route displaying the existing
  firmware radio assets p0460/p0461, with working center Select and Options Select.
- `showGalleryDetails` queries Android's ContentResolver for actual content
  MIME/size metadata; absent permissions/unknown values do not produce fiction.
- Photo previews can traverse the app-selected images using Left/Right, and
  their Bitmap is disposed of on exit. Back restores the last browsed index.
- Picker completion from photo preview returns to the image list.
- No new broad Android media permissions; the Storage Access Framework is
  still the only source of image and video URIs.

## Explicit non-parity
Gallery Sort here is an **app-local practical implementation**, not a firmware
verified reconstruction of the source HTML's sort controls and order.
Nokia fonts and 56px menu icons are reused, but exact row dimensions and
popups still need screenshot comparison. Unsupported media types stay unsupported.

## Device test checklist
1. In Gallery > Images, select three known files with differently sorted names.
   Apply Name A-Z and Name Z-A; verify order survives APK relaunch.
2. Apply Newest added; import another file; confirm it sorts ahead.
3. Verify the original Android Photos and Files data is unchanged by sorting.
4. Preview first image, navigate Left/Right across wraps, Back and re-open.
5. Open Details of a selected image/video, compare name, file size and MIME
   against the Android document provider where available.
6. Test missing/revoked document grants and invalid MIME/size metadata.
7. After initiating Add from phone within Preview, select one file, return,
   ensure the app displays the proper Gallery list.
8. Exercise both center Select and Options > Select on Sort route.

Build and release must be verified in GitHub Actions. Device testing not run.
