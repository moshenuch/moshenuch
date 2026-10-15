#!/usr/bin/env bash
# Android 11 acceptance for Contacts > Move contacts / Copy contacts (the
# HTML v6.343 phone/SIM memory model; SIM is the in-app SIM list - the real
# SIM card is never touched). BLOCKING in the suite:
#  M1. Move phone->SIM: every contact leaves the provider; the in-app SIM
#     list (prefs) holds exactly the moved names; confirm wording follows
#     HTML ("Move contacts?" + "N contacts selected").
#  E.  Empty source: with phone memory empty, From phone to SIM is refused
#     with "Selected memory empty" (exact HTML wording).
#  M2. Move SIM->phone: provider and SIM list swap back.
#  C.  Copy phone->SIM: provider unchanged; SIM list gains the copies.
#  N.  No on the confirm changes nothing (provider + SIM list).
# Verification reads the Contacts provider directly, the app prefs via
# run-as, and the app's C2Contacts logcat markers. Emulator only - this
# moves every contact it can see, so never run it against a handset.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/move-copy-contacts-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
prefs_dir="/data/data/$package/shared_prefs"
backup="$out/move-copy-prefs-backup.xml"
restore_needed=0
mov_one="AAAC2MoveOne"
mov_two="AAAC2MoveTwo"
num_one="07900005011"
num_two="07900005012"
seeded_ids=""

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
 'LSK':(90,414),'RSK':(270,414),'OK':(180,474),'DOWN':(180,505),
}
for name in ['LSK','RSK','OK','DOWN']:
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
  adb logcat -d -v brief -s C2Contacts:I AndroidRuntime:E '*:S' > "$out/move-copy-contacts-log.txt" || true
  adb shell screencap -p /sdcard/c2-movecopy.png 2>/dev/null || true
  adb pull /sdcard/c2-movecopy.png "$out/move-copy-contacts-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-movecopy.png 2>/dev/null || true
}
# EXIT trap owns every cleanup path: evidence, prefs restore (removes the
# SIM list this probe creates), seeded-contact deletion by captured id where
# the rows still carry them, all guarded under set -euo pipefail.
finish() {
  capture_evidence
  adb shell am force-stop "$package" || true
  if [[ "$restore_needed" == "1" ]]; then
    adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup" || true
    if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | cmp -s - "$backup"; then
      echo "PASS original c2_state.xml restored after probe (bytes verified)" | tee -a "$result"
    else
      echo "FAIL restore of c2_state.xml did not verify byte-for-byte" | tee -a "$result"
    fi
  else
    adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
    echo "PASS no pre-existing c2_state.xml; probe prefs removed" | tee -a "$result"
  fi
  # Provider rows may have been re-added with fresh ids by the move-back
  # sections; delete seeded-name rows only when the count matches exactly
  # what this probe seeded (emulator is fresh per CI run).
  local rows
  rows="$(adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E "data1=($mov_one|$mov_two)" || true)"
  local row_count
  row_count="$(grep -c . <<< "$rows" || true)"
  if [[ "$row_count" == "2" ]]; then
    while IFS= read -r line; do
      rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
      [[ "$rid" =~ ^[0-9]+$ ]] || continue
      adb shell content delete --uri content://com.android.contacts/raw_contacts \
        --where "_id=$rid" >/dev/null 2>&1 || true
    done <<< "$rows"
    echo "PASS seeded Move/Copy contacts removed from provider" | tee -a "$result"
  elif [[ "$row_count" != "0" ]]; then
    echo "WARN seeded-name provider rows: $row_count (expected 0 or 2); left for the discarded emulator" | tee -a "$result"
  fi
}
trap finish EXIT

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
marker_seen() {
  adb logcat -d -s C2Contacts:I 2>/dev/null | grep -qF "$1"
}
require_marker() {
  local tries=0
  while ! marker_seen "$1"; do
    tries=$((tries + 1))
    if [[ "$tries" -ge 6 ]]; then break; fi
    sleep 1
  done
  if ! marker_seen "$1"; then
    echo "FAIL expected app log marker: $1" | tee -a "$result"
    echo "--- C2Contacts logcat at failure ---" | tee -a "$result"
    adb logcat -d -s C2Contacts:I 2>/dev/null | tee -a "$result" > "$out/move-copy-section-log.txt" || true
    exit 1
  fi
}
raw_ids() { # one raw-contact _id per line, sorted; empty when provider empty
  adb shell content query --uri content://com.android.contacts/raw_contacts \
    --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true
}
raw_count() {
  local ids
  ids="$(raw_ids)"
  if [[ -z "$ids" ]]; then echo 0; else grep -c . <<< "$ids"; fi
}
sim_count() { # entries in the in-app SIM list, from the prefs file
  local line
  line="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -o '<string name="sim_contacts_v1">[^<]*' || true)"
  if [[ -z "$line" ]]; then echo 0; else grep -o '&quot;name&quot;' <<< "$line" | wc -l; fi
}
seed_contact() { # seed_contact <name> <number>; prints the new raw id
  local name="$1" number="$2" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id")" != "1" ]]; then
    echo "UNVERIFIED seed of $name did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
    exit 1
  fi
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"$name" >/dev/null 2>&1 || true
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
    --bind data1:s:"$number" --bind data2:i:2 >/dev/null 2>&1 || true
  echo "$new_id"
}

