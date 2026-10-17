#!/usr/bin/env bash
# Android 11 acceptance for Contacts > Del. all contacts. BLOCKING in the
# suite:
#  Y. Yes:  with two seeded contacts present, Del. all contacts > Yes must
#     remove every provider contact and report the exact deleted count.
#  Y-add/Y-remove: a contact appearing or vanishing between the
#     confirm and Yes refuses the delete (audit #34 item B1). The Y
#     confirm screenshot must show the full native wording incl. the
#     synced-account consequence line (audit #35 item 1).
#  E. Empty: with the provider empty, Del. all contacts is refused with the
#     "Selected memory empty" notice (HTML v6.343 wording); nothing changes.
#  N. No:   answering No on the confirm leaves the provider byte-identical.
# The confirm is the in-house C2 question box (Yes/No softkeys); the wording
# follows the HTML v6.343 bulk template ("Move contacts?" + "N contacts
# selected") because the prototype ships no delete-all confirm of its own.
# Verification reads the Contacts provider directly and the app's C2Contacts
# logcat markers. Emulator only - this deletes every contact it can see, so
# never run it against a handset.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/delete-all-contacts-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
del_one="AAAC2DelOne"
del_two="AAAC2DelTwo"
num_one="07900005001"
num_two="07900005002"

# Hard guard (audit-#14 item 5): this probe deletes every provider contact.
# Refuse to run anywhere but an emulator.
qemu="$(adb shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r'; adb shell getprop ro.boot.qemu 2>/dev/null | tr -d '\r')"
if ! grep -q 1 <<< "$qemu"; then
  echo "REFUSING: delete-all-contacts probe runs on emulators only (ro.kernel.qemu/ro.boot.qemu unset)" | tee -a "$result"
  exit 1
fi

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
  adb logcat -d -v brief -s C2Contacts:I AndroidRuntime:E '*:S' > "$out/delete-all-contacts-log.txt" || true
  adb shell screencap -p /sdcard/c2-delall.png 2>/dev/null || true
  adb pull /sdcard/c2-delall.png "$out/delete-all-contacts-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-delall.png 2>/dev/null || true
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
marker_seen() { # marker_seen <text>: 0 when the app logged it since last clear
  adb logcat -d -s C2Contacts:I 2>/dev/null | grep -F "$1" >/dev/null
}
require_marker() {
  # Logcat and tap delivery lag on a loaded emulator; give the marker a few
  # seconds before failing.
  local tries=0
  while ! marker_seen "$1"; do
    tries=$((tries + 1))
    if [[ "$tries" -ge 6 ]]; then break; fi
    sleep 1
  done
  if ! marker_seen "$1"; then
    echo "FAIL expected app log marker: $1" | tee -a "$result"
    echo "--- C2Contacts logcat at failure ---" | tee -a "$result"
    adb logcat -d -s C2Contacts:I 2>/dev/null | tee -a "$result" > "$out/delete-all-contacts-section-log.txt" || true
    exit 1
  fi
}
raw_ids() { # one LIVE raw-contact _id per line, sorted; empty when provider empty.
  # Synced raws are soft-deleted (deleted=1) so the account can propagate the
  # removal; the user-facing promise is that no live contacts remain.
  # Fail closed (audit #37): an adb/provider error must not read as an empty
  # list - the No before/after comparison would pass on two failed reads.
  local out
  if ! out="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
      --projection _id --where "deleted=0" 2>&1)"; then
    echo "FAIL raw_ids query errored: $out" | tee -a "$result"
    exit 1
  fi
  if grep -qiE 'error|exception|security' <<< "$out"; then
    echo "FAIL raw_ids query returned an error: $out" | tee -a "$result"
    exit 1
  fi
  printf '%s\n' "$out" | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true
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
  # All values are single tokens: adb shell re-joins arguments and the remote
  # tokenizer splits on spaces, so a spaced name would fail as an argument.
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"$name" >/dev/null 2>&1 || true
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
    --bind data1:s:"$number" --bind data2:i:2 >/dev/null 2>&1 || true
  if ! adb shell content query --uri content://com.android.contacts/data \
      --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -Fq "$number"; then
    echo "UNVERIFIED seeded contact $name unreadable in provider (raw id $new_id)" | tee -a "$result"
    exit 1
  fi
  echo "$new_id"
}

# Grant the full runtime set up front so no cold-start permission sheet can
# intercept the first taps (same set as the other contacts probes).
for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
            READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# From the home display: CENTRE opens the menu grid (Contacts is index 0),
