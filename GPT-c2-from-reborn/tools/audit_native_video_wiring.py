#!/usr/bin/env python3
"""Build-side source guard for C2's in-house video flow (not a device test)."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
java = root / "app/src/main/java/com/moshenuch/c2reborn"
preview = (java / "C2CameraPreview.java").read_text()
activity = (java / "MainActivity.java").read_text()
phone = (java / "C2PhoneView.java").read_text()
manifest = (root / "app/src/main/AndroidManifest.xml").read_text()

checks = {
    "embedded MediaRecorder configured": "new MediaRecorder()" in preview
        and "recorder.setCamera(camera)" in preview,
    "MPEG-4 output configured": "MediaRecorder.OutputFormat.MPEG_4" in preview,
    "Video duration and size limits are configured": ("recorder.setMaxDuration(120_000)" in preview
        and "recorder.setMaxFileSize(150L * 1024L * 1024L)" in preview
        and "MEDIA_RECORDER_INFO_MAX_DURATION_REACHED" in preview),
    "record/stop lifecycle": "boolean startRecording(File output)" in preview
        and "boolean stopRecording()" in preview,
    "surface loss finalises video": "finishNativeVideoCapture();" in preview,
    "video microphone permission declared": "android.permission.RECORD_AUDIO" in manifest,
    "video microphone runtime request": "REQ_VIDEO_MIC" in activity,
    "native record and stop actions": "host.toggleNativeVideoCapture();" in phone,
    "video completion navigates C2 gallery": 'go("galleryVideos");' in phone,
    "public Movies publishing": "MediaStore.Video.Media.EXTERNAL_CONTENT_URI" in activity,
    "recording finalised when app pauses": "if (isNativeVideoRecording()) finishNativeVideoCapture();" in activity,
    "no unimplemented recorder placeholder": "In-house video recording is under development" not in phone,
}

for description, passed in checks.items():
    print(("PASS" if passed else "FAIL") + ": " + description)
if not all(checks.values()):
    raise SystemExit("C2 native video source checks failed")
print("Source wiring checks passed. Android compilation and on-device capture still required.")
