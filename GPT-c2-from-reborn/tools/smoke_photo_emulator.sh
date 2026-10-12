#!/usr/bin/env bash
# Post-publication Android 11 camera photo and MediaStore visibility probe.
# This is a file/index test; external Oppo Gallery UI and Send receivers
# remain separate manual acceptance tests.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
package="com.moshenuch.c2reborn"
result="$report_dir/photo-results.txt"
: > "$result"
on_exit() {
  adb logcat -d -v threadtime -s C2Camera:V '*:S' > "$report_dir/photo-camera-log.txt" 2>&1 || true
  adb shell screencap -p /sdcard/c2-photo-probe.png 2>/dev/null || true
  adb pull /sdcard/c2-photo-probe.png "$report_dir/photo-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-photo-probe.png 2>/dev/null || true
}
trap on_exit EXIT
size="$(adb shell wm size | sed -n 's/.*: //p' | tail -n1 | tr -d '\r')"
if [[ ! "$size" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  echo "FAIL emulator size unavailable: $size" | tee -a "$result"
  exit 1
fi
width="${BASH_REMATCH[1]}" height="${BASH_REMATCH[2]}"
mapfile -t points < <(python3 - "$width" "$height" <<'PY'
import sys
w, h = map(int, sys.argv[1:])
scale = min(w / 360, h / 780)
ox, oy = (w - 360 * scale) / 2, (h - 780 * scale) / 2
for x, y in [(180, 474), (212, 474), (90, 414), (180, 505)]:
    print(f"{round(ox + x * scale)} {round(oy + y * scale)}")
PY
)
tap() {
  local name="$1" px py
  read -r px py <<< "${points[$2]}"
  echo "TAP $name: $px,$py" >> "$result"
  adb shell input tap "$px" "$py"
  sleep 0.8
}
adb shell pm grant "$package" android.permission.CAMERA >/dev/null 2>&1 || true
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
adb logcat -c || true
adb shell 'ls -1 /sdcard/Pictures/"C2 Reborn"/*.jpg 2>/dev/null' |
  tr -d '\r' | sort > "$report_dir/photo-before.txt" || true

tap "Home Menu" 0
tap "Menu right to Messaging" 1
tap "Menu right to Media" 1
tap "Open Media" 0
tap "Open Camera" 0
sleep 3
tap "Capture photo" 0

found=""
for i in $(seq 1 20); do
  found="$(adb shell 'ls -1 /sdcard/Pictures/"C2 Reborn"/*.jpg 2>/dev/null' |
    tr -d '\r' | grep -Fvx -f "$report_dir/photo-before.txt" | head -n1 || true)"
  [[ -n "$found" ]] && break
  sleep 1
done
if [[ -z "$found" ]]; then
  echo "FAIL no newly published JPEG in Pictures/C2 Reborn" | tee -a "$result"
  exit 1
fi
echo "PASS newly published JPEG: $found" | tee -a "$result"

if ! adb pull "$found" "$report_dir/emulator-captured.jpg" >/dev/null; then
  echo "FAIL unable to read shared JPEG from Android Pictures" | tee -a "$result"
  exit 1
fi
python3 - "$report_dir/emulator-captured.jpg" <<'PY' | tee -a "$result"
import sys
from pathlib import Path
jpeg = Path(sys.argv[1]).read_bytes()
if len(jpeg) <= 128 or not jpeg.startswith(b"\xff\xd8") or not jpeg.endswith(b"\xff\xd9"):
    raise SystemExit("FAIL JPEG missing SOI/EOI markers or too small")
print(f"PASS complete JPEG markers and size: {len(jpeg)} bytes")
PY
ffprobe -v error -select_streams v:0 -show_entries stream=codec_name,width,height \
  -of default=noprint_wrappers=1 "$report_dir/emulator-captured.jpg" \
  | tee "$report_dir/photo-jpeg-details.txt"
ffmpeg -nostdin -v error -i "$report_dir/emulator-captured.jpg" \
  -frames:v 1 -f null -
echo "PASS FFmpeg decoded saved photo" | tee -a "$result"

# The app's own post-commit MediaStore query must find its published row.
adb logcat -d -v brief -s C2Camera:V '*:S' > "$report_dir/photo-camera-log.txt" || true
if grep -q 'PHOTO_INDEX indexed=true pending=0' "$report_dir/photo-camera-log.txt"; then
  echo "PASS C2 query confirms nonpending MediaStore image row" | tee -a "$result"
else
  echo "FAIL C2 did not confirm its photo indexed in MediaStore" | tee -a "$result"
  exit 1
fi
echo "NOTE External Oppo Gallery UI visibility still requires manual test" | tee -a "$result"

# Test the actual C2 Photo > Options > Send path against a second Android
# package. A real recipient must open EXTRA_STREAM, not just display
# Android's share chooser.
receiver="GPT-c2-from-reborn/shareprobe/build/outputs/apk/debug/shareprobe-debug.apk"
if [[ ! -f "$receiver" ]]; then
  echo "FAIL CI receiver APK missing: $receiver" | tee -a "$result"
  exit 1
fi
adb install -r -t "$receiver" >> "$result"
adb logcat -c || true
tap "Photo result Options" 2
tap "Options down to Delete" 3
tap "Options down to Send" 3
tap "Select Send" 0
sleep 3

# Android's resolver is system UI. Locate the independent recipient by its
# actual visible accessibility label, not a device-dependent fixed position.
target_found=false
for attempt in 1 2 3; do
  adb shell uiautomator dump /sdcard/c2-share-ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/c2-share-ui.xml "$report_dir/share-ui.xml" >/dev/null 2>&1 || true
  if [[ -s "$report_dir/share-ui.xml" ]]; then
    coords="$(python3 - "$report_dir/share-ui.xml" <<'PY'
import re, sys, xml.etree.ElementTree as ET
try:
    root = ET.parse(sys.argv[1]).getroot()
    for item in root.iter("node"):
        text = item.attrib.get("text", "") + " " + item.attrib.get("content-desc", "")
        if "C2 Share Probe" in text:
            b = item.attrib.get("bounds", "")
            match = re.match(r"\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]", b)
            if match:
                x1,y1,x2,y2 = map(int, match.groups())
                print(f"{(x1+x2)//2} {(y1+y2)//2}")
                break
except Exception:
    pass
PY
)"
    if [[ -n "$coords" ]]; then
      read -r sharex sharey <<< "$coords"
      echo "TAP C2 Share Probe: $sharex,$sharey" >> "$result"
      adb shell input tap "$sharex" "$sharey"
      target_found=true
      break
    fi
  fi
  adb shell input swipe "$((width/2))" "$((height*3/4))" \
    "$((width/2))" "$((height/3))" 500
  sleep 1
done
adb logcat -d -v brief -s C2ShareProbe:V '*:S' > "$report_dir/share-receiver-log.txt" || true
if [[ "$target_found" != true ]]; then
  echo "FAIL recipient app missing from chooser - see share-ui.xml" | tee -a "$result"
  exit 1
fi
sleep 2
adb logcat -d -v brief -s C2ShareProbe:V '*:S' > "$report_dir/share-receiver-log.txt" || true
if grep -q 'PASS opened C2 JPEG from separate application' "$report_dir/share-receiver-log.txt"; then
  echo "PASS independent Android app received and decoded C2 JPEG share URI" | tee -a "$result"
else
  echo "FAIL photo recipient could not read C2 shared URI" | tee -a "$result"
  exit 1
fi
