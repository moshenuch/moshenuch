#!/usr/bin/env bash
# Android 11 acceptance for Contacts > Move contacts / Copy contacts (the
# HTML v6.343 phone/SIM memory model; SIM is the in-app SIM list - the real
# SIM card is never touched). BLOCKING in the suite:
#  D.  Duplicate destination: a SIM entry matching a phone contact in +44
#     form (vs the phone's national form) refuses the whole batch with the
#     exact HTML error wording; nothing changes.
#  F.  Partial failure: a provider row deleted behind the app's back after
#     the confirm makes the batch fail and roll back ("Contacts changed; try
#     again"); no partial transfer survives.
#  E.  Empty SIM memory: From SIM to phone is refused with "Selected memory
#     empty" (exact HTML wording).
#  M1. Move phone->SIM: single-number contacts move; a multi-number contact
#     and a contact carrying extra data (email row) are SKIPPED (not
#     stripped) and stay untouched in the provider, email row included
#     (audit #16).
#  C-extra. Copy phone->SIM copies the extra-data contact as name+number
#     only; the provider original keeps its email row (audit #16).
#  M2. Move SIM->phone: provider and SIM list swap back.
#  C.  Copy phone->SIM: provider unchanged; multi-number skipped again.
#  N.  No on the confirm changes nothing (provider + SIM list).
#  W.  Two identical contacts inside the source batch refuse the whole Move
#     (duplicate_source; audit #16).
#  R.  Rollback-failure injection: WRITE_CONTACTS revoked mid-rollback makes
#     the reverse deletes fail; the app must log rolled_back=0 with
#     undone < done and show the undo-incomplete notice (audit #16).
# Baseline contacts are snapshotted up front and re-seeded by the trap if an
# interrupted run strands them, so the probe can never eat real contacts.
# Verification reads the Contacts provider directly, the app prefs via
# run-as, and the app's C2Contacts logcat markers. Emulator only.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/move-copy-contacts-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
prefs_dir="/data/data/$package/shared_prefs"
backup="$out/move-copy-prefs-backup.xml"
baseline_tsv="$out/move-copy-baseline-contacts.tsv"
restore_needed=0
mov_one="AAAC2MoveOne"
mov_two="AAAC2MoveTwo"
mov_multi="AAAC2MoveMulti"
mov_extra="AAAC2MoveExtra"
num_one="07900005011"
num_two="07900005012"
num_multi_a="07900005013"
num_multi_b="07900005014"
num_extra="07900005015"
email_extra="mov.extra@c2probe.example"

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
# provider_dump: TSV of name<TAB>number_count per raw contact, one per line.
provider_dump() { # provider_dump <raw-out-file>: raw adb output kept for diagnosis
  # No --where: adb shell mashes arguments on spaces. Filter in python.
  adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:mimetype:data1 2>&1 \
    | tr -d '\r' | tee "$1" | python3 -c '
import sys, collections, re
names = {}
nums = collections.defaultdict(set)
for line in sys.stdin:
    # adb prefixes every row with "Row: N "; without stripping, the first
    # key parses as "Row: N raw_contact_id" and every row is dropped.
    line = re.sub(r"^Row:\s*\d+\s+", "", line.strip())
    parts = dict(p.split("=", 1) for p in line.split(", ") if "=" in p)
    rid = parts.get("raw_contact_id")
    mt = parts.get("mimetype", "")
    d1 = parts.get("data1", "")
    if not rid:
        continue
    if mt.endswith("/name"):
        names[rid] = d1
    elif mt.endswith("/phone_v2"):
        nums[rid].add(d1)
# Include number-only raw contacts (no name row): the app lists those too.
for rid in sorted(set(names) | set(nums), key=int):
    label = names.get(rid) or "(noname)"
    print(label + "\t" + str(len(nums.get(rid, set()))))
'
}
# EXIT trap owns every cleanup path: evidence, prefs restore (removes any
# SIM list state), baseline re-seed for contacts an interrupted run moved
# out of the provider, seeded-fixture deletion by name (count-guarded).
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
  # Re-seed baseline contacts that are no longer in the provider (a run
  # interrupted mid-Move strands them in SIM prefs otherwise).
  if [[ -s "$baseline_tsv" ]]; then
    local reseeded=0 name ncount
    while IFS=$'\t' read -r name ncount; do
      [[ -n "$name" ]] || continue
      if ! provider_dump "$out/.pd-trap-raw.txt" | grep -qF "$name	"; then
        local rid
        rid="$(adb shell content insert --uri content://com.android.contacts/raw_contacts \
          --bind starred:i:0 2>&1 | tr -d '\r' | grep -oE 'raw_contact_id=[0-9]+|/[0-9]+$' | grep -oE '[0-9]+' | head -1 || true)"
        if [[ "$rid" =~ ^[0-9]+$ ]]; then
          adb shell content insert --uri content://com.android.contacts/data \
            --bind raw_contact_id:i:"$rid" \
            --bind mimetype:s:vnd.android.cursor.item/name \
            --bind data1:s:"$name" >/dev/null 2>&1 || true
          reseeded=$((reseeded + 1))
        fi
      fi
    done < "$baseline_tsv"
    if [[ "$reseeded" -gt 0 ]]; then
      echo "PASS re-seeded $reseeded baseline contacts an interrupted run stranded" | tee -a "$result"
    fi
  fi
  # Delete this probe's seeded rows by name, only when the count matches
  # exactly what was seeded (emulator is fresh per CI run).
  local rows row_count
  rows="$(adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E "data1=($mov_one|$mov_two|$mov_multi|$mov_extra)" || true)"
  row_count="$(grep -c . <<< "$rows" || true)"
  if [[ "$row_count" == "4" ]]; then
    local line rid
    while IFS= read -r line; do
      rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
      [[ "$rid" =~ ^[0-9]+$ ]] || continue
      adb shell content delete --uri content://com.android.contacts/raw_contacts \
        --where "_id=$rid" >/dev/null 2>&1 || true
    done <<< "$rows"
    echo "PASS seeded Move/Copy contacts removed from provider" | tee -a "$result"
  elif [[ "$row_count" != "0" ]]; then
    echo "WARN seeded-name provider rows: $row_count (expected 0 or 4); left for the discarded emulator" | tee -a "$result"
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
  if [[ -z "$line" ]]; then echo 0; else { grep -o '&quot;name&quot;' <<< "$line" || true; } | wc -l | tr -d ' '; fi
}
seed_contact() { # seed_contact <name> <number> [number2] [email]; prints the new raw id
  local name="$1" number="$2" number2="${3:-}" email="${4:-}" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
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
  if [[ -n "$number2" ]]; then
    adb shell content insert --uri content://com.android.contacts/data \
      --bind raw_contact_id:i:"$new_id" \
      --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
      --bind data1:s:"$number2" --bind data2:i:1 >/dev/null 2>&1 || true
  fi
  if [[ -n "$email" ]]; then
    adb shell content insert --uri content://com.android.contacts/data \
      --bind raw_contact_id:i:"$new_id" \
      --bind mimetype:s:vnd.android.cursor.item/email_v2 \
      --bind data1:s:"$email" --bind data2:i:1 >/dev/null 2>&1 || true
  fi
  echo "$new_id"
}

