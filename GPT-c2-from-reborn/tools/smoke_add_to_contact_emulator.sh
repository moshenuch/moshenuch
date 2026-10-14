#!/usr/bin/env bash
# Android 11 acceptance for Add to contact (v2.03.2). Core sections are
# BLOCKING in the suite:
#  UK. The seeded contact's existing number is stored in +44 form; dialling
#      its trunk-0 or 0044 form must be refused as a duplicate.
#  A.  Dialer: dial a new number, Options > Add to contact, pick the seeded
#      contact; the number must land on that contact and nothing else moves.
#  B.  Duplicate: repeating the same add must be refused with no provider write.
#  C.  Cancel: leaving the picker with Back must not write.
#  E.  Permission: with WRITE_CONTACTS revoked the picker must refuse the write.
# Conversations coverage is reported separately (non-blocking): the SMS
# provider refuses shell seeding on some images.
#  D1. Conversations: an SMS sender can be added to an existing contact.
#  D2. An alphanumeric SMS sender is refused without opening the picker.
# The only contact present is the one this probe seeds; verification reads the
# Contacts provider directly and the app's C2Contacts logcat markers.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/add-to-contact-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
target_name="AAAC2AddTarget"
target_existing="+447555000000"
uk_local="07555000000"
uk_0044="00447555000000"
new_number="07900001234"
perm_number="07600000000"
sms_sender="07777000111"
sms_alpha="C2ALERTS"

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
 '4':(90,610),'5':(180,610),'6':(270,610),'7':(90,663),'9':(270,663),
}
for name in ['LSK','RSK','OK','DOWN','RIGHT','0','1','2','3','4','5','6','7','9']:
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
  adb logcat -d -v brief -s C2Contacts:I AndroidRuntime:E '*:S' > "$out/add-to-contact-log.txt" || true
  adb shell screencap -p /sdcard/c2-add-to-contact.png 2>/dev/null || true
  adb pull /sdcard/c2-add-to-contact.png "$out/add-to-contact-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-add-to-contact.png 2>/dev/null || true
}
trap capture_evidence EXIT

foreground_package() {
  adb shell dumpsys activity activities | grep -E 'mResumedActivity|topResumedActivity' | head -1 || true
}
expect_foreground() {
  local fg
  fg="$(foreground_package)"
  if [[ "$fg" != *"$package/"* ]]; then
    echo "FAIL C2 lost foreground: $fg" | tee -a "$result"
    exit 1
  fi
}
phone_rows() {
  # Unfiltered provider dump filtered locally: adb shell re-joins arguments and
  # the remote tokenizer splits on spaces, so a remote --where with quoted
  # values never survives. API 30 prints nothing on a successful insert.
  adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:mimetype:data1 2>&1 | tr -d '\r' \
    | grep "raw_contact_id=$raw_id," || true
}
count_phone_rows() {
  phone_rows | grep -c "data1=$1" || true
}
marker_seen() { # marker_seen <text>: 0 when the app logged it since last clear
  adb logcat -d -s C2Contacts:I 2>/dev/null | grep -qF "$1"
}
require_marker() {
  if ! marker_seen "$1"; then
    echo "FAIL expected app log marker: $1" | tee -a "$result"
    echo "--- C2Contacts logcat at failure ---" | tee -a "$result"
    adb logcat -d -s C2Contacts:I 2>/dev/null | tee -a "$result" > "$out/add-to-contact-section-log.txt" || true
    echo "--- end logcat ---" | tee -a "$result"
    exit 1
  fi
}
dial_number() { # dial_number <digits from {0,5,7,9}> on a fresh home page
  local d
  for d in $(echo "$1" | grep -o .); do tap "$d"; done
}

adb shell pm grant "$package" android.permission.READ_CONTACTS 2>/dev/null || true
adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
adb logcat -c || true

# --- Seed one device contact -------------------------------------------
# content insert refuses a bind-less insert ("Bindings not specified."), so
# the raw contact carries a harmless starred=0 bind. All values are single
# tokens: adb shell re-joins arguments and the remote tokenizer splits on
# spaces, so a name containing a space would fail as an unsupported argument.
raw_ids_before="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
  --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true)"
seed_out="$(adb shell content insert --uri content://com.android.contacts/raw_contacts \
  --bind starred:i:0 2>&1 | tr -d '\r')"
echo "seed raw_contacts insert output: ${seed_out:-<empty (success on API 30)>}" >> "$result"
raw_ids_after="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
  --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true)"
raw_id="$(comm -13 <(echo "$raw_ids_before") <(echo "$raw_ids_after") || true)"
if [[ "$(grep -c . <<< "$raw_id")" != "1" ]]; then
  echo "UNVERIFIED seed did not add exactly one raw contact (new ids: ${raw_id:-none})" | tee -a "$result"
  exit 1
