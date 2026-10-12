#!/usr/bin/env bash
# Optional video acceptance probe: exercise native Nokia Record/Stop with
# Android's emulated camera. A third-party Appetize/handset audit remains needed.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
package="com.moshenuch.c2reborn"
result="$report_dir/video-results.txt"
: > "$result"

finish() {
  adb logcat -d -v threadtime > "$report_dir/video-logcat.txt" 2>&1 || true
  adb shell screencap -p /sdcard/c2-video-probe.png 2>/dev/null || true
  adb pull /sdcard/c2-video-probe.png "$report_dir/video-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-video-probe.png 2>/dev/null || true
}
trap finish EXIT

# C2PhoneView draws its 360x780 Nokia surface centred and aspect-fitted.
# Derive touch coordinates from the actual emulator display; never assume
# Pixel 4 or 1080x1920 dimensions.
size="$(adb shell wm size | sed -n 's/.*: //p' | tail -n1 | tr -d '\r')"
if [[ ! "$size" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  echo "FAIL unable to determine emulator screen size: $size" | tee -a "$result"
  exit 1
fi
width="${BASH_REMATCH[1]}" height="${BASH_REMATCH[2]}"
mapfile -t touch_points < <(python3 - "$width" "$height" <<'PY'
import sys
w, h = map(int, sys.argv[1:])
scale = min(w / 360, h / 780)
ox, oy = (w - 360 * scale) / 2, (h - 780 * scale) / 2
# Centre OK, Right, Down in native 360x780 canvas coordinates.
for x, y in [(180, 474), (212, 474), (180, 505)]:
    print(f"{round(ox + x * scale)} {round(oy + y * scale)}")
PY
)
tap() {
  local label="$1" px py
  read -r px py <<< "${touch_points[$2]}"
  echo "TAP $label: $px,$py" >> "$result"
  adb shell input tap "$px" "$py"
  sleep 0.8
}
# MainActivity requests SMS, Contacts and Phone permissions at startup.
# Grant them for the isolated emulator and restart so no runtime permission
# sheet covers Nokia's on-screen keypad during the video probe.
for permission in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
                  CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG CAMERA RECORD_AUDIO; do
  adb shell pm grant "$package" "android.permission.$permission" 2>/dev/null || true
done
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
adb logcat -c || true

# Fresh install starts with Home's Menu on centre OK.
tap "Home Menu" 0
tap "Menu move right" 1
tap "Menu move right to Media" 1
tap "Open Media" 0
tap "Select Video camera row" 2
tap "Open Video camera" 0
sleep 3
# Do not mistake a clip left from a previous run for a newly recorded video.
# On an isolated emulator this is usually empty, but the assertion is useful
# if the test script is reused against a persistent emulator/device.
adb shell 'ls -1 /sdcard/Movies/"C2 Reborn"/*.mp4 2>/dev/null' |
  tr -d '\r' | sort > "$report_dir/video-before.txt" || true
tap "Record" 0
sleep 8
tap "Stop" 0

# Stop/MediaStore publication is asynchronous. Check up to 35 seconds.
found=""
for i in $(seq 1 35); do
  found="$(adb shell 'ls -1 /sdcard/Movies/"C2 Reborn"/*.mp4 2>/dev/null' |
    tr -d '\r' | grep -Fvx -f "$report_dir/video-before.txt" | head -n1 || true)"
  [[ -n "$found" ]] && break
  sleep 1
done
adb logcat -d -v brief -s C2Camera:V '*:S' > "$report_dir/video-camera-log.txt" || true
if [[ -n "$found" ]]; then
  echo "PASS saved MP4 in Movies/C2 Reborn: $found" | tee -a "$result"
  echo "Android files:" >> "$result"
  adb shell 'ls -l /sdcard/Movies/"C2 Reborn"/' >> "$result" || true
else
  echo "FAIL no shared MP4 detected within 35 seconds" | tee -a "$result"
  echo "Look for VIDEO_STOP/VIDEO_VALIDATE/VIDEO_SAVE in video-camera-log.txt" >> "$result"
  exit 1
fi
# Independently inspect the *actual downloaded file* for moov/mdat atoms,
# stream codecs, duration and decodability. C2's own VIDEO_VALIDATE log
# alone is not sufficient to prove the saved file can be opened.
if ! adb pull "$found" "$report_dir/emulator-recorded.mp4" >/dev/null; then
  echo "FAIL unable to pull recorded MP4 for independent verification" | tee -a "$result"
  exit 1
fi
if python3 GPT-c2-from-reborn/tools/check_recorded_mp4.py \
    "$report_dir/emulator-recorded.mp4" >"$report_dir/video-mp4-validation.txt" 2>&1; then
  cat "$report_dir/video-mp4-validation.txt" | tee -a "$result"
else
  cat "$report_dir/video-mp4-validation.txt" | tee -a "$result"
  echo "FAIL recorded file is incomplete, missing streams, or cannot decode" | tee -a "$result"
  exit 1
fi
if grep -q 'VIDEO_VALIDATE result=true' "$report_dir/video-camera-log.txt"; then
  echo "PASS recorded MP4 duration and dimensions validated by C2" | tee -a "$result"
else
  echo "WARN video validation log was not observed" | tee -a "$result"
fi
if grep -q 'VIDEO_SAVE result=shared' "$report_dir/video-camera-log.txt"; then
  echo "PASS C2 MediaStore publication completed" | tee -a "$result"
else
  echo "WARN shared-publication log was not observed" | tee -a "$result"
fi
echo "Scope: emulated video file only; cannot establish real microphone content or Appetize compatibility." | tee -a "$result"