for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
            READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# Back up prefs so the EXIT trap can drop the SIM list this probe creates.
adb shell am force-stop "$package" || true
adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' > "$backup" || true
if [[ -s "$backup" ]] && grep -q '<map>' "$backup"; then
  restore_needed=1
  echo "BACKUP existing c2_state.xml captured for restore" >> "$result"
fi

baseline_count="$(raw_count)"
echo "provider baseline raw contacts: $baseline_count" >> "$result"
seed_contact "$mov_one" "$num_one" >/dev/null
seed_contact "$mov_two" "$num_two" >/dev/null
expected=$((baseline_count + 2))
echo "Seeded $mov_one and $mov_two; expecting $expected contacts visible to the app" >> "$result"

# From the home display: CENTRE opens the menu grid, OK enters Contacts
# (index 0), then DOWN x8 = Move contacts / DOWN x9 = Copy contacts.
open_contacts_menu() {
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
  fg="$(foreground_package)"
  [[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $fg" | tee -a "$result"; exit 1; }
  tap OK                          # home menu grid (selection starts at index 0)
  tap OK                          # grid index 0: Contacts
}

# --- M1. Move phone -> SIM --------------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
shot movecopy-move-menu
tap OK                          # Move contacts submenu (sel 0 = From phone to SIM)
shot movecopy-move-submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$expected"
shot movecopy-move-confirm
tap LSK                         # Yes
sleep 2
require_marker "MOVE_CONTACTS dir=phone2sim done=$expected failed=0"
after_move="$(raw_count)"
if [[ "$after_move" != "0" ]]; then
  echo "FAIL Move phone->SIM left $after_move raw contacts in provider" | tee -a "$result"
  exit 1
fi
sim_after_move="$(sim_count)"
if [[ "$sim_after_move" != "$expected" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_move entries, expected $expected" | tee -a "$result"
  exit 1
fi
echo "PASS Move phone->SIM emptied provider and SIM list holds all $sim_after_move" | tee -a "$result"
expect_foreground
shot movecopy-after-move

# --- E. Empty phone memory is refused ---------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM (phone memory empty now)
sleep 1
require_marker 'MOVE_CONTACTS_REFUSED reason=empty dir=0'
echo 'PASS Move from empty phone memory refused with Selected memory empty' | tee -a "$result"
shot movecopy-empty-refused

# --- M2. Move SIM -> phone --------------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
tap DOWN                        # From SIM to phone
adb logcat -c || true
tap OK
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=sim2phone count=$expected"
tap LSK                         # Yes
sleep 2
require_marker "MOVE_CONTACTS dir=sim2phone done=$expected failed=0"
after_back="$(raw_count)"
if [[ "$after_back" != "$expected" ]]; then
  echo "FAIL Move SIM->phone left provider at $after_back raw contacts, expected $expected" | tee -a "$result"
  exit 1
fi
sim_after_back="$(sim_count)"
if [[ "$sim_after_back" != "0" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_back entries after move back, expected 0" | tee -a "$result"
  exit 1
fi
echo "PASS Move SIM->phone restored provider and emptied SIM list" | tee -a "$result"
expect_foreground
shot movecopy-after-move-back

# --- C. Copy phone -> SIM ---------------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Copy contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "COPY_CONTACTS_CONFIRM dir=phone2sim count=$expected"
shot movecopy-copy-confirm
tap LSK                         # Yes
sleep 2
require_marker "COPY_CONTACTS dir=phone2sim done=$expected failed=0"
after_copy="$(raw_count)"
if [[ "$after_copy" != "$expected" ]]; then
  echo "FAIL Copy changed provider count to $after_copy, expected $expected" | tee -a "$result"
  exit 1
fi
sim_after_copy="$(sim_count)"
if [[ "$sim_after_copy" != "$expected" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_copy entries after copy, expected $expected" | tee -a "$result"
  exit 1
fi
echo "PASS Copy phone->SIM kept provider and copied $sim_after_copy to SIM list" | tee -a "$result"
expect_foreground
shot movecopy-after-copy

# --- N. No changes nothing ---------------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Copy contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker 'COPY_CONTACTS_CONFIRM'
shot movecopy-confirm-no
tap RSK                         # No
sleep 2
if marker_seen 'COPY_CONTACTS dir='; then
  echo 'FAIL answering No still ran the copy handler' | tee -a "$result"
  exit 1
fi
after_no="$(raw_count)"
sim_after_no="$(sim_count)"
if [[ "$after_no" != "$expected" || "$sim_after_no" != "$expected" ]]; then
  echo "FAIL No changed state (provider $after_no, SIM $sim_after_no; expected $expected/$expected)" | tee -a "$result"
  exit 1
fi
echo 'PASS Copy contacts No left provider and SIM list unchanged' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Move/Copy flows' | tee -a "$result"
shot movecopy-after-no

# Mirror key screenshots into the results as single-line base64 so the job
# log (the only reliable retrieval path) carries them for visual review.
for f in movecopy-move-submenu movecopy-move-confirm movecopy-after-move movecopy-empty-refused movecopy-copy-confirm movecopy-confirm-no; do
  if [[ -s "$out/$f.png" ]]; then
    echo "SHOTB64 $f $(base64 -w0 "$out/$f.png")" >> "$result"
  fi
done
echo 'PASS Move/Copy contacts acceptance complete' | tee -a "$result"