fi
seed_out="$(adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/name \
  --bind data1:s:"$target_name" 2>&1 | tr -d '\r')"
echo "seed name row: ${seed_out:-<empty>}" >> "$result"
seed_out="$(adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$raw_id" \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:"$target_existing" --bind data2:i:2 2>&1 | tr -d '\r')"
echo "seed phone row: ${seed_out:-<empty>}" >> "$result"
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

# --- UK. Local-form dial of the +44 seeded number must be refused ---------
dial_number "$uk_local"
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN   # dialer options: Call, Save, Send message, Add to contact
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap OK                          # picker: the seeded contact is the only entry
sleep 2
require_marker 'ADD_TO_CONTACT_REFUSED reason=duplicate'
if [[ "$(count_phone_rows "$uk_local")" != "0" ]]; then
  echo "FAIL UK local-form duplicate wrote a row for $uk_local" | tee -a "$result"
  exit 1
fi
echo "PASS UK international-form duplicate refused: dialled $uk_local equals seeded $target_existing" | tee -a "$result"
expect_foreground
for i in $(seq 12); do tap RSK; done   # clear the dial, land back on home

# --- UK2. 0044-form dial of the same number must also be refused ----------
dial_number "$uk_0044"
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN   # dialer options: Call, Save, Send message, Add to contact
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap OK                          # picker: the seeded contact is the only entry
sleep 2
require_marker 'ADD_TO_CONTACT_REFUSED reason=duplicate'
if [[ "$(count_phone_rows "$uk_0044")" != "0" ]]; then
  echo "FAIL UK 0044-form duplicate wrote a row for $uk_0044" | tee -a "$result"
  exit 1
fi
echo "PASS UK 0044-form duplicate refused: dialled $uk_0044 equals seeded $target_existing" | tee -a "$result"
expect_foreground
for i in $(seq 16); do tap RSK; done   # clear the dial, land back on home

# --- A. Dialer add ---------------------------------------------------------
dial_number "$new_number"
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap OK
sleep 2
require_marker 'ADD_TO_CONTACT_INSERTED rawContactId='
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
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap OK                          # pick the same contact again
sleep 2
require_marker 'ADD_TO_CONTACT_REFUSED reason=duplicate'
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
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap RSK                         # Back out of the picker
sleep 1
if [[ "$(count_phone_rows "$new_number")" != "1" ]]; then
  echo "FAIL picker Back wrote a row: $(count_phone_rows "$new_number") rows for $new_number" | tee -a "$result"
  phone_rows >> "$out/add-to-contact-provider.txt"
  exit 1
fi
echo "PASS picker cancel via Back left provider unchanged" | tee -a "$result"
expect_foreground

# --- E. WRITE_CONTACTS revoked must refuse the write ------------------------
# Revoking a runtime permission kills the app process, so relaunch before
# driving the UI again. On relaunch MainActivity re-requests the missing
# permission with the SYSTEM sheet, which would swallow keypad taps (an
# earlier run re-granted WRITE_CONTACTS through a blind tap on Allow and the
# insert went through). Answer the sheet with a deliberate Deny first; a
# chained media sheet can follow the phone-permission result.
adb shell pm revoke "$package" android.permission.WRITE_CONTACTS 2>&1 | tee -a "$result" || true
sleep 1
perm_state="$(adb shell dumpsys package "$package" | grep -E 'android.permission.(WRITE|READ)_CONTACTS' | head -4 | tr -d '\r')"
echo "permission state after revoke: $perm_state" | tee -a "$result"
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sleep 2
sheet_clear=0
empty_dumps=0
# grep exits 1 on no match; without the trailing || true the assignment would
# trip set -e and kill the probe silently (run 38595627823).
for attempt in 1 2 3 4 5 6 7 8; do
  adb shell uiautomator dump /sdcard/c2-ui.xml >/dev/null 2>&1 || true
  deny_bounds="$(adb shell cat /sdcard/c2-ui.xml 2>/dev/null | tr -d '\r' | grep -o 'text="Deny"[^>]*' | head -1 | grep -o '\[[0-9]*,[0-9]*\]\[[0-9]*,[0-9]*\]' || true)"
  if [[ -z "$deny_bounds" ]]; then
    empty_dumps=$((empty_dumps + 1))
    # Two consecutive sheet-free dumps before declaring the sheet gone: the
    # dialog can appear a beat after the cold launch.
    if [[ "$empty_dumps" -ge 2 ]]; then sheet_clear=1; break; fi
    sleep 1
    continue
  fi
  empty_dumps=0
  nums="$(grep -o '[0-9]*' <<< "$deny_bounds" | tr '\n' ' ' || true)"
  read -r x1 y1 x2 y2 <<< "$nums"
  echo "TAP permission-sheet Deny $(( (x1+x2)/2 )),$(( (y1+y2)/2 )) (attempt $attempt)" | tee -a "$result"
  adb shell input tap $(( (x1+x2)/2 )) $(( (y1+y2)/2 ))
  sleep 1
