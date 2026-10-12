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
for x, y in [(180, 474), (212, 474)]:
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
echo "NOTE Oppo Gallery visibility and share-target attachment access still require manual tests" | tee -a "$result"