# OK enters it, seven Downs select "Del. all contacts".
open_delete_all() {
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
  fg="$(foreground_package)"
  [[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $fg" | tee -a "$result"; exit 1; }
  tap OK                          # home menu grid (selection starts at index 0)
  tap OK                          # grid index 0: Contacts
  tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
}

seed_numberless() { # seed_numberless <name>: a contact with a name but NO phone row
  local name="$1" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id")" != "1" ]]; then
    echo "UNVERIFIED seed of number-less $name did not add exactly one raw contact" | tee -a "$result"
    exit 1
  fi
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"$name" >/dev/null 2>&1 || true
  # The seeded contact must have NO phone row: that is the case under test
  # (audit #14 item 1).
  if adb shell content query --uri content://com.android.contacts/data \
      --projection raw_contact_id:mimetype 2>&1 | tr -d '\r' \
      | grep -F "raw_contact_id=$new_id" | grep -Fq "phone_v2"; then
    echo "UNVERIFIED number-less seed $name unexpectedly has a phone row" | tee -a "$result"
    exit 1
  fi
  echo "$new_id"
}

seed_synced_contact() { # seed_synced_contact <name> <number>: raw contact bound to an account
  local name="$1" number="$2" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind account_name:s:probe@example.com --bind account_type:s:com.example.sync \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id")" != "1" ]]; then
    echo "UNVERIFIED synced seed of $name did not add exactly one raw contact" | tee -a "$result"
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
  if ! adb shell content query --uri content://com.android.contacts/raw_contacts \
      --projection _id:account_type 2>&1 | tr -d '\r' \
      | grep -F "_id=$new_id" | grep -Fq 'com.example.sync'; then
    echo "UNVERIFIED synced seed $name lost its account_type in the provider" | tee -a "$result"
    exit 1
  fi
  echo "$new_id"
}

# --- Y. Yes removes every provider contact ---------------------------------
baseline_ids="$(raw_ids)"
baseline_count="$(grep -c . <<< "$baseline_ids" || true)"
[[ -n "$baseline_ids" ]] || baseline_count=0
echo "provider baseline raw contacts: $baseline_count" >> "$result"
seed_contact "$del_one" "$num_one" >/dev/null
seed_contact "$del_two" "$num_two" >/dev/null
seed_numberless "AAAC2DelThree" >/dev/null
seed_synced_contact "AAAC2DelSync" "07900005003" >/dev/null
expected=$((baseline_count + 4))
echo "Seeded $del_one, $del_two, number-less AAAC2DelThree and synced AAAC2DelSync; expecting $expected aggregates counted by Del. all contacts" >> "$result"

open_delete_all
adb logcat -c || true
tap OK                          # "Del. all contacts"
sleep 1
require_marker "DELETE_ALL_CONTACTS_CONFIRM count=$expected"
# This screenshot is the audit #35 item 1 proof: the confirm box must show
# the full wording including the synced-account consequence line. Its
# existence is a gate (audit #36 item 3); a human/agent view of the PNG
# confirms the "may be removed" line is visible.
shot delall-confirm-yes
if [[ ! -s "$out/delall-confirm-yes.png" ]]; then
  echo "FAIL delall-confirm-yes.png missing or empty; consequence-line proof unavailable" | tee -a "$result"
  exit 1
fi

# Y-add (audit #34 item B1): a contact that APPEARS between the confirm and
# Yes must refuse the delete, not remove a contact the user never saw
# counted. The seed goes straight into the provider while the confirm is
# open, so the pinned membership no longer matches at Yes.
four_id="$(seed_contact AAAC2DelFour 07900005004)"
adb logcat -c || true
tap LSK                         # Yes
require_marker "DELETE_ALL_CONTACTS_REFUSED reason=changed"
leftover="$(raw_ids)"
leftover_count="$(grep -c . <<< "$leftover" || true)"
[[ -n "$leftover" ]] || leftover_count=0
if [[ "$leftover_count" -ne $((expected + 1)) ]]; then
  echo "FAIL changed-refuse path deleted contacts anyway (left $leftover_count, expected $((expected + 1)))" | tee -a "$result"
  exit 1
fi
echo 'PASS Delete all contacts Yes refused after a contact appeared between confirm and Yes' | tee -a "$result"
shot delall-add-refused
sleep 2                         # let the refusal notice auto-dismiss (1800ms)

# Y-remove (audit #34 item B1): a contact that VANISHES between the confirm
# and Yes must refuse too. The Yes handler reset selection to the top row.
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
require_marker "DELETE_ALL_CONTACTS_CONFIRM count=$((expected + 1))"
adb shell content delete --uri content://com.android.contacts/raw_contacts \
  --where "_id=$four_id" >/dev/null 2>&1 || true
