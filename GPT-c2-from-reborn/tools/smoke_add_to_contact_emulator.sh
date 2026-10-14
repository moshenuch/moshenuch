#!/usr/bin/env bash
# Isolated Android 11 test: dial a new number and add it to an existing
# contact through Options > Add to contact. The only contact present is the
# one this probe seeds; verification reads the Contacts provider directly.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/add-to-contact-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
target_name="AAA C2 AddTarget"
target_existing="07555000000"
new_number="07900001234"

size="$(adb shell wm size | sed -n 's/.*: //p' | tail -1 | tr -d '\r')"
if [[ ! "$size" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  echo 'UNVERIFIED display geometry unavailable' | tee -a "$result"; exit 1
fi
# Named design-space points (360x780 reference scaled to the display).
mapfile -t points < <(python3 - "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" <<'PY'
import sys
w,h=map(int,sys.argv[1:])
scale=min(w/360,h/780)
ox=(w-360*scale)/2
oy=(h-780*scale)/2
named={
 'LSK':(90,414),'OK':(180,474),'DOWN':(180,505),
 '0':(180,716),'1':(90,557),'2':(180,557),'3':(270,557),
 '4':(90,610),'7':(90,663),'9':(270,663),
}
for name in ['LSK','OK','DOWN','0','1','2','3','4','7','9']:
 x,y=named[name]
 print(f"{name} {round(ox+x*scale)} {round(oy+y*scale)}")
PY
)
tap() {
  local name="$1" x y
  read -r _ x y <<< "$(printf '%s\n' "${points[@]}" | grep -E "^${name} ")"
  echo "TAP $name $x,$y" >> "$result"
  adb shell input tap "$x" "$y"
  sleep 1
}
capture_evidence() {
  adb logcat -d -v brief -s C2Contacts:V AndroidRuntime:E '*:S' > "$out/add-to-contact-log.txt" || true
  adb shell screencap -p /sdcard/c2-add-to-contact.png 2>/dev/null || true
  adb pull /sdcard/c2-add-to-contact.png "$out/add-to-contact-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-add-to-contact.png 2>/dev/null || true
}
trap capture_evidence EXIT

adb shell pm grant "$package" android.permission.READ_CONTACTS 2>/dev/null || true
adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true

raw_uri="$(adb shell content insert --uri content://com.android.contacts/raw_contacts | tr -d '\r')"
raw_id="${raw_uri##*/}"
if [[ ! "$raw_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not seed raw contact: $raw_uri" | tee -a "$result"; exit 1
fi
adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/name \
  --bind data1:s:"$target_name" >/dev/null
adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:"$target_existing" --bind data2:i:2 >/dev/null
echo "Seeded contact $target_name (raw id $raw_id) with $target_existing" >> "$result"

adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
foreground="$(adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -1 || true)"
[[ "$foreground" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $foreground" | tee -a "$result"; exit 1; }

# Type the new number on the Home keypad, then Options > Add to contact.
for d in 0 7 9 0 0 0 0 1 2 3 4; do tap "$d"; done
tap LSK
tap DOWN
tap DOWN
tap DOWN
tap OK
sleep 1
# Contact picker: the seeded contact is the only entry; confirm with OK.
tap OK
sleep 2

rows="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:mimetype:data1 \
  --where "mimetype='vnd.android.cursor.item/phone_v2' AND raw_contact_id=$raw_id" | tr -d '\r')"
echo "$rows" > "$out/add-to-contact-provider.txt"
if ! grep -Fq "data1=$new_number" <<< "$rows"; then
  echo "FAIL dialled number $new_number was not added to seeded contact $raw_id" | tee -a "$result"
  exit 1
fi
echo "PASS Add to contact appended exact dialled number $new_number to existing contact $raw_id" | tee -a "$result"
if ! grep -Fq "data1=$target_existing" <<< "$rows"; then
  echo "FAIL pre-existing number $target_existing missing from contact $raw_id" | tee -a "$result"
  exit 1
fi
echo "PASS pre-existing contact number $target_existing preserved" | tee -a "$result"
after="$(adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -1 || true)"
if [[ "$after" != *"$package/"* ]]; then
  echo "FAIL C2 lost foreground after Add to contact: $after" | tee -a "$result"
  exit 1
fi
echo 'PASS C2 remains foreground after Add to contact' | tee -a "$result"
