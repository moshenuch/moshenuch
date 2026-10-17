#!/usr/bin/env python3
"""Static regression guard for the C2 native camera and Gallery integration.

These checks validate source integration only, not Android compilation or
camera/media correctness on an emulator or handset.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/com/moshenuch/c2reborn"
activity = (java / "MainActivity.java").read_text(encoding="utf-8")
view = (java / "C2PhoneView.java").read_text(encoding="utf-8")
gallery = (java / "NativeGalleryStore.java").read_text(encoding="utf-8")
provider = (java / "C2MediaProvider.java").read_text(encoding="utf-8")
preview = (java / "C2CameraPreview.java").read_text(encoding="utf-8")

checks = {
    "Still camera specifies JPEG rotation, not only preview rotation": (
        "photoParameters.setRotation(jpegOrientation)" in preview
        and "camera.setDisplayOrientation(sensorOrientation)" in preview
    ),
    "Gallery image viewer applies same EXIF orientation as photo result": (
        "galleryPreviewBitmap = galleryStore.decodePreview(item.uri, 240, 240);" in view
        and "galleryPreviewBitmap = orientCapturedPhotoPreview(" in view
        and "android.net.Uri.parse(item.uri), galleryPreviewBitmap" in view
    ),
    "Photo Save shows Nokia in-house feedback": (
        "capturedPhotoSaveNotice()" in view
        and 'showNotice(capturedPhotoSaveNotice());' in view
    ),
    "Photo result respects EXIF orientation": (
        "orientCapturedPhotoPreview(uri, capturedPhotoPreview)" in view
        and "androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION" in view
        and "Bitmap.createBitmap(" in view
    ),
    "Delete confirmation uses Nokia Canvas rather than stock grey dialog": (
        "private void drawConfirm(Canvas c)" in view
        and "c.drawRect(11, 93, 229, boxBottom, p);" in view
        and 'c.drawText("Confirm"' not in view
    ),
    "Save and Send verify committed photo still readable": (
        "boolean capturedPhotoExists(Uri uri)" in activity
        and "!((MainActivity) getContext()).capturedPhotoExists(capturedPhotoUri)" in view
        and "if (!capturedPhotoExists(uri))" in activity
    ),
    "Shared Pictures are indexed before private photo is removed": (
        "private boolean isSharedPhotoIndexed(Uri uri)" in activity
        and "MediaStore.Images.Media.IS_PENDING" in activity
        and "MediaStore.Images.Media.SIZE" in activity
        and 'if (!isSharedPhotoIndexed(uri))' in activity
        and "if (shared != null) deleteCapturedPhoto(result);" in activity
    ),
    "Share chooser grants attachment read access via ClipData": (
        "android.content.Intent.ACTION_SEND" in activity
        and "android.content.Intent.EXTRA_STREAM" in activity
        and 'android.content.ClipData.newRawUri(' in activity
        and "send.setClipData(clip);" in activity
        and "chooser.setClipData(clip);" in activity
        and 'chooser.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)' in activity
    ),

    "Captured media rotates bounded C2 Gallery without deleting source file": (
        "boolean addCaptured(boolean video, Uri uri)" in gallery
        and "while (list.size() >= 150)" in gallery
        and "list.remove(oldest);" in gallery
        and "return add(video, Collections.singletonList(uri)) > 0;" in gallery
        and "boolean indexed = galleryStore.addCaptured(video, uri);" in view
        and "showCapturedPhoto(uri);" in view
        and "CAPTURE_RESULT page=" in view
        and "if (!indexed) showNotice(" in view
    ),
    "CI saturates real camera capture against 153 seeded Gallery entries": (
        (root / "tools/smoke_gallery_capacity_emulator.sh").exists()
        and "gallery-capacity-results.txt" in
            (root / "tools/smoke_gallery_capacity_emulator.sh").read_text(encoding="utf-8")
        and "gallery-capacity-final.xml" in
            (root / "tools/smoke_gallery_capacity_emulator.sh").read_text(encoding="utf-8")
    ),
    "Late finalized video can be indexed after Gallery reaches capacity": (
        "galleryStore.addCaptured(true, uri)" in view
    ),
    "Independent share receiver opens granted JPEG in separate package": (
        (root / "shareprobe/src/main/java/com/moshenuch/c2shareprobe/ShareReceiverActivity.java").exists()
        and "getContentResolver().openInputStream(stream)" in
            (root / "shareprobe/src/main/java/com/moshenuch/c2shareprobe/ShareReceiverActivity.java").read_text(encoding="utf-8")
        and "smoke_photo_emulator.sh" in
            (root / "tools/run_emulator_smoke_suite.sh").read_text(encoding="utf-8")
    ),
    "Private media delete is implemented": (
        'if (!"image".equals(category) && !"video".equals(category)) return 0;' in provider
        and 'file.isFile() && file.delete() ? 1 : 0' in provider
    ),
    "Photo Delete waits for Nokia confirmation before removing saved media": (
        'requestConfirm("deleteCapturedPhoto", "Delete?")' in view
        and 'if ("deleteCapturedPhoto".equals(action))' in view
        and '((MainActivity) getContext()).deleteCapturedPhoto(capturedPhotoUri)' in view
        and 'galleryStore.removeUri(false, capturedPhotoUri.toString());' in view
        and 'showNotice("Photo deleted");' in view
        and 'showNotice("Unable to delete photo");' in view
    ),
    "Photo delete removes only matching saved Gallery URI": (
        "boolean removeUri(boolean video, String uri)" in gallery
        and "galleryStore.removeUri(false, capturedPhotoUri.toString());" in view
    ),
    "Photo Save identifies private fallback truthfully": (
        "capturedPhotoSaveNotice()" in view
        and '"Photo saved privately"' in view
    ),
    "Successful publishing frees duplicate private photo": (
        "if (shared != null) deleteCapturedPhoto(result);" in activity
    ),
    "Video recorder stop and MediaStore publishing are off UI thread": (
        'Thread worker = new Thread(() ->' in activity
        and 'valid = cameraPreview.stopRecording();' in activity
        and 'Uri shared = publishVideoToMovies(privateCopy);' in activity
        and 'worker.start();' in activity
    ),
    "Camera cannot be released while MediaRecorder is finalising": (
        'if (recording || stopping)' in preview
        and 'cameraPreview.finishVideoCamera();' in activity
        and 'if (cameraPreview != null && !videoStopWorkerRunning) cameraPreview.stop();' in activity
    ),
    "Video UI guards against repeated Stop and uses video-specific Options": (
        'if (videoSaving()) return true;' in view
        and 'if ("videoViewfinder".equals(page) && videoSaving())' in view
        and 'optionItems = new String[]{"Record","Switch to camera","Video clips","Help"};' in view
    ),
    "Home/Recents resume retries preview with existing camera permission": (
        "boolean isPreviewRunning() { return started && camera != null; }" in preview
        and "RESUME_PREVIEW_RETRY" in activity
        and "cameraPreview.isPreviewRunning()" in activity
        and "phoneFrame.postDelayed(() ->" in activity
        and "checkSelfPermission(Manifest.permission.CAMERA)" in activity
        and "videoFinalising || videoStopWorkerRunning" in activity
    ),
    "Capture failure can restore preview after UI is resumed": (
        "cameraActivityResumed && phoneFrame != null" in activity
        and "phoneFrame.post(() ->" in activity
        and "showNativeCameraPreview(true);" in activity
    ),
    "Private photo and video retained when shared publishing fails": (
        "shared != null ? shared : result" in activity
        and "saved = privateCopy; // Keep the usable recording." in activity
        and "if (sharedPublished)" in activity
        and "deleteCapturedPhoto(privateCopy);" in activity
    ),
    "Nokia video Pause and Play use the in-house player": (
        'if ("LSK".equals(key) || "OK".equals(key))' in activity
        and 'videoPlayerPlaySoftkey.performClick();' in activity
        and 'play.setOnClickListener(v -> {' in activity
        and 'if (player.isPlaying()) {' in activity
        and 'player.pause();' in activity
        and 'player.start();' in activity
        and 'status.setText("Paused");' in activity
        and 'status.setText("Playing");' in activity
    ),
    "Nokia Gallery resumes after closing the player": (
        'back.setOnClickListener(v -> closeGalleryVideo());' in activity
        and 'if (player != null && phoneView != null) phoneView.onNativeVideoViewerClosed();' in activity
        and 'player.stopPlayback();' in activity
    ),
    "Nokia player uses the same close action for native RSK and Android Back": (
        'boolean handleVideoPlayerNokiaKey(String key)' in activity
        and 'if ("RSK".equals(key) || "END".equals(key))' in activity
        and 'closeGalleryVideo();' in activity
        and 'handleVideoPlayerNokiaKey("RSK");' in activity
        and 'phoneView.onNativeVideoViewerClosed()' in activity
        and '"galleryVideos".equals(page)' in view
    ),
    "C2Back diagnostics remain available for real hardware": (
        '"C2Back", "DISPATCH keyBack action="' in activity
        and '"C2Back", "ON_BACK_PRESSED playerBack"' in activity
        and '"C2Back", "NOKIA_RSK_VIDEO_CLOSE"' in activity
    ),
    "Gallery video and photo browsers stay Nokia-native": (
        'go("galleryVideoBrowser");' in view
        and 'go("galleryImageBrowser");' in view
        and 'MediaStore.Video.Media.EXTERNAL_CONTENT_URI' in view
        and 'MediaStore.Images.Media.EXTERNAL_CONTENT_URI' in view
    ),
}

for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)

if not all(checks.values()):
    raise SystemExit("Camera/Gallery source wiring checks failed")

print("Camera/Gallery source wiring checks passed; APK and device tests still needed.")