sleep 1
adb logcat -c || true
tap LSK                         # Yes
require_marker "DELETE_ALL_CONTACTS_REFUSED reason=changed"
leftover="$(raw_ids)"
leftover_count="$(grep -c . <<< "$leftover" || true)"
[[ -n "$leftover" ]] || leftover_count=0
if [[ "$leftover_count" -ne $expected ]]; then
  echo "FAIL vanished-refuse path deleted contacts anyway (left $leftover_count, expected $expected)" | tee -a "$result"
  exit 1
fi
echo 'PASS Delete all contacts Yes refused after a contact vanished between confirm and Yes' | tee -a "$result"
shot delall-remove-refused
sleep 2                         # let the refusal notice auto-dismiss

# Y-real: with membership unchanged since the confirm, Yes deletes exactly
# the pinned aggregate set.
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK
sleep 1
require_marker "DELETE_ALL_CONTACTS_CONFIRM count=$expected"
adb logcat -c || true
tap LSK                         # Yes
sleep 2
require_marker "DELETE_ALL_CONTACTS deleted=$expected failed=0"
leftover="$(raw_ids)"
if [[ -n "$leftover" ]]; then
  echo "FAIL Delete all contacts Yes left raw contacts: $leftover" | tee -a "$result"
  exit 1
fi
echo "PASS Delete all contacts Yes removed every provider contact ($expected deleted)" | tee -a "$result"
echo 'PASS Delete all contacts count and delete included a contact without a phone number' | tee -a "$result"
# The synced seed was soft-deleted (deleted=1), which is the correct Android
# sync-delete behavior the confirm discloses. Hard-remove it so the next
# probes in the suite see a byte-clean provider.
adb shell content delete \
  --uri "'content://com.android.contacts/raw_contacts?caller_is_syncadapter=true'" \
  --where "'deleted=1'" >/dev/null 2>&1 || true
sleep 1
# Fail closed (audit #36 item 2): a query error must not read as clean.
if ! tomb_out="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
    --projection _id 2>&1)"; then
  echo "FAIL tombstone check query errored: $tomb_out" | tee -a "$result"
  exit 1
fi
if grep -qiE 'error|exception|security' <<< "$tomb_out"; then
  echo "FAIL tombstone check query returned an error: $tomb_out" | tee -a "$result"
  exit 1
fi
tombstones="$(printf '%s' "$tomb_out" | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true)"
if [[ -n "$tombstones" ]]; then
  echo "FAIL soft-deleted raw contacts survived cleanup: $tombstones" | tee -a "$result"
  exit 1
fi
expect_foreground
echo 'PASS C2 remains foreground after Delete all contacts Yes' | tee -a "$result"
shot delall-after-yes
sleep 2                         # let the result notice auto-dismiss (1800ms)

# --- E. Empty provider is refused ------------------------------------------
# Still on the Contacts menu; the Yes handler reset selection to the top row.
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
adb logcat -c || true
tap OK
sleep 1
require_marker 'DELETE_ALL_CONTACTS_REFUSED reason=empty'
leftover="$(raw_ids)"
[[ -z "$leftover" ]] || { echo "FAIL empty-refuse path changed the provider" | tee -a "$result"; exit 1; }
echo 'PASS Delete all contacts refused empty provider with Selected memory empty' | tee -a "$result"
shot delall-empty-refused

# --- N. No leaves the provider byte-identical ------------------------------
seed_contact "$del_one" "$num_one" >/dev/null
seed_contact "$del_two" "$num_two" >/dev/null
before_no="$(raw_ids)"
open_delete_all
adb logcat -c || true
tap OK
sleep 1
require_marker 'DELETE_ALL_CONTACTS_CONFIRM count=2'
shot delall-confirm-no
tap RSK                         # No
sleep 2
after_no="$(raw_ids)"
if [[ "$after_no" != "$before_no" ]]; then
  echo "FAIL Delete all contacts No changed the provider (before: $before_no / after: $after_no)" | tee -a "$result"
  exit 1
fi
echo 'PASS Delete all contacts No left provider unchanged' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Delete all contacts No' | tee -a "$result"
shot delall-after-no

# Mirror key screenshots into the results as single-line base64 so the job
# log (the only reliable retrieval path) carries them for visual review.
for f in delall-confirm-yes delall-add-refused delall-remove-refused delall-after-yes delall-confirm-no delall-empty-refused delall-after-no; do
  if [[ -s "$out/$f.png" ]]; then
    echo "SHOTB64 $f $(base64 -w0 "$out/$f.png")" >> "$result"
  fi
done
echo 'PASS Delete all contacts acceptance complete' | tee -a "$result"
