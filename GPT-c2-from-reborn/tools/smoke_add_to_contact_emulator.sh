#!/usr/bin/env bash
# Isolated Android 11 acceptance for Add to contact (v2.03.1):
#  A. Dialer: dial a new number, Options > Add to contact, pick the seeded
#     contact; the number must land on that contact and nothing else moves.
#  B. Duplicate: repeating the same add must be refused with no provider write.
#  C. Cancel: leaving the picker with Back must not write.
#  D. Conversations: an SMS sender can be added to an existing contact from the
#     Conversations options menu. SMS provider seeding is best-effort from the
#     shell; when the seed is refused this section reports UNVERIFIED.
# The only contact present is the one this probe seeds; verification reads the
# Contacts provider directly.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/add-to-contact-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
target_name="AAAC2AddTarget"
target_existing="07555000000"
new_number="07900001234"
sms_sender="07777000111"

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
 'LSK':(90,414),'RSK':(270,414),'OK':(180,474),'DOWN':(180,505),'RIGHT':(212,474),
 '0':(180,716),'1':(90,557),'2':(180,557),'3':(270,557),
 '4':(90,610),'7':(90,663),'9':(270,663),
}
for name in ['LSK','RSK','OK','DOWN','RIGHT','0','1','2','3','4','7','9']:
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

foreground_package() {
  adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -1 || true
}
phone_rows() {
  # Unfiltered provider dump filtered locally: adb shell re-splits arguments,
  # so a remote --where with spaces or quoted values never reaches the parser.
  adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:mimetype:data1 2>&1 | tr -d '\r' \
    | grep "raw_contact_id=$raw_id," || true
}
count_phone_rows() {
  phone_rows | grep -c "data1=$1" || true
}
expect_foreground() {
  local fg
  fg="$(foreground_package)"
  if [[ "$fg" != *"$package/"* ]]; then
    echo "FAIL C2 lost foreground: $fg" | tee -a "$result"
    exit 1
  fi
}

adb shell pm grant "$package" android.permission.READ_CONTACTS 2>/dev/null || true
adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true

# --- Seed one device contact -------------------------------------------
# content insert refuses a bind-less insert ("Bindings not specified."), so
# the raw contact carries a harmless starred=0 bind. All values are single
# tokens: adb shell re-joins arguments and the remote tokenizer splits on
# spaces, so a name containing a space would fail as an unsupported argument.
# The API 30 `content` tool prints nothing on a successful insert (only the
# inserted row's existence proves success), so read the new raw id back.
seed_out="$(adb shell content insert --uri content://com.android.contacts/raw_contacts \
  --bind starred:i:0 2>&1 | tr -d '\r')"
echo "seed raw_contacts insert output: ${seed_out:-<empty (success on API 30)>}" >> "$result"
raw_id="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
  --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n | tail -1 || true)"
if [[ ! "$raw_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not seed raw contact: ${seed_out:-<empty>}" | tee -a "$result"; exit 1
fi
seed_out="$(adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/name \
  --bind data1:s:"$target_name" 2>&1 | tr -d '\r')"
echo "seed name row: $seed_out" >> "$result"
seed_out="$(adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:"$target_existing" --bind data2:i:2 2>&1 | tr -d '\r')"
echo "seed phone row: $seed_out" >> "$result"
if ! grep -Fq "$target_existing" <<< "$(phone_rows)"; then
  echo "UNVERIFIED seeded contact unreadable in provider (raw id $raw_id)" | tee -a "$result"
  exit 1
fi
echo "Seeded contact $target_name (raw id $raw_id) with $target_existing" >> "$result"

adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
fg="$(foreground_package)"
[[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $fg" | tee -a "$result"; exit 1; }

# --- A. Dialer add -------------------------------------------------------
for d in 0 7 9 0 0 0 0 1 2 3 4; do tap "$d"; done
tap LSK
tap DOWN; tap DOWN; tap DOWN   # dialer options: Call, Save, Send message, Add to contact
tap OK
sleep 1
tap OK                          # picker: the seeded contact is the only entry
sleep 2

rows="$(phone_rows)"
echo "$rows" > "$out/add-to-contact-provider.txt"
if grep -Fq 'Permission Denial' <<< "$rows"; then
  echo "UNVERIFIED contacts provider denied shell reads: $rows" | tee -a "$result"; exit 1
fi
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
expect_foreground
echo 'PASS C2 remains foreground after Add to contact' | tee -a "$result"

# --- B. Duplicate attempt must be refused ---------------------------------
tap LSK
tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
tap OK                          # pick the same contact again
sleep 2
if [[ "$(count_phone_rows "$new_number")" != "1" ]]; then
  echo "FAIL duplicate Add to contact wrote another row: $(count_phone_rows "$new_number") rows for $new_number" | tee -a "$result"
  phone_rows >> "$out/add-to-contact-provider.txt"
  exit 1
fi
echo "PASS duplicate Add to contact refused, provider unchanged" | tee -a "$result"
expect_foreground

# --- C. Picker cancel via Back must not write ------------------------------
tap LSK
tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
tap RSK                         # Back out of the picker
sleep 1
if [[ "$(count_phone_rows "$new_number")" != "1" ]]; then
  echo "FAIL picker Back wrote a row: $(count_phone_rows "$new_number") rows for $new_number" | tee -a "$result"
  phone_rows >> "$out/add-to-contact-provider.txt"
  exit 1
fi
echo "PASS picker cancel via Back left provider unchanged" | tee -a "$result"
expect_foreground

# --- D. Conversations entry -----------------------------------------------
now_ms="$(date +%s)000"
sms_out="$(adb shell content insert --uri content://sms \
  --bind address:s:"$sms_sender" --bind body:s:Hi \
  --bind type:i:1 --bind date:l:"$now_ms" 2>&1 | tr -d '\r')"
echo "seed sms row: $sms_out" >> "$result"
sms_check="$(adb shell content query --uri content://sms \
  --projection address:body 2>&1 | tr -d '\r' || true)"
if ! grep -Fq "address=$sms_sender" <<< "$sms_check"; then
  echo "UNVERIFIED conversations section skipped: SMS provider refused shell seeding: $sms_out" | tee -a "$result"
  exit 0
fi

adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
fg="$(foreground_package)"
[[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $fg" | tee -a "$result"; exit 1; }

tap OK                          # home menu grid (selection starts at index 0)
tap DOWN; tap RIGHT             # grid index 4: messaging
tap OK
sleep 1
tap DOWN                        # messaging list index 1: Conversations
tap OK
sleep 1
tap LSK                         # conversations options
tap DOWN; tap DOWN              # Call, Save a contact, Add to contact
tap OK
sleep 1
tap OK                          # picker: seeded contact is the only entry
sleep 2

rows="$(phone_rows)"
echo "$rows" >> "$out/add-to-contact-provider.txt"
if ! grep -Fq "data1=$sms_sender" <<< "$rows"; then
  echo "FAIL conversations Add to contact did not append sender $sms_sender to contact $raw_id" | tee -a "$result"
  exit 1
fi
echo "PASS Conversations Add to contact appended sender number $sms_sender to existing contact $raw_id" | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Conversations Add to contact' | tee -a "$result"
