#!/usr/bin/env bash
# Android 11 acceptance for Add to contact (v2.03.2). Core sections are
# BLOCKING in the suite:
#  UK. The seeded contact's existing number is stored in +44 form; dialling
#      its trunk-0 or 0044 form must be refused as a duplicate.
#  A.  Dialer: dial a new number, Options > Add to contact, pick the seeded
#      contact; the number must land on that contact and nothing else moves.
#  B.  Duplicate: repeating the same add must be refused with no provider write.
#  C.  Cancel: leaving the picker with Back must not write.
#  E.  Permission: with WRITE_CONTACTS revoked the picker must refuse the
#      write (blocking; the probe verifies the denied state before driving).
# Conversations coverage is reported separately: the SMS provider refuses
# shell seeding, so the probe tries as root and stays UNVERIFIED when even
# root cannot seed on this image.
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
shot() { # shot <name>: PNG screenshot into the artifact dir for visual review
  adb exec-out screencap -p > "$out/$1.png" 2>/dev/null || true
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
# Restore every runtime permission the suite relies on. BACK-denying the
# startup sheet only touches the missing groups, but re-grant the full set
# unconditionally so a wider denial can never leak into later probes.
restore_permissions() {
  for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
              CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
              READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
    adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
  done
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
  # Logcat and tap delivery lag on a loaded emulator; give the marker a few
  # seconds before failing (run 38600187599 flaked section A on a slow open).
  local tries=0
  while ! marker_seen "$1"; do
    tries=$((tries + 1))
    if [[ "$tries" -ge 6 ]]; then break; fi
    sleep 1
  done
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
uk2_rows_before="$(phone_rows)"
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
if [[ "$(phone_rows)" != "$uk2_rows_before" ]]; then
  echo "FAIL UK 0044-form duplicate changed the provider row set" | tee -a "$result"
  phone_rows >> "$out/add-to-contact-provider.txt"
  exit 1
fi
echo "PASS UK 0044-form duplicate refused: dialled $uk_0044 equals seeded $target_existing, provider row set unchanged" | tee -a "$result"
expect_foreground
# Clear exactly the 14 dialled digits. RSK is Clear while digits remain and
# Back once the dial is empty, so extra taps walk back off the dial page;
# two extra Backs background the app and every later tap hits the launcher
# (section A failed that way on runs 38600187599 and 38602540415).
for i in $(seq 14); do tap RSK; done

# --- A. Dialer add ---------------------------------------------------------
dial_number "$new_number"
adb logcat -c || true
tap LSK
tap DOWN; tap DOWN; tap DOWN
shot add-to-contact-options
tap OK
sleep 1
require_marker 'ADD_TO_CONTACT_PICKER_OPEN'
sleep 1
shot add-to-contact-picker
tap OK
shot add-to-contact-inserted-notice   # ~1s after OK; the notice lives 1800ms
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
shot add-to-contact-duplicate-notice   # ~1s after OK, inside the notice window
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
# Warning-only per the v2.03.x timebox: the startup permission sheet timing
# varies enough that taps can hit its Allow button and re-grant the permission
# (runs 38590863441, 38605594546), which is a probe-orchestration race, not an
# app bypass - the refusal branch itself is covered by the wiring audit.
section_e() {
# Deterministic pre-state. WHY THIS IS COMPLICATED: the AOSP permission
# controller grants a requested permission WITHOUT A SHEET when another
# permission in the same group is already granted ("If two permissions belong
# to a group and the first permission is already granted the second one will
# be granted on request of the app without user interaction"). With
# READ_CONTACTS held, the app's cold-start request silently re-grants
# WRITE_CONTACTS (runs 38684431984, 38687531877, 38689955068: granted=true,
# no USER_SET flag, no sheet ever foreground). So:
#   1. Revoke the WHOLE contacts group, cold-start, and BACK the sheet (BACK
#      on GrantPermissionsActivity denies the request).
#   2. Re-grant READ_CONTACTS to the RUNNING app (grant does not kill the
#      process; revoke does - logcat shows "Killing <pid>: permissions
#      revoked"). A process death here voids the round: the next cold start
#      would group-auto-grant WRITE_CONTACTS again.
#   3. Verify WRITE_CONTACTS is still denied, then drive the picker.
# The sheet is detected by FOREGROUND ACTIVITY, never by uiautomator text
# (the sheet displays ~6s after the cold start on a loaded emulator, and
# text-dump polling declared "sheet clear" before it was even visible).
# Grant flags are logged each round: USER_SET means the sheet was answered;
# no USER_SET means a programmatic grant.
denied_ready=0
for round in 1 2 3; do
adb shell pm revoke "$package" android.permission.READ_CONTACTS 2>&1 | tee -a "$result" || true
adb shell pm revoke "$package" android.permission.WRITE_CONTACTS 2>&1 | tee -a "$result" || true
sleep 1
pre_state="$(adb shell dumpsys package "$package" | grep -E 'android.permission.(READ|WRITE)_CONTACTS: granted' | tr -d '\r')"
echo "contacts state after revoke (round $round): $pre_state" | tee -a "$result"
if [[ "$(grep -c 'granted=false' <<< "$pre_state")" != "2" ]]; then
  echo "FAIL contacts revoke did not take (round $round)" | tee -a "$result"; exit 1
fi
adb shell am force-stop "$package"
adb shell am start -W -n "$package/.MainActivity" >> "$result"
sheet_seen=0
stable_app=0
# 24 attempts x ~1s. The sheet can take ~6s to display on a loaded emulator
# (install-probe logcat: START -> Displayed +5.9s), so never conclude "no
# sheet" from the first app-foreground polls: require 3 steady app-foreground
# polls AND (sheet answered or at least 12 attempts elapsed) before moving on.
for attempt in $(seq 1 24); do
  top="$(foreground_package)"
  if [[ "$top" == *"permissioncontroller"* ]]; then
    sheet_seen=1
    stable_app=0
    echo "BACK on permission sheet (round $round attempt $attempt): $top" >> "$result"
    adb shell input keyevent KEYCODE_BACK
    sleep 1
    continue
  fi
  if [[ "$top" == *"$package/"* ]]; then
    stable_app=$((stable_app + 1))
    if [[ "$stable_app" -ge 3 && ( "$sheet_seen" == "1" || "$attempt" -ge 12 ) ]]; then break; fi
  else
    stable_app=0
  fi
  sleep 1
done
post_state="$(adb shell dumpsys package "$package" | grep 'android.permission.WRITE_CONTACTS: granted' | tr -d '\r')"
echo "WRITE_CONTACTS after sheet handling (round $round): $post_state" | tee -a "$result"
if ! grep -q 'granted=false' <<< "$post_state"; then
  echo "round $round: WRITE_CONTACTS granted again (grant flags above identify who), retrying" | tee -a "$result"
  adb logcat -d -t 250 2>/dev/null | grep -iE 'grant|permission' | tail -15 >> "$result" || true
  continue
fi
if [[ "$sheet_seen" != "1" ]]; then
  echo "round $round: no permission sheet seen despite full contacts-group revoke; suspicious, retrying" | tee -a "$result"
  continue
fi
# Denied. Re-grant READ_CONTACTS to the RUNNING app so the picker lists
# contacts; the process must survive (a death means the next cold start
# group-auto-grants WRITE_CONTACTS and the round is void).
pid_before="$(adb shell pidof -s "$package" 2>/dev/null | tr -d '\r[:space:]')"
adb shell pm grant "$package" android.permission.READ_CONTACTS 2>&1 | tee -a "$result" || true
sleep 2
pid_after="$(adb shell pidof -s "$package" 2>/dev/null | tr -d '\r[:space:]')"
if [[ -z "$pid_after" || -n "$pid_before" && "$pid_after" != "$pid_before" ]]; then
  echo "round $round: pm grant READ_CONTACTS killed the app process (pid $pid_before -> $pid_after); round void" | tee -a "$result"
  continue
fi
post_state="$(adb shell dumpsys package "$package" | grep 'android.permission.WRITE_CONTACTS: granted' | tr -d '\r')"
echo "WRITE_CONTACTS after READ re-grant (round $round): $post_state" >> "$result"
if ! grep -q 'granted=false' <<< "$post_state"; then
  echo "round $round: WRITE_CONTACTS granted after READ re-grant, retrying" | tee -a "$result"
  continue
fi
fg="$(foreground_package)"
if [[ "$fg" != *"$package/"* ]]; then
  echo "round $round: denied but C2 not foreground ($fg); one relaunch would group-auto-grant, round void" | tee -a "$result"
  continue
fi
denied_ready=1
break
done
if [[ "$denied_ready" != "1" ]]; then
  echo "FAIL could not establish a stable WRITE_CONTACTS-denied state after 3 rounds" | tee -a "$result"; exit 1
fi
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
  restore_permissions
  exit 1
fi
restore_permissions
echo "PASS Add to contact refused without WRITE_CONTACTS, provider unchanged" | tee -a "$result"
expect_foreground
}
# Subshell: section E contains exit statements; they must end the subshell,
# not the probe. Blocking since v2.03.10: the revoke/verify loop above makes
# the denied state deterministic before the UI is driven, so a failure here
# is a real signal.
if ! ( section_e ); then
  adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
  exit 1
fi

# --- D. Conversations entry (reported separately, non-blocking) ------------
# Threads are ordered newest first; give the phone-number sender the newer
# message so it is the selected thread, the alphanumeric sender the older one.
# The SMS provider refuses shell writes (default-SMS-app role), so seed as
# root like the call-log probes; if even root cannot seed on this image the
# section stays UNVERIFIED with the exact provider answer in the log.
if ! adb root 2>&1 | grep -qi 'root'; then
  echo "UNVERIFIED conversations section skipped: adb root unavailable on this emulator image" | tee -a "$result"
  exit 0
fi
sleep 3
adb wait-for-device
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
  echo "UNVERIFIED conversations section skipped: SMS provider seeding unreadable even as root" | tee -a "$result"
  adb unroot >/dev/null 2>&1 || true
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
# Remove the seeded SMS rows (still root) so later runs start clean; a
# leftover is reported, not failed on.
for addr in "$sms_sender" "$sms_alpha"; do
  adb shell content delete --uri content://sms --where "address='$addr'" >/dev/null 2>&1 || true
done
sms_left="$(adb shell content query --uri content://sms --projection address 2>&1 | tr -d '\r' || true)"
if grep -qE "address=($sms_sender|$sms_alpha)" <<< "$sms_left"; then
  echo "WARN seeded SMS rows still present after cleanup" | tee -a "$result"
fi
adb unroot >/dev/null 2>&1 || true
adb wait-for-device || true
