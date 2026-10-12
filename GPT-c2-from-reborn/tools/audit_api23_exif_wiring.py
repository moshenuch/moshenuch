#!/usr/bin/env python3
"""Source-wiring regression gate for the API 23 EXIF compatibility change."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text()
gradle = (root / "app/build.gradle").read_text()
props = (root / "gradle.properties").read_text()
start = source.index("private Bitmap orientCapturedPhotoPreview(")
end = source.index("private void drawCameraResult(", start)
helper = source[start:end]
assert "androidx.exifinterface.media.ExifInterface" in helper
assert "android.media.ExifInterface" not in helper
assert "SDK_INT < 24" not in helper
assert "openInputStream(uri)" in helper
assert "ExifInterface.TAG_ORIENTATION" in helper
assert "Bitmap.createBitmap(" in helper
assert "androidx.exifinterface:exifinterface:1.3.7" in gradle
assert "minSdk 23" in gradle
assert "android.useAndroidX=true" in props
assert source.count("orientCapturedPhotoPreview(") >= 3, (
    "Expected helper declaration and both camera result / Gallery call sites"
)
assert "galleryPreviewBitmap = orientCapturedPhotoPreview(" in source
assert "capturedPhotoPreview = orientCapturedPhotoPreview(" in source
print("PASS: API 23 EXIF parser and camera/Gallery orientation wiring")
print("NOTE: static checks do not prove visual orientation correctness.")