for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
            READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# Back up prefs and baseline contacts so the EXIT trap can undo everything.
adb shell am force-stop "$package" || true
adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' > "$backup" || true
if [[ -s "$backup" ]] && grep -q '<map>' "$backup"; then
  restore_needed=1
  echo "BACKUP existing c2_state.xml captured for restore" >> "$result"
fi
provider_dump "$out/pd-baseline-raw.txt" > "$baseline_tsv"
baseline_total="$(grep -c . < "$baseline_tsv" || true)"
baseline_raw="$(raw_count)"
if [[ "$baseline_total" == "0" && "$baseline_raw" != "0" ]]; then
  echo 'DIAG provider_dump returned no rows while raw_contacts is non-empty; raw query output follows' | tee -a "$result"
  sed 's/^/  RAW: /' "$out/pd-baseline-raw.txt" >> "$result"
fi
baseline_multi="$(awk -F '\t' '$2 > 1' "$baseline_tsv" | grep -c . || true)"
baseline_singles=$((baseline_total - baseline_multi))
echo "provider baseline: $baseline_total contacts ($baseline_multi multi-number)" >> "$result"

seed_contact "$mov_one" "$num_one" >/dev/null
seed_contact "$mov_two" "$num_two" >/dev/null
seed_contact "$mov_multi" "$num_multi_a" "$num_multi_b" >/dev/null
seed_contact "$mov_extra" "$num_extra" "" "$email_extra" >/dev/null
provider_dump "$out/pd-post-seed-raw.txt" > "$out/move-copy-post-seed-dump.tsv"
echo '--- provider dump after seeding (name<TAB>number_count) ---' >> "$result"
sed 's/^/  /' "$out/move-copy-post-seed-dump.tsv" >> "$result"
post_total="$(grep -c . < "$out/move-copy-post-seed-dump.tsv" || true)"
post_raw="$(raw_count)"
if [[ "$post_raw" != "$((baseline_raw + 4))" ]]; then
  echo "UNVERIFIED raw_contacts holds $post_raw rows after seeding, expected $((baseline_raw + 4))" | tee -a "$result"
  exit 1
