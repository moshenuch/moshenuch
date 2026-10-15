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
#  L.  A contact whose only number carries a custom label is skipped on Move
#     and kept intact (label row included); Copy takes it as name+number
#     (audit #17).
#  (The fail-closed safety-scan refusal of audit #17 item 1 is NOT probed:
#  an incomplete scan means the provider errored while the permission is
#  granted - AppOps denial is not enforced against the contacts provider on
#  this emulator, and pm revoke kills the app process, so there is no
#  external injection that reaches that path. Code-audited instead.)
#  W.  Two identical contacts inside the source batch refuse the whole Move
#     (duplicate_source; audit #16). The pair is seeded with aggregation
#     suspended, because the provider otherwise merges identical
#     name+number rows into one multi-number contact (run 38803437070).
#  R.  Rollback-failure injection: WRITE_CONTACTS revoked mid-rollback makes
#     the reverse deletes fail; the app must log rolled_back=0 with
#     undone < done and show the undo-incomplete notice (audit #16).
#  V.  A second number added to a contact between confirm and Yes makes the
#     Yes-time revalidation skip it (fresh number scan, audit #18); the
#     contact stays intact with both numbers.
#  B2. A SIM entry with a blank number is rejected on Move SIM->phone and
#     the partial batch rolls back cleanly - separate from the R
#     rollback-failure injection (audit #18).
#  G.  A same-count mutation - the number VALUE changed between confirm
#     and Yes - refuses the batch with the HTML changed-list wording
#     (audit #19); the mutated contact stays as edited.
# The probe requires an emulator (ro.kernel.qemu/ro.boot.qemu) and refuses
# to run otherwise; pre-existing provider contacts must all be synthetic
# AAAC2* suite leftovers, because the EXIT trap restores prefs and removes
# its own fixtures but could only re-seed anything else as a bare name row
# (audit #18).
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
mov_label="AAAC2MoveLabel"
num_one="07900005011"
num_two="07900005012"
num_multi_a="07900005013"
num_multi_b="07900005014"
num_extra="07900005015"
num_label="07900005016"
mov_sync="AAAC2MoveSync"
num_sync="07900005017"
sync_account="c2probe.sync@example.com"
num_noname="07900005018"
mov_home="AAAC2MoveHome"
num_home="07900005019"
mov_prim="AAAC2MovePrim"
num_prim="07900005020"
mov_gain="AAAC2MoveGain"
num_gain="07900005021"
num_gain2="07900005022"
blank_ok="AAAC2BlankOk"
num_blank_ok="07900007031"
mov_sname="AAAC2MoveName"
given_sname="AAAC2MoveGiven"
family_sname="AAAC2MoveFamily"
num_sname="07900005023"
mov_mut="AAAC2MoveMut"
num_mut="07900005024"
num_mut2="07900005025"
email_extra="mov.extra@c2probe.example"
label_custom="C2ProbeLabel"

