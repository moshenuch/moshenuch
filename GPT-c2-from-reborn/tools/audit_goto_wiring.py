#!/usr/bin/env python3
"""Fail CI if the tested Go to model stops being wired to real Android actions.

These are source-wiring assertions, NOT runtime camera/emulator assertions.
The Java smoke tests independently execute routing and serializer logic.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/moshenuch/c2reborn"
view = (JAVA / "C2PhoneView.java").read_text(encoding="utf-8")
activity = (JAVA / "MainActivity.java").read_text(encoding="utf-8")
router = (JAVA / "NativeGoToRouter.java").read_text(encoding="utf-8")

def body(source: str, signature: str) -> str:
    index = source.find(signature)
    if index < 0:
        raise AssertionError(f"Missing method: {signature}")
    start = source.find("{", index + len(signature))
    if start < 0:
        raise AssertionError(f"Missing method body: {signature}")
    depth = 0
    for n in range(start, len(source)):
        if source[n] == "{":
            depth += 1
        elif source[n] == "}":
            depth -= 1
            if depth == 0:
                return source[start:n + 1]
    raise AssertionError(f"Unclosed method: {signature}")

def verify(description: str, condition: bool) -> None:
    if not condition:
        raise AssertionError("FAIL: " + description)
    print("PASS: " + description)

load = body(view, "private void loadState()")
save = body(view, "private void saveGoToShortcuts()")
route = body(view, "private void openGoToShortcut(int shortcutId)")
capture = body(view, "private void startCameraCapture(boolean video)")
launch_capture = body(view, "private void launchHardwareCapture(boolean video)")
android_camera = body(activity, "public void launchCameraCapture(boolean video)")
result = body(activity, "protected void onActivityResult(int requestCode, int resultCode, Intent data)")
press = body(view, "private void press(String key)")
choose = body(view, "private void chooseOption()")
confirm = body(view, "private void executeConfirmed()")
oncreate = body(view, "public C2PhoneView(Context context)")

verify("Saved order loaded from goto_order_v1",
       'prefs.getString("goto_order_v1"' in load and "restoreSerialized(" in load)
verify("Saved visible IDs loaded from goto_visible_v1",
       'prefs.getString("goto_visible_v1"' in load and "restoreSerialized(" in load)
verify("Saved order encoded with tested model",
       'putString("goto_order_v1", goToShortcuts.serializedOrder())' in save)
verify("Saved visibility encoded with tested model",
       'putString("goto_visible_v1", goToShortcuts.serializedEnabled())' in save)
verify("SharedPreferences write committed asynchronously",
       ".apply()" in save)
verify("Startup invokes loadState",
       "loadState();" in oncreate)
verify("Go to selection resolves stable visible ID",
       'openGoToShortcut(goToShortcuts.visibleId(sel))' in view)
verify("Actual view dispatch calls the Java-tested router",
       "NativeGoToRouter.launch(shortcutId" in route)
verify("Real handler forwards video boolean to camera",
       "startCameraCapture(video);" in route)
verify("C2 Camera enters in-house viewfinder",
       'go(video ? "videoViewfinder" : "cameraViewfinder")' in capture)
verify("In-house photo shutter uses embedded JPEG capture",
       "captureNativePhoto();" in launch_capture)
verify("Video never delegates to Android recorder",
       "launchCameraCapture(" not in launch_capture and
       "In-house video recording is under development" in launch_capture)
verify("Android Video recorder selects ACTION_VIDEO_CAPTURE",
       "MediaStore.ACTION_VIDEO_CAPTURE" in android_camera and
       "MediaStore.ACTION_IMAGE_CAPTURE" in android_camera and
       "video\n                ? MediaStore.ACTION_VIDEO_CAPTURE" in android_camera)
verify("Android Video recorder selects VIDEO request code",
       "video ? REQ_CAPTURE_VIDEO : REQ_CAPTURE_IMAGE" in android_camera)
verify("Capture result accepts video request code",
       "REQ_CAPTURE_IMAGE || requestCode == REQ_CAPTURE_VIDEO" in result)
verify("Captured video is completed and recorded in Gallery",
       "C2MediaProvider.complete(this, pending, video, data)" in result and
       "phoneView.onCameraCaptureCompleted(video, saved)" in result)
verify("Restore defaults first opens a Yes/No confirmation",
       'requestConfirm("restoreGoTo"' in choose)
verify("Only Yes / centre OK invokes confirmed action",
       'if ("LSK".equals(key) || "OK".equals(key)) executeConfirmed();' in press)
verify("No / End cancels the confirmation",
       '"RSK".equals(key) || "END".equals(key)' in press and
       'confirmOpen = false;' in press)
verify("Confirmed restore resets the tested model",
       'if ("restoreGoTo".equals(action))' in confirm and
       "goToShortcuts.restoreDefaults();" in confirm and
       "saveGoToShortcuts();" in confirm)
verify("Runtime debugging emits only shortcut IDs and action modes",
       '"C2GoToAudit"' in view and '"C2GoToAudit"' in activity)
verify("Dispatch defines separate photo and video actions",
       "case 3: handler.capture(false)" in router and
       "case 4: handler.capture(true)" in router)

print("Go to source wiring: all checks passed")