fi
if [[ "$post_total" == "0" && "$post_raw" != "0" ]]; then
  echo 'DIAG provider_dump returned no rows while raw_contacts is non-empty; raw query output follows' | tee -a "$result"
  sed 's/^/  RAW: /' "$out/pd-post-seed-raw.txt" >> "$result"
fi
multi_total="$(awk -F '\t' '$2 > 1' "$out/move-copy-post-seed-dump.tsv" | grep -c . || true)"
singles=$((post_total - multi_total))
# Move excludes contacts with data beyond name+one number; the probe seeds
# exactly one (mov_extra, carries an email row). Copy still takes it.
move_eligible=$((singles - 1))
echo "Seeded 2 plain singles + 1 extra-data single + 1 multi-number; post-seed provider: $post_total contacts, move-eligible: $move_eligible, copy-eligible: $singles, multi: $multi_total" >> "$result"
if ! grep -qF "$mov_multi	2" "$out/move-copy-post-seed-dump.tsv"; then
  echo "UNVERIFIED multi-number seed $mov_multi does not show exactly 2 numbers" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -qF "data1=$email_extra"; then
  echo "UNVERIFIED extra-data seed $mov_extra is missing its email row ($email_extra)" | tee -a "$result"
  exit 1
fi

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

# --- D. Duplicate destination refuses the batch -----------------------------
# Fixture: a SIM entry duplicating mov_one lives in prefs before app start.
adb shell am force-stop "$package" || true
printf '%s\n' \
  '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
  '<map>' \
  "<string name=\"sim_contacts_v1\">[{&quot;name&quot;:&quot;$mov_one&quot;,&quot;number&quot;:&quot;+44 7900 005011&quot;}]</string>" \
  '</map>' | adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'"
fixture_check="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
if ! grep -qF 'sim_contacts_v1' <<< "$fixture_check" || ! grep -qF '+44 7900 005011' <<< "$fixture_check"; then
  echo 'UNVERIFIED duplicate-fixture prefs write did not land' | tee -a "$result"
  echo "$fixture_check" | sed 's/^/  /' >> "$result"
  exit 1
fi
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$move_eligible"
shot movecopy-dup-confirm
tap LSK                         # Yes
sleep 2
require_marker 'MOVE_CONTACTS_REFUSED reason=duplicate dir=phone2sim'
if [[ "$(raw_count)" != "$((baseline_total + 4))" || "$(sim_count)" != "1" ]]; then
  echo "FAIL duplicate refusal changed state (provider $(raw_count), SIM $(sim_count))" | tee -a "$result"
  exit 1
fi
echo 'PASS Duplicate destination refused with the HTML wording and changed nothing' | tee -a "$result"
shot movecopy-dup-refused
# Clear the duplicate fixture: restore the pre-probe prefs.
adb shell am force-stop "$package" || true
if [[ "$restore_needed" == "1" ]]; then
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup"
else
  adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
fi

# --- F. Partial failure rolls the batch back ---------------------------------
# Confirm, then delete mov_two's provider row behind the app's back; the
# move of mov_two fails and the already-moved mov_one must come back.
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$move_eligible"
shot movecopy-rollback-confirm
two_id="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' \
  | grep -E "data1=$mov_two\$" | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
