#!/usr/bin/env bash
# Isolated Android 11 test: delete only a JPEG captured during this probe.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/photo-delete-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
before="$out/photo-delete-before.txt"
list_jpegs() {
  adb shell 'ls -1 /sdcard/Pictures/"C2 Reborn"/*.jpg 2>/dev/null' | tr -d '\r' | sort || true
}
list_jpegs > "$before"
size="$(adb shell wm size | sed -n 's/.*: //p' | tail -1 | tr -d '\r')"
if [[ ! "$size" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  echo 'UNVERIFIED display geometry unavailable' | tee -a "$result"; exit 1
fi
mapfile -t points < <(python3 - "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" <<'PY'
import sys
w,h=map(int,sys.argv[1:])
scale=min(w/360,h/780)
ox=(w-360*scale)/2
oy=(h-780*scale)/2
for x,y in [(180,474),(212,474),(90,414),(180,505)]:
 print(round(ox+x*scale),round(oy+y*scale))
PY
)
tap() {
  local x y
  read -r x y <<< "${points[$2]}"
  echo "TAP $1 $x,$y" >> "$result"
  adb shell input tap "$x" "$y"
  sleep 1
}
capture_evidence() {
  adb logcat -d -v brief -s C2Camera:V C2Gallery:V AndroidRuntime:E '*:S' > "$out/photo-delete-log.txt" || true
  adb shell screencap -p /sdcard/c2-photo-delete.png 2>/dev/null || true
  adb pull /sdcard/c2-photo-delete.png "$out/photo-delete-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-photo-delete.png 2>/dev/null || true
}
trap capture_evidence EXIT
adb shell pm grant "$package" android.permission.CAMERA 2>/dev/null || true
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
foreground="$(adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -1 || true)"
[[ "$foreground" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $foreground" | tee -a "$result"; exit 1; }
tap 'Home Menu' 0
tap 'Menu right' 1
tap 'Menu right to Media' 1
tap 'Open Media' 0
tap 'Open Camera' 0
sleep 3
tap 'Capture' 0
created=''
for attempt in $(seq 1 20); do
  created="$(list_jpegs | grep -Fvx -f "$before" | head -1 || true)"
  [[ -n "$created" ]] && break
  sleep 1
done
[[ -n "$created" ]] || { echo 'FAIL dedicated JPEG was not captured' | tee -a "$result"; exit 1; }
echo "PASS captured dedicated JPEG $created" | tee -a "$result"
tap 'Photo Options' 2
tap 'Select Delete row' 3
tap 'Open Delete' 0
# Confirmation opens with Yes on left Nokia softkey; No is on right.
tap 'Confirm Yes' 2
sleep 3
if list_jpegs | grep -Fxq "$created"; then
  echo 'FAIL Delete Yes did not remove captured JPEG from shared Pictures' | tee -a "$result"
  exit 1
fi
echo 'PASS Delete Yes removed exact newly captured JPEG' | tee -a "$result"
if ! diff -u "$before" <(list_jpegs) >> "$result"; then
  echo 'FAIL unrelated JPEG listing changed after targeted deletion' | tee -a "$result"
  exit 1
fi
echo 'PASS pre-existing JPEG listing unchanged' | tee -a "$result"