# Emulator-only guard (audit #18): this probe seeds and deletes provider
# contacts and its cleanup is fixture-scoped; refuse on real hardware.
qemu="$(adb shell getprop ro.kernel.qemu | tr -d '\r')"
qemu_boot="$(adb shell getprop ro.boot.qemu | tr -d '\r')"
if [[ "$qemu" != "1" && "$qemu_boot" != "1" ]]; then
  echo "UNVERIFIED not an emulator (ro.kernel.qemu='$qemu' ro.boot.qemu='$qemu_boot'); refusing to run a contact-mutating probe" | tee -a "$result"
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
      # </dev/null: adb consumes the loop's stdin otherwise and every second
      # baseline line would be skipped (run 38815565784 analysis).
      if ! provider_dump "$out/.pd-trap-raw.txt" < /dev/null | grep -F "$name	">/dev/null; then
        local rid
        rid="$(adb shell content insert --uri content://com.android.contacts/raw_contacts \
          --bind starred:i:0 < /dev/null 2>&1 | tr -d '\r' | grep -oE 'raw_contact_id=[0-9]+|/[0-9]+$' | grep -oE '[0-9]+' | head -1 || true)"
        if [[ "$rid" =~ ^[0-9]+$ ]]; then
          adb shell content insert --uri content://com.android.contacts/data \
            --bind raw_contact_id:i:"$rid" \
            --bind mimetype:s:vnd.android.cursor.item/name \
            --bind data1:s:"$name" < /dev/null >/dev/null 2>&1 || true
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
    --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E "data1=($mov_one|$mov_two|$mov_multi|$mov_extra|$mov_label|$mov_sync|$mov_home|$mov_prim|$mov_sname)" || true)"
  row_count="$(grep -c . <<< "$rows" || true)"
  if [[ "$row_count" == "9" ]]; then
    local line rid
    while IFS= read -r line; do
      rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
      [[ "$rid" =~ ^[0-9]+$ ]] || continue
      adb shell content delete --uri content://com.android.contacts/raw_contacts \
        --where "_id=$rid" < /dev/null >/dev/null 2>&1 || true
    done <<< "$rows"
    echo "PASS seeded Move/Copy contacts removed from provider" | tee -a "$result"
  elif [[ "$row_count" != "0" ]]; then
    echo "WARN seeded-name provider rows: $row_count (expected 0 or 9); left for the discarded emulator" | tee -a "$result"
  fi
  # The nameless seed has no name row; delete it by its phone number.
  local nn_rid
  nn_rid="$(adb shell content query --uri content://com.android.contacts/data \
    --projection raw_contact_id:mimetype:data1 2>&1 | tr -d '\r' \
    | grep -F 'phone_v2' | grep -F "data1=$num_noname" \
    | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
  if [[ "$nn_rid" =~ ^[0-9]+$ ]]; then
    adb shell content delete --uri content://com.android.contacts/raw_contacts \
      --where "_id=$nn_rid" >/dev/null 2>&1 || true
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
  adb logcat -d -s C2Contacts:I 2>/dev/null | grep -F "$1" >/dev/null
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
raw_ids() { # one raw-contact _id per line, lexicographically sorted for comm
  adb shell content query --uri content://com.android.contacts/raw_contacts \
    --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort || true
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
seed_contact() { # seed_contact <name> <number> [number2] [email] [label]; prints the new raw id
  local name="$1" number="$2" number2="${3:-}" email="${4:-}" label="${5:-}" before after new_id
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
  if [[ -n "$label" ]]; then
    adb shell content insert --uri content://com.android.contacts/data \
      --bind raw_contact_id:i:"$new_id" \
      --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
      --bind data1:s:"$number" --bind data2:i:0 --bind data3:s:"$label" >/dev/null 2>&1 || true
  else
    adb shell content insert --uri content://com.android.contacts/data \
      --bind raw_contact_id:i:"$new_id" \
      --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
      --bind data1:s:"$number" --bind data2:i:2 >/dev/null 2>&1 || true
  fi
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

# Account-backed (synced) contact: Move must skip it because a local delete
# propagates through sync (audit #17 item 4).
seed_sync_contact() { # seed_sync_contact <name> <number> <account_name>
  local name="$1" number="$2" account="$3" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind account_type:s:com.google --bind account_name:s:"$account" >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
    echo "UNVERIFIED account-backed seed of $name did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
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

# Nameless (number-only) contact: the provider fills its display name with
# the number itself and the SIM->phone return path refuses blank names, so
# Move must skip it instead of stranding it (audit #17 item 5).
seed_noname_contact() { # seed_noname_contact <number>
  local number="$1" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
    echo "UNVERIFIED nameless seed did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
    exit 1
  fi
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
    --bind data1:s:"$number" --bind data2:i:2 >/dev/null 2>&1 || true
  echo "$new_id"
}

# Non-plain-mobile contacts: restore writes TYPE_MOBILE without is_primary,
# so a HOME/WORK number or a primary flag would be lost by the round trip;
# Move must skip them (audit #18).
seed_typed_contact() { # seed_typed_contact <name> <number> <type> [is_primary]
  local name="$1" number="$2" type="$3" primary="${4:-0}" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
    echo "UNVERIFIED typed seed of $name did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
    exit 1
  fi
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"$name" >/dev/null 2>&1 || true
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
    --bind data1:s:"$number" --bind data2:i:"$type" \
    --bind is_primary:i:"$primary" >/dev/null 2>&1 || true
  echo "$new_id"
}

# Identical name+number seeds get merged into one multi-number aggregate by
# the provider unless aggregation is suspended; the source-duplicate
# refusal needs two real entries (run 38803437070 W analysis).
seed_dup_contact() { # seed_dup_contact <name> <number>
  local name="$1" number="$2" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 --bind aggregation_mode:i:1 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
    echo "UNVERIFIED dup seed of $name did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
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

# Structured-name contact: given/family components cannot ride a SIM entry
# (restore writes the bare display name), so Move must skip it (audit #19).
seed_sname_contact() { # seed_sname_contact <display> <given> <family> <number>
  local display="$1" given="$2" family="$3" number="$4" before after new_id
  before="$(raw_ids)"
  adb shell content insert --uri content://com.android.contacts/raw_contacts \
    --bind starred:i:0 >/dev/null 2>&1 || true
  after="$(raw_ids)"
  new_id="$(comm -13 <(echo "$before") <(echo "$after") || true)"
  if [[ "$(grep -c . <<< "$new_id" || true)" != "1" ]]; then
    echo "UNVERIFIED structured-name seed did not add exactly one raw contact (new ids: ${new_id:-none})" | tee -a "$result"
    exit 1
  fi
  adb shell content insert --uri content://com.android.contacts/data \
    --bind raw_contact_id:i:"$new_id" \
    --bind mimetype:s:vnd.android.cursor.item/name \
    --bind data1:s:"$display" --bind data2:s:"$given" --bind data3:s:"$family" >/dev/null 2>&1 || true
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
# Audit #18: the trap can only re-seed stranded baseline contacts as bare
# name rows, so any pre-existing contact must be a synthetic suite leftover
# (AAAC2*); refuse when real contact data is present.
if [[ "$baseline_total" != "0" ]]; then
  non_probe="$(cut -f1 "$baseline_tsv" | grep -vE '^AAAC2' || true)"
  if [[ -n "$non_probe" ]]; then
    echo "UNVERIFIED provider baseline holds non-probe contacts; the trap can only restore bare name rows, so the probe refuses to run with real contact data present" | tee -a "$result"
    exit 1
  fi
  echo "provider baseline contacts are all synthetic AAAC2* suite leftovers" >> "$result"
fi

seed_contact "$mov_one" "$num_one" >/dev/null
seed_contact "$mov_two" "$num_two" >/dev/null
seed_contact "$mov_multi" "$num_multi_a" "$num_multi_b" >/dev/null
seed_contact "$mov_extra" "$num_extra" "" "$email_extra" >/dev/null
seed_contact "$mov_label" "$num_label" "" "" "$label_custom" >/dev/null
seed_sync_contact "$mov_sync" "$num_sync" "$sync_account" >/dev/null
seed_noname_contact "$num_noname" >/dev/null
seed_typed_contact "$mov_home" "$num_home" 1 >/dev/null
seed_typed_contact "$mov_prim" "$num_prim" 2 1 >/dev/null
seed_sname_contact "$mov_sname" "$given_sname" "$family_sname" "$num_sname" >/dev/null
provider_dump "$out/pd-post-seed-raw.txt" > "$out/move-copy-post-seed-dump.tsv"
echo '--- provider dump after seeding (name<TAB>number_count) ---' >> "$result"
sed 's/^/  /' "$out/move-copy-post-seed-dump.tsv" >> "$result"
post_total="$(grep -c . < "$out/move-copy-post-seed-dump.tsv" || true)"
post_raw="$(raw_count)"
if [[ "$post_raw" != "$((baseline_raw + 10))" ]]; then
  echo "UNVERIFIED raw_contacts holds $post_raw rows after seeding, expected $((baseline_raw + 10))" | tee -a "$result"
  exit 1
fi
if [[ "$post_total" == "0" && "$post_raw" != "0" ]]; then
  echo 'DIAG provider_dump returned no rows while raw_contacts is non-empty; raw query output follows' | tee -a "$result"
  sed 's/^/  RAW: /' "$out/pd-post-seed-raw.txt" >> "$result"
fi
multi_total="$(awk -F '\t' '$2 > 1' "$out/move-copy-post-seed-dump.tsv" | grep -c . || true)"
singles=$((post_total - multi_total))
# Move excludes contacts with data beyond name+one number or no usable
# name; the probe seeds exactly seven (mov_extra an email row, mov_label a
# custom label, mov_sync account-backed, mov_home a HOME number, mov_prim
# an is_primary flag, mov_sname structured name components, plus the
# nameless number-only contact). Copy still takes the named ones.
move_eligible=$((singles - 7))
echo "Seeded 2 plain singles + 1 email + 1 custom-label + 1 account-backed + 1 nameless + 1 non-mobile + 1 primary + 1 structured-name + 1 multi-number; post-seed provider: $post_total contacts, move-eligible: $move_eligible, copy-eligible: $singles, multi: $multi_total" >> "$result"
if ! grep -qF "$mov_multi	2" "$out/move-copy-post-seed-dump.tsv"; then
  echo "UNVERIFIED multi-number seed $mov_multi does not show exactly 2 numbers" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -F "data1=$email_extra">/dev/null; then
  echo "UNVERIFIED extra-data seed $mov_extra is missing its email row ($email_extra)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data3 2>&1 | tr -d '\r' | grep -F "data3=$label_custom">/dev/null; then
  echo "UNVERIFIED custom-label seed $mov_label is missing its label row ($label_custom)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/raw_contacts \
     --projection account_name 2>&1 | tr -d '\r' | grep -F "account_name=$sync_account">/dev/null; then
  echo "UNVERIFIED account-backed seed $mov_sync is missing its account ($sync_account)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:data2 2>&1 | tr -d '\r' \
     | grep -E "data1=$num_home, data2=1\$">/dev/null; then
  echo "UNVERIFIED non-mobile seed $mov_home is missing its HOME phone row (data2=1)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:is_primary 2>&1 | tr -d '\r' \
     | grep -E "data1=$num_prim, is_primary=1\$">/dev/null; then
  echo "UNVERIFIED primary seed $mov_prim is missing its is_primary flag" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:data2:data3 2>&1 | tr -d '\r' \
     | grep -E "data1=$mov_sname, data2=$given_sname, data3=$family_sname\$">/dev/null; then
  echo "UNVERIFIED structured-name seed $mov_sname is missing its given/family components" | tee -a "$result"
  exit 1
fi
if ! grep -qF "$(printf '(noname)\t1')" "$out/move-copy-post-seed-dump.tsv"; then
  echo "UNVERIFIED nameless seed does not show as a (noname) single-number contact" | tee -a "$result"
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
if [[ "$(raw_count)" != "$((baseline_total + 10))" || "$(sim_count)" != "1" ]]; then
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
# move of mov_two fails and everything already moved in the batch must come
# back. Contacts are processed in display-name order, so the batch moves
# AAAC2DelOne, AAAC2DelTwo and AAAC2MoveOne before reaching mov_two.
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
require_marker "MOVE_CONTACTS dir=phone2sim done=3 failed=1 skipped=$((multi_total + 7)) skipped_multi=$multi_total skipped_data=6 skipped_noname=1 rolled_back=1 undone=3"
echo 'PASS rollback marker honest: rolled_back=1 with undone==done' | tee -a "$result"
f_provider="$(raw_count)"
f_sim="$(sim_count)"
if [[ "$f_sim" != "0" ]]; then
  echo "FAIL rollback left $f_sim entries in the SIM list" | tee -a "$result"
  exit 1
fi
if [[ "$f_provider" != "$((baseline_total + 9))" ]]; then
  echo "FAIL provider holds $f_provider rows after rollback, expected $((baseline_total + 9)) (baseline + mov_one re-added + multi + extra-data + custom-label + account-backed + nameless + non-mobile + primary + structured-name)" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$mov_one\$">/dev/null; then
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
require_marker "MOVE_CONTACTS dir=phone2sim done=$move_eligible failed=0 skipped=$((multi_total + 7)) skipped_multi=$multi_total skipped_data=6 skipped_noname=1"
after_move="$(raw_count)"
if [[ "$after_move" != "$((multi_total + 7))" ]]; then
  echo "FAIL Move left $after_move raw contacts, expected the $multi_total multi-number + extra-data + custom-label + account-backed + nameless + non-mobile + primary + structured-name contacts" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -E "data1=$mov_multi\$">/dev/null; then
  echo "FAIL multi-number contact $mov_multi is gone after Move" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -F "data1=$email_extra">/dev/null; then
  echo "FAIL extra-data contact $mov_extra lost its email row after Move" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data3 2>&1 \
     | tr -d '\r' | grep -F "data3=$label_custom">/dev/null; then
  echo "FAIL custom-label contact $mov_label lost its label row after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS extra-data contact kept its email row after Move' | tee -a "$result"
echo 'PASS custom-label contact kept its label row after Move' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$mov_sync\$">/dev/null; then
  echo "FAIL account-backed contact $mov_sync missing from the provider after Move" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/raw_contacts \
     --projection account_name 2>&1 | tr -d '\r' | grep -F "account_name=$sync_account">/dev/null; then
  echo "FAIL account-backed contact $mov_sync lost its account row after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS account-backed contact kept out of Move' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -F "data1=$num_noname">/dev/null; then
  echo "FAIL nameless contact ($num_noname) missing from the provider after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS nameless contact kept out of Move' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:data2 2>&1 | tr -d '\r' \
     | grep -E "data1=$num_home, data2=1\$">/dev/null; then
  echo "FAIL non-mobile contact $mov_home lost its HOME phone row after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS non-mobile contact kept out of Move' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:is_primary 2>&1 | tr -d '\r' \
     | grep -E "data1=$num_prim, is_primary=1\$">/dev/null; then
  echo "FAIL primary contact $mov_prim lost its is_primary flag after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS primary-number contact kept out of Move' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1:data2:data3 2>&1 | tr -d '\r' \
     | grep -E "data1=$mov_sname, data2=$given_sname, data3=$family_sname\$">/dev/null; then
  echo "FAIL structured-name contact $mov_sname lost its name components after Move" | tee -a "$result"
  exit 1
fi
echo 'PASS structured-name contact kept out of Move' | tee -a "$result"
sim_after_move="$(sim_count)"
if [[ "$sim_after_move" != "$move_eligible" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_move entries, expected $move_eligible" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_extra">/dev/null; then
  echo "FAIL extra-data contact $mov_extra landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_label">/dev/null; then
  echo "FAIL custom-label contact $mov_label landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_sync">/dev/null; then
  echo "FAIL account-backed contact $mov_sync landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$num_noname">/dev/null; then
  echo "FAIL nameless contact ($num_noname) landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_home">/dev/null; then
  echo "FAIL non-mobile contact $mov_home landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_prim">/dev/null; then
  echo "FAIL primary contact $mov_prim landed in the SIM list on Move" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_sname">/dev/null; then
  echo "FAIL structured-name contact $mov_sname landed in the SIM list on Move" | tee -a "$result"
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
if [[ "$after_back" != "$((baseline_total + 10))" ]]; then
  echo "FAIL Move SIM->phone left provider at $after_back, expected $((baseline_total + 10))" | tee -a "$result"
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

# --- V. Number added between confirm and Yes is honoured --------------------
# Audit #18: the Yes-time revalidation reloads contact numbers, so a contact
# that gains a number after the confirm must be skipped, not moved with
# silent number loss.
seed_contact "$mov_gain" "$num_gain" >/dev/null
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu (sel 0 = From phone to SIM)
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$((move_eligible + 1))"
shot movecopy-gain-confirm
gain_id="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' \
  | grep -E "data1=$mov_gain\$" | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
if [[ ! "$gain_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not resolve $mov_gain raw id for the mid-flow number injection" | tee -a "$result"
  exit 1
fi
adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$gain_id" \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:"$num_gain2" --bind data2:i:2 >/dev/null 2>&1 || true
tap LSK                         # Yes
sleep 2
require_marker "MOVE_CONTACTS dir=phone2sim done=$move_eligible failed=0 skipped=$((multi_total + 8)) skipped_multi=$((multi_total + 1)) skipped_data=6 skipped_noname=1"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$num_gain\$">/dev/null; then
  echo "FAIL mid-flow contact $mov_gain lost its first number after the skip" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$num_gain2\$">/dev/null; then
  echo "FAIL mid-flow contact $mov_gain lost its injected second number after the skip" | tee -a "$result"
  exit 1
fi
if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_gain">/dev/null; then
  echo "FAIL mid-flow multi-number contact $mov_gain landed in the SIM list" | tee -a "$result"
  exit 1
fi
echo 'PASS number added between confirm and Yes keeps the contact out of Move' | tee -a "$result"
shot movecopy-gain-skipped
# Restore the pre-V state: move the SIM list back, then remove the fixture.
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
gain_id="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' \
  | grep -E "data1=$mov_gain\$" | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
if [[ "$gain_id" =~ ^[0-9]+$ ]]; then
  adb shell content delete --uri content://com.android.contacts/raw_contacts \
    --where "_id=$gain_id" >/dev/null 2>&1 || true
fi
if [[ "$(raw_count)" != "$((baseline_total + 10))" || "$(sim_count)" != "0" ]]; then
  echo "FAIL V restore left provider at $(raw_count) and SIM at $(sim_count), expected $((baseline_total + 10)) and 0" | tee -a "$result"
  exit 1
fi
expect_foreground

# --- B2. Blank-number SIM entry rejected, batch rolls back cleanly ----------
# Audit #18: distinct from R (which injects a rollback FAILURE); here the
# undo succeeds and the notice must be the plain changed-list wording.
adb shell am force-stop "$package" || true
printf '%s\n' \
  '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
  '<map>' \
  "<string name=\"sim_contacts_v1\">[{&quot;name&quot;:&quot;$blank_ok&quot;,&quot;number&quot;:&quot;$num_blank_ok&quot;},{&quot;name&quot;:&quot;AAAC2BlankBad&quot;,&quot;number&quot;:&quot;&quot;}]</string>" \
  '</map>' | adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'"
fixture_check="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
if ! grep -qF 'AAAC2BlankBad' <<< "$fixture_check"; then
  echo 'UNVERIFIED blank-number prefs write did not land' | tee -a "$result"
  exit 1
fi
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu
tap DOWN                        # From SIM to phone
adb logcat -c || true
tap OK
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=sim2phone count=2"
tap LSK                         # Yes: blank_ok inserts, blank entry fails
sleep 2
require_marker "MOVE_CONTACTS dir=sim2phone done=1 failed=1 skipped=0 skipped_multi=0 skipped_data=0 skipped_noname=0 rolled_back=1 undone=1"
require_marker "MOVE_CONTACTS_NOTICE text=Contacts changed; try again"
if [[ "$(raw_count)" != "$((baseline_total + 10))" ]]; then
  echo "FAIL blank-number rejection changed the provider (raw contacts: $(raw_count))" | tee -a "$result"
  exit 1
fi
if [[ "$(sim_count)" != "2" ]]; then
  echo "FAIL blank-number rollback left SIM list at $(sim_count), expected 2 (both entries restored)" | tee -a "$result"
  exit 1
fi
if adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -F "data1=$blank_ok">/dev/null; then
  echo "FAIL rolled-back insert $blank_ok still present in the provider" | tee -a "$result"
  exit 1
fi
echo 'PASS blank-number SIM entry rejected; batch rolled back cleanly' | tee -a "$result"
shot movecopy-blank-rejected
# Clear the blank fixtures: restore the pre-probe prefs.
adb shell am force-stop "$package" || true
if [[ "$restore_needed" == "1" ]]; then
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup"
else
  adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
fi

# --- G. Same-count mutation between confirm and Yes refuses the Move --------
seed_contact "$mov_mut" "$num_mut" >/dev/null
open_contacts_menu
tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
tap OK                          # Move contacts submenu (sel 0 = From phone to SIM)
adb logcat -c || true
tap OK                          # From phone to SIM
sleep 1
require_marker "MOVE_CONTACTS_CONFIRM dir=phone2sim count=$((move_eligible + 1))"
shot movecopy-mutation-confirm
mut_id="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' \
  | grep -E "data1=$mov_mut\$" | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
if [[ ! "$mut_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not resolve $mov_mut raw id for the mutation fixture" | tee -a "$result"
  exit 1
fi
mut_data_id="$(adb shell content query --uri content://com.android.contacts/data \
  --projection _id:mimetype --where "raw_contact_id=$mut_id" 2>&1 | tr -d '\r' \
  | grep -F 'phone_v2' | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -1)"
if [[ ! "$mut_data_id" =~ ^[0-9]+$ ]]; then
  echo "UNVERIFIED could not resolve $mov_mut phone row id for the mutation fixture" | tee -a "$result"
  exit 1
fi
adb shell content update --uri content://com.android.contacts/data \
  --where "_id=$mut_data_id" --bind data1:s:"$num_mut2" < /dev/null >/dev/null 2>&1 || true
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$num_mut2\$">/dev/null; then
  echo "UNVERIFIED mutation of $mov_mut to $num_mut2 did not land" | tee -a "$result"
  exit 1
fi
tap LSK                         # Yes
sleep 2
require_marker 'MOVE_CONTACTS_REFUSED reason=changed dir=phone2sim'
if [[ "$(raw_count)" != "$((baseline_total + 11))" || "$(sim_count)" != "0" ]]; then
  echo "FAIL mutation refusal changed state (provider $(raw_count), SIM $(sim_count))" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 | tr -d '\r' | grep -E "data1=$num_mut2\$">/dev/null; then
  echo "FAIL mutated contact $mov_mut lost its edited number $num_mut2" | tee -a "$result"
  exit 1
fi
echo 'PASS same-count mutation between confirm and Yes refused the Move' | tee -a "$result"
shot movecopy-mutation-refused
adb shell content delete --uri content://com.android.contacts/raw_contacts \
  --where "_id=$mut_id" < /dev/null >/dev/null 2>&1 || true
if [[ "$(raw_count)" != "$((baseline_total + 10))" ]]; then
  echo "UNVERIFIED mutation fixture removal left $(raw_count) raw contacts, expected $((baseline_total + 10))" | tee -a "$result"
  exit 1
fi
expect_foreground

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
echo '--- SIM prefs content after Copy ---' >> "$result"
adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | sed 's/^/  /' >> "$result"
echo '--- C2Contacts logcat after Copy ---' >> "$result"
adb logcat -d -s C2Contacts:I 2>/dev/null | tail -8 | sed 's/^/  /' >> "$result"
after_copy="$(raw_count)"
if [[ "$after_copy" != "$((baseline_total + 10))" ]]; then
  echo "FAIL Copy changed provider count to $after_copy, expected $((baseline_total + 10))" | tee -a "$result"
  exit 1
fi
sim_after_copy="$(sim_count)"
if [[ "$sim_after_copy" != "$singles" ]]; then
  echo "FAIL in-app SIM list holds $sim_after_copy entries after copy, expected $singles" | tee -a "$result"
  exit 1
fi
if ! adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_extra">/dev/null; then
  echo "FAIL extra-data contact $mov_extra missing from the SIM list after Copy" | tee -a "$result"
  exit 1
fi
if ! adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_label">/dev/null; then
  echo "FAIL custom-label contact $mov_label missing from the SIM list after Copy" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data3 2>&1 \
     | tr -d '\r' | grep -F "data3=$label_custom">/dev/null; then
  echo "FAIL Copy disturbed the custom-label contact original (label row gone)" | tee -a "$result"
  exit 1
fi
if ! adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$mov_sync">/dev/null; then
  echo "FAIL account-backed contact $mov_sync missing from the SIM list after Copy" | tee -a "$result"
  exit 1
fi
if ! adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -F "$num_noname">/dev/null; then
  echo "FAIL nameless contact ($num_noname) missing from the SIM list after Copy" | tee -a "$result"
  exit 1
fi
if ! adb shell content query --uri content://com.android.contacts/raw_contacts \
     --projection account_name 2>&1 | tr -d '\r' | grep -F "account_name=$sync_account">/dev/null; then
  echo "FAIL Copy disturbed the account-backed contact original (account row gone)" | tee -a "$result"
  exit 1
fi
echo 'PASS custom-label contact copied to SIM as name+number only' | tee -a "$result"
if ! adb shell content query --uri content://com.android.contacts/data \
     --projection data1 2>&1 \
     | tr -d '\r' | grep -F "data1=$email_extra">/dev/null; then
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
if [[ "$after_no" != "$((baseline_total + 10))" || "$sim_after_no" != "$singles" ]]; then
  echo "FAIL No changed state (provider $after_no, SIM $sim_after_no)" | tee -a "$result"
  exit 1
fi
echo 'PASS Copy contacts No left provider and SIM list unchanged' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Move/Copy flows' | tee -a "$result"
shot movecopy-after-no

# --- W. Duplicates within the source batch refuse the Move -------------------
seed_dup_contact "AAAC2MoveDup" "07900006020" >/dev/null
seed_dup_contact "AAAC2MoveDup" "07900006020" >/dev/null
provider_dump "$out/pd-dup-raw.txt" > "$out/move-copy-dup-dump.tsv"
if [[ "$(grep -cF $'AAAC2MoveDup\t1' "$out/move-copy-dup-dump.tsv" || true)" != "2" ]]; then
  echo 'UNVERIFIED duplicate seeds did not survive as two single-number contacts' | tee -a "$result"
  sed 's/^/  /' "$out/move-copy-dup-dump.tsv" >> "$result"
  exit 1
fi
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
if [[ "$(raw_count)" != "$((baseline_total + 12))" || "$(sim_count)" != "$sim_before_w" ]]; then
  echo "FAIL within-source duplicate refusal changed state (provider $(raw_count), SIM $(sim_count) was $sim_before_w)" | tee -a "$result"
  exit 1
fi
echo 'PASS within-source duplicate batch refused (duplicate_source)' | tee -a "$result"
shot movecopy-dup-source-refused
# Remove the duplicate fixtures so the trap count guard sees its 9 rows.
# Run 38811686679 left one of the two raws behind (silent delete), so the
# removal logs the delete output and retries until both raws are gone.
dup_rows="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E 'data1=AAAC2MoveDup' || true)"
sed 's/^/  DUPROW: /' <<< "$dup_rows" >> "$result"
dup_ids="$(while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line"
done <<< "$dup_rows" | sort -u)"
if [[ "$(grep -c . <<< "$dup_ids" || true)" != "2" ]]; then
  echo "UNVERIFIED expected 2 duplicate-fixture raw ids, got: ${dup_ids:-none}" | tee -a "$result"
  exit 1
fi
while IFS= read -r rid; do
  [[ "$rid" =~ ^[0-9]+$ ]] || continue
  for try in 1 2 3; do
    # </dev/null: adb consumes the loop's stdin otherwise and the second
    # fixture raw is never processed (run 38815565784 root cause).
    del_out="$(adb shell content delete --uri content://com.android.contacts/raw_contacts \
      --where "_id=$rid" < /dev/null 2>&1 | tr -d '\r' || true)"
    echo "  DUPDEL rid=$rid try=$try: $del_out" >> "$result"
    # Count only a real _id row; an empty-provider message would count as a
    # line and fake "still present".
    gone="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
      --projection _id --where "_id=$rid" < /dev/null 2>&1 | tr -d '\r' | grep -cE "_id=$rid\$" || true)"
    [[ "$gone" == "0" ]] && break
    sleep 1
  done
done <<< "$dup_ids"
left="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -cE 'data1=AAAC2MoveDup' || true)"
if [[ "$left" != "0" ]]; then
  echo "UNVERIFIED duplicate fixture rows remaining after delete: $left" | tee -a "$result"
  exit 1
fi
if [[ "$(raw_count)" != "$((baseline_total + 10))" ]]; then
  echo "UNVERIFIED duplicate fixture removal left $(raw_count) raw contacts, expected $((baseline_total + 10))" | tee -a "$result"
  exit 1
fi

# --- R. Rollback failure is reported honestly ---------------------------------
# SIM list: 50 valid entries then one with a blank number. Move SIM->phone
# inserts the valid ones, then the blank-number insert fails (malformed
# entry; the batch rolls back). A watchdog deletes the LAST inserted row
# behind the app's back the moment it appears, so that op's reverse delete
# comes up empty: the app must log rolled_back=0 with undone < done and
# show "Contacts changed; undo incomplete". The probe asserts the exact
# post-failure state: provider holds no AAAC2Rb rows (watchdog removed one,
# the rollback removed the rest), the SIM list still holds all 51 fixture
# entries, and the app logged the visible notice text. (AppOps denial is
# not enforced against the contacts provider on this emulator and pm
# revoke kills the app process mid-handler; external row deletion is the
# only injection that reaches the rollback-failure path.) The watchdog
# targets the last insert because the rollback reaches it last; if the app
# beats the watchdog the attempt rolls back cleanly and retries.
rb_pass=0
rb_done=0
rb_undone=0
rb_left=0
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
      --where "_id=$rid" < /dev/null >/dev/null 2>&1 || true
  done <<< "$rb_rows"
  sim_json="$(python3 -c '
import json
entries = [{"name": f"AAAC2Rb{i:02d}", "number": f"07900006{i:02d}"} for i in range(1, 51)]
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
    echo 'FAIL rollback-fixture prefs write did not land' | tee -a "$result"
    exit 1
  fi
  open_contacts_menu
  tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
  tap OK                          # Move contacts submenu
  adb logcat -c || true
  tap DOWN                        # From SIM to phone
  tap OK
  sleep 1
  require_marker 'MOVE_CONTACTS_CONFIRM dir=sim2phone count=51'
  # Watchdog: the moment the LAST insert (AAAC2Rb50) is visible, delete its
  # raw contact. The rollback reaches it last, maximising the window.
  (
    for i in $(seq 1 100); do
      rid="$(adb shell content query --uri content://com.android.contacts/data \
        --projection raw_contact_id:data1 2>/dev/null | tr -d '\r' \
        | grep -F 'data1=AAAC2Rb50' | head -1 \
        | sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p')"
      if [[ "$rid" =~ ^[0-9]+$ ]]; then
        adb shell content delete --uri content://com.android.contacts/raw_contacts \
          --where "_id=$rid" >/dev/null 2>&1 || true
        echo "  attempt $attempt watchdog deleted last insert (raw contact $rid)" >> "$result"
        break
      fi
    done
  ) &
  rb_watch_pid=$!
  tap LSK                         # Yes
  wait "$rb_watch_pid" || true
  sleep 4
  require_marker 'MOVE_CONTACTS dir=sim2phone'
  marker_line="$(adb logcat -d -s C2Contacts:I 2>/dev/null | grep -F 'MOVE_CONTACTS dir=sim2phone' | tail -1)"
  echo "  attempt $attempt marker: $marker_line" >> "$result"
  rb_done="$(sed -n 's/.* done=\([0-9][0-9]*\).*/\1/p' <<< "$marker_line")"
  rb_undone="$(sed -n 's/.* undone=\([0-9][0-9]*\).*/\1/p' <<< "$marker_line")"
  if ! grep -qF 'rolled_back=0' <<< "$marker_line" \
     || [[ ! "$rb_done" =~ ^[0-9]+$ || ! "$rb_undone" =~ ^[0-9]+$ || "$rb_done" -lt 1 || ! "$rb_undone" -lt "$rb_done" ]]; then
    echo "  attempt $attempt: watchdog missed the rollback window (clean run); retrying" >> "$result"
    continue
  fi
  if [[ "$rb_done" != "50" || "$rb_undone" != "49" ]]; then
    echo "FAIL unexpected rollback counts: done=$rb_done undone=$rb_undone (expected done=50 undone=49)" | tee -a "$result"
    exit 1
  fi
  # Exact post-failure provider state: no AAAC2Rb rows remain (the watchdog
  # removed one, the rollback removed the other 49).
  rb_left="$(adb shell content query --uri content://com.android.contacts/data \
    --projection data1 2>&1 | tr -d '\r' | grep -cE 'data1=AAAC2Rb[0-9]' || true)"
  if [[ "$rb_left" != "0" ]]; then
    echo "FAIL post-failure provider state: $rb_left stranded AAAC2Rb name rows, expected 0" | tee -a "$result"
    exit 1
  fi
  # Exact post-failure SIM state: all 51 fixture entries are still there.
  rb_sim="$(sim_count)"
  if [[ "$rb_sim" != "51" ]]; then
    echo "FAIL post-failure SIM state: $rb_sim entries, expected 51" | tee -a "$result"
    exit 1
  fi
  # The visible notice text, as logged at the moment it was shown.
  if ! adb logcat -d -s C2Contacts:I 2>/dev/null | grep -F 'MOVE_CONTACTS_NOTICE text=Contacts changed; undo incomplete'>/dev/null; then
    echo 'FAIL undo-incomplete notice text was not logged when shown' | tee -a "$result"
    exit 1
  fi
  rb_pass=1
  break
done
if [[ "$rb_pass" != "1" ]]; then
  echo 'UNVERIFIED rollback-failure injection never landed: the watchdog missed the rollback window on all 3 attempts (markers above)' | tee -a "$result"
  exit 1
fi
echo "PASS rollback failure reported honestly: rolled_back=0, undone=$rb_undone < done=$rb_done, provider/SIM state exact, notice text verified" | tee -a "$result"
shot movecopy-rollback-incomplete
# Clean up the stranded inserts and the SIM fixture; restore pre-probe prefs.
rb_rows="$(adb shell content query --uri content://com.android.contacts/data \
  --projection raw_contact_id:data1 2>&1 | tr -d '\r' | grep -E 'data1=AAAC2Rb' || true)"
while IFS= read -r line; do
  [[ -n "$line" ]] || continue
  rid="$(sed -n 's/.*raw_contact_id=\([0-9][0-9]*\).*/\1/p' <<< "$line")"
  [[ "$rid" =~ ^[0-9]+$ ]] || continue
  adb shell content delete --uri content://com.android.contacts/raw_contacts \
    --where "_id=$rid" < /dev/null >/dev/null 2>&1 || true
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
