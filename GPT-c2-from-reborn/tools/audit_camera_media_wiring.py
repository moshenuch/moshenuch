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
    "Recording finalizes before camera is released": (
        "boolean valid = cameraPreview.stopRecording();" in activity
        and "if (recording) stopRecording();" in preview
    ),
    "Capture failure can restore preview after UI is resumed": (
        "cameraActivityResumed && phoneFrame != null" in activity
        and "phoneFrame.post(() ->" in activity
        and "showNativeCameraPreview(true);" in activity
    ),
    "Private photo and video retained when publishing fails": (
        "shared != null ? shared : result" in activity
        and "shared != null ? shared : saved" in activity
    ),
}

for name, ok in checks.items():
    print(("PASS" if ok else "FAIL") + ": " + name)

if not all(checks.values()):
    raise SystemExit("Camera/Gallery source wiring checks failed")

print("Camera/Gallery source wiring checks passed; APK and device tests still needed.")