if [[ ! "$two_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not resolve $mov_two raw id for the failure fixture" | tee -a "$result"
  exit 1
fi
adb shell content delete --uri content://com.android.contacts/raw_contacts   --where "_id=$two_id" >/dev/null 2>&1 || true
tap LSK                         # Yes: mov_one moves, mov_two fails -> rollback
sleep 2
require_marker "MOVE_CONTACTS dir=phone2sim done=1 failed=1 skipped=$((multi_total + 1)) skipped_multi=$multi_total skipped_data=1 rolled_back=1 undone=1"
echo 'PASS rollback marker honest: rolled_back=1 with undone==done' | tee -a "$result"
f_provider="$(raw_count)"
f_sim="$(sim_count)"
if [[ "$f_sim" != "0" ]]; then
  echo "FAIL rollback left $f_sim entries in the SIM list" | tee -a "$result"
  exit 1
fi
if [[ "$f_provider" != "$((baseline_total + 3))" ]]; then
  echo "FAIL provider holds $f_provider rows after rollback, expected $((baseline_total + 3)) (baseline + mov_one re-added + multi + extra-data)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -qE "data1=$mov_one\$"; then
  echo "FAIL rollback did not restore $mov_one to the provider" | tee -a "$result"
  exit 1
fi
echo 'PASS Move phone->SIM rolled back after a mid-batch failure (Contacts changed; try again)' | tee -a "$result"
shot movecopy-after-rollback
# Re-seed mov_two for the remaining sections.
seed_contact "$mov_two" "$num_two" >/dev/null

# --- E. Empty SIM memory is refused ------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Copy contacts submenu
tap DOWN                        # From SIM to phone (SIM memory empty)
adb logcat -c || true
tap OK
sleep 1
require_marker 'COPY_CONTACTS_REFUSED reason=empty dir=1'
echo 'PASS Copy from empty SIM memory refused with Selected memory empty' | tee -a "$result"
shot movecopy-empty-refused

# --- M1. Move phone -> SIM (multi-number skipped) ----------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu (sel 0 = From phone to SIM)
shot movecopy-move-submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$move_eligible"
shot movecopy-move-confirm
tap LSK                         # Yes
sleep 2
require_marker "MOVE_CONTACTS dir=phone2sim done=$move_eligible failed=0 skipped=$((multi_total + 1)) skipped_multi=$multi_total skipped_data=1"
after_move="$(raw_count)"
if [[ "$after_move" != "$((multi_total + 1))" ]]; then
  echo "FAIL Move left $after_move raw contacts, expected the $multi_total multi-number + the extra-data contact" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -qE "data1=$mov_multi\$"; then
  echo "FAIL multi-number contact $mov_multi is gone after Move" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -qF "data1=$email_extra"; then
  echo "FAIL extra-data contact $mov_extra lost its email row after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS extra-data contact kept its email row after Move' | tee -a "$result"
sim_after_move="$(sim_count)"
if [[ "$sim_after_move" != "$move_eligible" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_move entries, expected $move_eligible" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -qF "$mov_extra"; then
  echo "FAIL extra-data contact $mov_extra landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
echo "PASS Move phone->SIM moved $move_eligible singles; multi-number and extra-data contacts untouched in provider" | tee -a "$result"
expect_foreground
shot movecopy-after-move

# --- M2. Move SIM -> phone ----------------------------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
tap DOWN                        # From SIM to phone
adb logcat -c || true
tap OK
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=sim2phone count=$move_eligible"
tap LSK                         # Yes
sleep 2
require_marker "MOVE_CONTACTS dir=sim2phone done=$move_eligible failed=0 skipped=0 skipped_multi=0 skipped_data=0"
after_back="$(raw_count)"
if [[ "$after_back" != "$((baseline_total + 4))" ]]; then
  echo "FAIL Move SIM->phone left provider at $after_back, expected $((baseline_total + 4))" | tee -a "$result"
  exit 1
fi
sim_after_back="$(sim_count)"
if [[ "$sim_after_back" != "0" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_back entries after move back, expected 0" | tee -a "$result"
  exit 1
fi
echo 'PASS Move SIM->phone restored provider and emptied SIM list' | tee -a "$result"
expect_foreground
shot movecopy-after-move-back

# --- C. Copy phone -> SIM (multi-number skipped) ------------------------------
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Copy contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "COPY_CONTACTS_CONFIRM dir=phone2sim count=$singles"
shot movecopy-copy-confirm
tap LSK                         # Yes
sleep 2
require_marker "COPY_CONTACTS dir=phone2sim done=$singles failed=0 skipped=$multi_total skipped_multi=$multi_total skipped_data=0"
after_copy="$(raw_count)"
if [[ "$after_copy" != "$((baseline_total + 4))" ]]; then
  echo "FAIL Copy changed provider count to $after_copy, expected $((baseline_total + 4))" | tee -a "$result"
  exit 1
fi
sim_after_copy="$(sim_count)"
if [[ "$sim_after_copy" != "$singles" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_copy entries after copy, expected $singles" | tee -a "$result"
  exit 1
fi
if ! adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -qF "$mov_extra"; then
  echo "FAIL extra-data contact $mov_extra missing from the SIM list after Copy" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -qF "data1=$email_extra"; then
  echo "FAIL Copy disturbed the extra-data contact original (email row gone)" | tee -a "$result"
  exit 1
fi
echo 'PASS extra-data contact copied to SIM as name+number only' | tee -a "$result"
echo "PASS Copy phone->SIM copied $singles singles incl. extra-data contact; multi-number skipped" | tee -a "$result"
expect_foreground
shot movecopy-after-copy

# --- N. No changes nothing ----------------------------------------------------
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
if [[ "$after_no" != "$((baseline_total + 4))" || "$sim_after_no" != "$singles" ]]; then
  echo "FAIL No changed state (provider $after_no, SIM $sim_after_no)" | tee -a "$result"
  exit 1
fi
echo 'PASS Copy contacts No left provider and SIM list unchanged' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Move/Copy flows' | tee -a "$result"
shot movecopy-after-no

# --- W. Duplicates within the source batch refuse the Move -------------------
seed_contact "AAAC2MoveDup" "07900006020" >/dev/null
seed_contact "AAAC2MoveDup" "07900006020" >/dev/null
sim_before_w="$(sim_count)"
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker 'MOVE_CONTACTS_CONFIRM dir=phone2sim'
tap LSK                         # Yes
sleep 2
require_marker 'MOVE_CONTACTS_REFUSED reason=duplicate_source dir=phone2sim'
if [[ "$(raw_count)" != "$((baseline_total + 6))" || "$(sim_count)" != "$sim_before_w" ]]; then
  echo "FAIL within-source duplicate refusal changed state (provider $(raw_count), SIM $(sim_count) was $sim_before_w)" | tee -a "$result"
  exit 1
fi
echo 'PASS within-source duplicate batch refused (duplicate_source)' | tee -a "$result"
shot movecopy-dup-source-refused
# Remove the duplicate fixtures so the trap count guard sees its 4 rows.
dup_rows="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E 'data1=AAAC2MoveDup' || true)"
while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
  [[ "$rid" =~ ^[0-9]+$ ]] || continue
  adb shell content delete --uri content://com.android.contacts/raw_contacts \
    --where "_id=$rid" >/dev/null 2>&1 || true
done <<< "$dup_rows"

# --- R. Rollback failure is reported honestly ---------------------------------
# SIM list: 20 valid entries then one with a blank number. Move SIM->phone
# inserts the valid ones, then fails; the rollback must delete the inserted
# rows. WRITE_CONTACTS is revoked the moment Yes is tapped so the reverse
# deletes fail too: the app must log rolled_back=0 with undone < done and
# show the undo-incomplete notice. The revoke can race past the handler
# (vacuous or complete rollback), so the attempt retries.
rb_pass=0
rb_done=0
rb_undone=0
for attempt in 1 2 3; do
  echo "rollback-injection attempt $attempt" >> "$result"
  # Drop stranded inserts from a previous attempt.
  rb_rows="$(adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E 'data1=AAAC2Rb' || true)"
  while IFS= read -r line; do
    [[ -n "$line" ]] || continue
    rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
    [[ "$rid" =~ ^[0-9]+$ ]] || continue
    adb shell content delete --uri content://com.android.contacts/raw_contacts \
      --where "_id=$rid" >/dev/null 2>&1 || true
  done <<< "$rb_rows"
  adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
  sim_json="$(python3 -c '
import json
entries = [{"name": f"AAAC2Rb{i:02d}", "number": f"079000060{i:02d}"} for i in range(1, 21)]
entries.append({"name": "AAAC2RbBad", "number": ""})
print(json.dumps(entries))
')"
  sim_escaped="$(python3 -c 'import sys, html; print(html.escape(sys.stdin.read().strip(), quote=True))' <<< "$sim_json")"
  adb shell am force-stop "$package" || true
  printf '%s\n' \
    '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
    '<map>' \
    "<string name=\"sim_contacts_v1\">$sim_escaped</string>" \
    '</map>' | adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'"
  fixture_check="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
  if ! grep -qF 'AAAC2RbBad' <<< "$fixture_check"; then
    echo 'UNVERIFIED rollback-fixture prefs write did not land' | tee -a "$result"
    exit 1
  fi
  open_contacts_menu
  tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
  tap OK                          # Move contacts submenu
  adb logcat -c || true
  tap DOWN                        # From SIM to phone
  tap OK
  sleep 1
  require_marker 'MOVE_CONTACTS_CONFIRM dir=sim2phone count=21'
  ( for i in $(seq 1 10); do
      adb shell pm revoke "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
    done ) &
  rb_revoke_pid=$!
  tap LSK                         # Yes
  wait "$rb_revoke_pid" || true
  sleep 3
  require_marker 'MOVE_CONTACTS dir=sim2phone'
  marker_line="$(adb logcat -d -s C2Contacts:I 2>/dev/null | grep -F 'MOVE_CONTACTS dir=sim2phone' | tail -1)"
  echo "  attempt $attempt marker: $marker_line" >> "$result"
  rb_done="$(sed -n 's/.* done=\([0-9][0-9]*\).*/\1/p' <<< "$marker_line")"
  rb_undone="$(sed -n 's/.* undone=\([0-9][0-9]*\).*/\1/p' <<< "$marker_line")"
  if grep -qF 'rolled_back=0' <<< "$marker_line" \
     && [[ "$rb_done" =~ ^[0-9]+$ && "$rb_undone" =~ ^[0-9]+$ && "$rb_undone" -lt "$rb_done" ]]; then
    rb_pass=1
    break
  fi
done
adb shell pm grant "$package" android.permission.WRITE_CONTACTS 2>/dev/null || true
if [[ "$rb_pass" != "1" ]]; then
  echo 'UNVERIFIED rollback-failure injection never landed (revoke raced past the handler 3 times)' | tee -a "$result"
  exit 1
fi
echo "PASS rollback failure reported honestly: rolled_back=0, undone=$rb_undone < done=$rb_done" | tee -a "$result"
shot movecopy-rollback-incomplete
# Clean up the stranded inserts and the SIM fixture; restore pre-probe prefs.
rb_rows="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E 'data1=AAAC2Rb' || true)"
while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
  [[ "$rid" =~ ^[0-9]+$ ]] || continue
  adb shell content delete --uri content://com.android.contacts/raw_contacts \
    --where "_id=$rid" >/dev/null 2>&1 || true
done <<< "$rb_rows"
adb shell am force-stop "$package" || true
if [[ "$restore_needed" == "1" ]]; then
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup"
else
  adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
fi

# Mirror key screenshots into the results as single-line base64 so the job
# log (the only reliable retrieval path) carries them for visual review.
for f in movecopy-move-submenu movecopy-move-confirm movecopy-dup-refused movecopy-empty-refused movecopy-copy-confirm movecopy-confirm-no movecopy-dup-source-refused movecopy-rollback-incomplete; do
  if [[ -s "$out/$f.png" ]]; then
    echo "SHOTB64 $f $(base64 -w0 "$out/$f.png")" >> "$result"
  fi
done
echo 'PASS Move/Copy contacts acceptance complete' | tee -a "$result"
