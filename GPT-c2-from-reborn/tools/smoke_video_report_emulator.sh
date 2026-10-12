#!/usr/bin/env bash
# Android 11 integration test for the REAL Nokia Report softkey and Sharesheet.
# Trigger a deterministic debug-only error through app-private preferences.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
out="$report_dir/video-report-results.txt"
: > "$out"
pkg="com.moshenuch.c2reborn"
receiver="GPT-c2-from-reborn/shareprobe/build/outputs/apk/debug/shareprobe-debug.apk"
if [[ ! -f "$receiver" ]]; then
  echo "FAIL diagnostic receiver APK unavailable" | tee -a "$out"
  exit 1
fi
adb install -r -t "$receiver" >> "$out"
adb shell am force-stop "$pkg"
cat > "$report_dir/video-report-fixture.xml" <<'XML'
<?xml version="1.0" encoding="utf-8" standalone="yes" ?>
<map>
    <boolean name="synthetic_error_once" value="true" />
</map>
XML
prefs="/data/data/$pkg/shared_prefs/c2_ci_video_report.xml"
adb shell run-as "$pkg" mkdir -p "/data/data/$pkg/shared_prefs"
adb shell "run-as $pkg sh -c 'cat > $prefs'" < "$report_dir/video-report-fixture.xml"
adb logcat -c || true
adb shell am start -W -n "$pkg/.MainActivity" >> "$out"
sleep 2

size="$(adb shell wm size | sed -n 's/.*: //p' | tail -n1 | tr -d '\r')"
[[ "$size" =~ ^([0-9]+)x([0-9]+)$ ]] ||
  { echo "FAIL emulator dimensions unavailable" | tee -a "$out"; exit 1; }
width="${BASH_REMATCH[1]}" height="${BASH_REMATCH[2]}"
read -r reportx reporty < <(python3 - "$width" "$height" <<'PY'
import sys
w,h=map(int,sys.argv[1:])
s=min(w/360,h/780)
ox=(w-360*s)/2;oy=(h-780*s)/2
print(round(ox+90*s),round(oy+414*s))
PY
)
adb shell screencap -p /sdcard/c2-video-report-error.png >/dev/null 2>&1 || true
adb pull /sdcard/c2-video-report-error.png "$report_dir/video-report-error-screen.png" >/dev/null 2>&1 || true

echo "TAP Nokia Report softkey: $reportx,$reporty" >> "$out"
adb shell input tap "$reportx" "$reporty"
sleep 3

# Locate the separate text/plain receiver in the real Android Sharesheet.
found=false
for attempt in 1 2 3; do
  adb shell uiautomator dump /sdcard/c2-video-report-ui.xml >/dev/null 2>&1 || true
  adb pull /sdcard/c2-video-report-ui.xml "$report_dir/video-report-ui.xml" >/dev/null 2>&1 || true
  xy="$(python3 - "$report_dir/video-report-ui.xml" <<'PY'
import re,sys,xml.etree.ElementTree as ET
try:
    root=ET.parse(sys.argv[1]).getroot()
    for node in root.iter("node"):
        s=node.attrib.get("text","")+" "+node.attrib.get("content-desc","")
        if "C2 Share Probe" not in s:continue
        m=re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",node.attrib.get("bounds",""))
        if not m:continue
        x1,y1,x2,y2=map(int,m.groups())
        print((x1+x2)//2,(y1+y2)//2)
        break
except Exception:
    pass
PY
)"
  if [[ -n "$xy" ]]; then
    read -r x y <<< "$xy"
    adb shell input tap "$x" "$y"
    echo "TAP external diagnostic receiver: $x,$y" >> "$out"
    found=true
    break
  fi
  adb shell input swipe "$((width/2))" "$((height*3/4))" \
    "$((width/2))" "$((height/3))" 500
  sleep 1
done

sleep 2
adb logcat -d -v brief -s C2Camera:V C2ShareProbe:V '*:S' \
  > "$report_dir/video-report-receiver-log.txt" || true
if grep -q 'VIDEO_DIAGNOSTIC chooser opened by user' \
    "$report_dir/video-report-receiver-log.txt"; then
  echo "PASS Nokia Report invoked Android diagnostic Sharesheet" | tee -a "$out"
else
  echo "FAIL Nokia Report did not open diagnostic Sharesheet" | tee -a "$out"
  exit 1
fi
if [[ "$found" != true ]]; then
  echo "FAIL recipient not found in Sharesheet; inspect video-report-ui.xml" | tee -a "$out"
  exit 1
fi
if grep -q 'PASS received C2 video diagnostic text from separate app' \
    "$report_dir/video-report-receiver-log.txt"; then
  echo "PASS separate app validated stop failure stage, version and Java stack" | tee -a "$out"
else
  echo "FAIL recipient rejected or could not receive text diagnostic" | tee -a "$out"
  exit 1
fi
echo "NOTE synthetic Stop failure; physical camera outcome still unverified" | tee -a "$out"
