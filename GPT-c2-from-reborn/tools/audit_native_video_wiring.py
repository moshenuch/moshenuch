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
    "MP4 video metadata checked before Movies publication": (
        "private boolean isPlayableNativeVideo(Uri privateUri)" in activity
        and "METADATA_KEY_VIDEO_WIDTH" in activity
        and "METADATA_KEY_DURATION" in activity
        and "!isPlayableNativeVideo(privateCopy)" in activity
    ),
    "Stalled native Stop recovers Nokia navigation without cancelling encoder worker": (
        "videoSaveHandler.postDelayed(videoStopTimeout, 12_000L)" in activity
        and "videoStopWorkerRunning = true;" in activity
        and "onNativeVideoFinalisationTimedOut()" in activity
        and "onNativeVideoFinalisationTimedOut()" in phone
        and "onNativeVideoSavedAfterTimeout(Uri uri)" in phone
    ),
    "Native video Stop logs thrown exception and blocked worker Java stack": (
        "VIDEO_STOP_THROW " in preview
        and "VIDEO_STOP_BEGIN thread=" in activity
        and "VIDEO_STOP_WORKER_STACK stage=" in activity
        and "stuck.getStackTrace()" in activity
        and "VIDEO_STOP_UNCAUGHT stage=" in activity
    ),
    "Native Stop failure renders Nokia recovery screen with Retry and Back": (
        'else if ("videoError".equals(page)) drawNokiaVideoError(c);' in phone
        and 'if ("videoError".equals(page)) return new String[]{"Report", "Retry", "Back"};' in phone
        and "onNativeVideoFinalisationFailed(" in phone
        and "public void onNativeVideoFinalisationTimedOut()" in phone
        and 'videoErrorReason' in phone
    ),
    "No-PC diagnostic report shares exception or stuck worker stack on user action": (
        "StackTraceElement[] lastStopExceptionStack()" in preview
        and "lastStopExceptionStack = ex.getStackTrace();" in preview
        and "lastVideoDiagnostic = formatVideoDiagnostic(" in activity
        and "MediaRecorder.stop timed out after 12 seconds" in activity
        and "cameraPreview.lastStopExceptionStack()" in activity
        and 'void shareLastVideoDiagnostic()' in activity
        and 'send.setType("text/plain")' in activity
        and 'send.putExtra(Intent.EXTRA_TEXT, lastVideoDiagnostic);' in activity
        and '"LSK".equals(key)' in phone
        and "shareLastVideoDiagnostic()" in phone
    ),
    "Diagnostic explicitly excludes media and personal content": (
        "No recorded media, contacts, messages or private file URIs attached." in activity
        and "No video recording failure reported." in activity
        and 'new Intent(Intent.ACTION_SEND)' in activity
        and 'send.setType("text/plain")' in activity
    ),
    "On-screen error identifies MediaRecorder.stop exception type": (
        'String lastStopFailure()' in preview
        and 'lastStopFailure = ex.getClass().getSimpleName();' in preview
        and '"Encoder stop error: " + stopError' in activity
    ),
    "Nokia error screen displays failed stage without adb": (
        'String lastNativeVideoFailureStage()' in activity
        and 'lastVideoFailureStage = videoFinaliseStage;' in activity
        and 'c.drawText("Stage: " + failedStage' in phone
    ),
    "Retry cannot overlap a still-stalled encoder": (
        "isNativeVideoStopWorkerRunning()" in activity
        and "host.isNativeVideoStopWorkerRunning()" in phone
        and "Recorder busy - restart C2 to retry" in phone
    ),

    "Recorder stop and publication run off Android UI thread": (
        "Thread worker = new Thread(() ->" in activity
        and "valid = cameraPreview.stopRecording();" in activity
        and "publishVideoToMovies(privateCopy)" in activity
        and "worker.start();" in activity
    ),
    "Native video Options do not show photo Capture action": (
        'optionItems = new String[]{"Record","Switch to camera","Video clips","Help"};' in phone
        and 'if ("videoViewfinder".equals(page)) {' in phone
    ),
    "Video saving protects Nokia softkeys and prevents duplicate Stop": (
        "videoFinalising = true;" in activity
        and "onNativeVideoSaving()" in activity
        and "if (videoSaving())" in phone
    ),
    "surface loss finalises video": "finishNativeVideoCapture();" in preview,
    "SurfaceChanged cannot restart preview while encoder is stopping": (
        "if (camera != null && started && !recording && !stopping)" in preview
        and "if (recording || stopping)" in preview
    ),
    "Video preview size matches supported CamcorderProfile before unlock": (
        "params.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight)" in preview
        and "camera.setParameters(params);" in preview
        and preview.index("params.setPreviewSize(") < preview.index("camera.unlock();")
    ),
    "Recorder does not replace the camera-owned preview surface": (
        "camera.setPreviewDisplay(getHolder());" in preview
        and "recorder.setPreviewDisplay(" not in preview
    ),
    "Blocked finaliser does not permit camera reuse": (
        "if (recording || stopping || camera == null" in preview
        and "videoStopWorkerRunning = true;" in activity
        and "if (cameraPreview == null || videoFinalising || videoStopWorkerRunning) return;" in activity
    ),
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
