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
    "Photo result respects EXIF orientation": (
        "orientCapturedPhotoPreview(uri, capturedPhotoPreview)" in view
        and "android.media.ExifInterface.TAG_ORIENTATION" in view
        and "Bitmap.createBitmap(" in view
    ),
    "Delete confirmation uses Nokia Canvas rather than stock grey dialog": (
        "private void drawConfirm(Canvas c)" in view
        and "c.drawRect(11, 93, 229, 208, p);" in view
        and 'c.drawText("Confirm"' not in view
    ),
    "Save and Send verify committed photo still readable": (
        "boolean capturedPhotoExists(Uri uri)" in activity
        and "!((MainActivity) getContext()).capturedPhotoExists(capturedPhotoUri)" in view
        and "if (!capturedPhotoExists(uri))" in activity
    ),
    "Private media delete is implemented": (
        'if (!"image".equals(category) && !"video".equals(category)) return 0;' in provider
        and 'file.isFile() && file.delete() ? 1 : 0' in provider
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
}

for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)

if not all(checks.values()):
    raise SystemExit("Camera/Gallery source wiring checks failed")

print("Camera/Gallery source wiring checks passed; APK and device tests still needed.")