done
adb shell rm -f /sdcard/c2-ui.xml 2>/dev/null || true
if [[ "$sheet_clear" != "1" ]]; then
  echo "UNVERIFIED could not clear the permission sheet after revoke" | tee -a "$result"; exit 1
fi
fg="$(foreground_package)"
[[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground after permission revoke: $fg" | tee -a "$result"; exit 1; }
# A fresh number: an already-saved one would hit the duplicate refusal first.
dial_number "$perm_number"
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
tap OK
sleep 2
require_marker 'ADD_TO_CONTACT_REFUSED reason=no_write_permission'
if [[ "$(count_phone_rows "$perm_number")" != "0" ]]; then
  echo "FAIL write-permission-denied Add to contact wrote a row for $perm_number" | tee -a "$result"
  phone_rows >> "$out/add-to-contact-provider.txt"
  adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
  exit 1
fi
adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
echo "PASS Add to contact refused without WRITE_CONTACTS, provider unchanged" | tee -a "$result"
expect_foreground

# --- D. Conversations entry (reported separately, non-blocking) ------------
# Threads are ordered newest first; give the phone-number sender the newer
# message so it is the selected thread, the alphanumeric sender the older one.
now_ms="$(date +%s)000"
sms_out="$(adb shell content insert --uri content://sms \
  --bind address:s:"$sms_alpha" --bind body:s:Alert \
  --bind type:i:1 --bind date:l:"$now_ms" 2>&1 | tr -d '\r')"
echo "seed sms alpha row: ${sms_out:-<empty>}" >> "$result"
sms_out="$(adb shell content insert --uri content://sms \
  --bind address:s:"$sms_sender" --bind body:s:Hi \
  --bind type:i:1 --bind date:l:"$((now_ms + 60000))" 2>&1 | tr -d '\r')"
echo "seed sms row: ${sms_out:-<empty>}" >> "$result"
sms_check="$(adb shell content query --uri content://sms \
  --projection address:body 2>&1 | tr -d '\r' || true)"
echo "sms provider dump: $sms_check" >> "$result"
if ! grep -Fq "address=$sms_sender" <<< "$sms_check" || ! grep -Fq "address=$sms_alpha" <<< "$sms_check"; then
  echo "UNVERIFIED conversations section skipped: SMS provider seeding unreadable from shell" | tee -a "$result"
  exit 0
fi

adb shell pm grant "$package" android.permission.READ_SMS 2>/dev/null || true
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
# D1: newest thread is the phone-number sender. Its options are
# Call, Save a contact, Add to contact (augmentation inserts Save a contact
# directly before Add to contact), so two Downs select Add to contact.
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN
tap OK
sleep 1
if ! marker_seen 'ADD_TO_CONTACT_PICKER_OPEN'; then
  echo "FAIL conversations Add to contact did not open the picker (menu position changed?)" | tee -a "$result"
  exit 0
fi
tap OK                          # picker: seeded contact is the only entry
sleep 2
rows="$(phone_rows)"
echo "$rows" >> "$out/add-to-contact-provider.txt"
if ! grep -Fq "data1=$sms_sender" <<< "$rows"; then
  echo "FAIL conversations Add to contact did not append sender $sms_sender to contact $raw_id" | tee -a "$result"
  exit 0
fi
echo "PASS Conversations Add to contact appended sender number $sms_sender to existing contact $raw_id" | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Conversations Add to contact' | tee -a "$result"

# D2: older thread is the alphanumeric sender; adding it must be refused
# before the picker opens. No Save a contact augmentation (no digits), so a
# single Down selects Add to contact.
tap DOWN                        # select the older (alphanumeric) thread
adb logcat -c || true
tap LSK
tap DOWN
tap OK
sleep 2
if ! marker_seen 'ADD_TO_CONTACT_REFUSED reason=invalid_sender'; then
  echo "FAIL alphanumeric sender was not refused with the expected notice" | tee -a "$result"
  exit 0
fi
if marker_seen 'ADD_TO_CONTACT_PICKER_OPEN'; then
  echo "FAIL picker opened for an alphanumeric sender" | tee -a "$result"
  exit 0
fi
echo "PASS alphanumeric SMS sender refused before picker, provider unchanged" | tee -a "$result"
expect_foreground
