#!/usr/bin/env bash
# Emulator-only fixture: 153 synthetic old index entries + one REAL photo.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
result="$report_dir/gallery-capacity-results.txt"
: > "$result"
package="com.moshenuch.c2reborn"
python3 - "$report_dir/gallery-capacity-seed.xml" <<'PY'
import json,sys,xml.etree.ElementTree as ET
root=ET.Element("map")
items=[{"uri":f"content://c2-fixture/image/{i:03d}",
        "title":f"CI item {i:03d}","addedAt":1000000000+i}
       for i in range(1,154)]
ET.SubElement(root,"string",name="gallery_images_v1").text=json.dumps(items)
ET.ElementTree(root).write(sys.argv[1],encoding="utf-8",xml_declaration=True)
print("Prepared 153 Gallery fixture rows")
PY
adb shell am force-stop "$package"
# run-as does not guarantee the shell starts in the package data directory.
# Use the fully qualified app-owned path; only the disposable CI emulator's
# debug SharedPreferences are changed, never photos/videos on shared storage.
prefs_file="/data/data/$package/shared_prefs/c2_state.xml"
adb shell run-as "$package" mkdir -p "/data/data/$package/shared_prefs"
adb shell "run-as $package sh -c 'cat > $prefs_file'" < "$report_dir/gallery-capacity-seed.xml"
adb shell run-as "$package" cat "$prefs_file" > "$report_dir/gallery-capacity-seed-readback.xml"
python3 - "$report_dir/gallery-capacity-seed-readback.xml" <<'PY'
import sys,json,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
assert len(json.loads(root.find("./string[@name='gallery_images_v1']").text))==153
print("PASS emulator seeded 153 Gallery index entries")
PY
size="$(adb shell wm size | sed -n 's/.*: //p' | tail -n1 | tr -d '\r')"
[[ "$size" =~ ^([0-9]+)x([0-9]+)$ ]] || { echo "FAIL display size" | tee -a "$result"; exit 1; }
width="${BASH_REMATCH[1]}" height="${BASH_REMATCH[2]}"
mapfile -t coordinates < <(python3 - "$width" "$height" <<'PY'
import sys
w,h=map(int,sys.argv[1:])
s=min(w/360,h/780);ox=(w-360*s)/2;oy=(h-780*s)/2
for x,y in [(180,474),(212,474)]:
    print(f"{round(ox+x*s)} {round(oy+y*s)}")
PY
)
tap() {
  local x y
  read -r x y <<< "${coordinates[$1]}"
  adb shell input tap "$x" "$y"
  sleep 0.8
}
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
adb logcat -c || true
adb shell 'ls -1 /sdcard/Pictures/"C2 Reborn"/*.jpg 2>/dev/null' |
  tr -d '\r' | sort > "$report_dir/gallery-capacity-before.txt" || true
tap 0; tap 1; tap 1; tap 0; tap 0  # Home > Menu > Media > Camera
sleep 3
tap 0 # Capture a real photo, not a fixture
newphoto=""
for i in $(seq 1 24); do
  newphoto="$(adb shell 'ls -1 /sdcard/Pictures/"C2 Reborn"/*.jpg 2>/dev/null' |
    tr -d '\r' | grep -Fvx -f "$report_dir/gallery-capacity-before.txt" | head -n1 || true)"
  [[ -n "$newphoto" ]] && break
  sleep 1
done
if [[ -z "$newphoto" ]]; then
  echo "FAIL full Gallery index prevented shared JPEG save" | tee -a "$result"
  exit 1
fi
echo "PASS shared JPEG saved: $newphoto" | tee -a "$result"
adb shell screencap -p /sdcard/c2-gallery-capacity.png 2>/dev/null || true
adb pull /sdcard/c2-gallery-capacity.png "$report_dir/gallery-capacity-screen.png" >/dev/null 2>&1 || true
adb logcat -d -v brief -s C2Gallery:V C2Camera:V '*:S' > "$report_dir/gallery-capacity-log.txt" || true
if ! grep -q 'CAPTURE_RESULT page=cameraResult indexed=true imageCount=150' "$report_dir/gallery-capacity-log.txt"; then
  echo "FAIL Nokia photo result or Gallery index invalid" | tee -a "$result"
  exit 1
fi
echo "PASS native Nokia photo result shown at full index capacity" | tee -a "$result"
adb shell am force-stop "$package"
adb shell run-as "$package" cat "$prefs_file" > "$report_dir/gallery-capacity-final.xml"
python3 - "$report_dir/gallery-capacity-final.xml" <<'PY' | tee -a "$result"
import json,sys,xml.etree.ElementTree as ET
root=ET.parse(sys.argv[1]).getroot()
items=json.loads(root.find("./string[@name='gallery_images_v1']").text)
uris={x["uri"] for x in items}
assert len(items)==150, f"Expected 150 index rows, got {len(items)}"
for i in range(1,5):
    assert f"content://c2-fixture/image/{i:03d}" not in uris
for i in range(5,154):
    assert f"content://c2-fixture/image/{i:03d}" in uris
new=[u for u in uris if not u.startswith("content://c2-fixture/")]
assert len(new)==1 and new[0].startswith("content://media/"),new
print("PASS 153-entry index trimmed to 149 newest + one captured image")
print("PASS oldest four index rows evicted; real captured media indexed")
print("LIMITATION seeded entries are NOT 153 actual camera captures")
PY
