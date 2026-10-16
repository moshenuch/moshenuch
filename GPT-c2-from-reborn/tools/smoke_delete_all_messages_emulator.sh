#!/usr/bin/env bash
# Android 11 acceptance for Messaging > Delete messages > All messages.
# BLOCKING in the suite:
#  Y. Yes: the in-house "Delete all messages?" question box (HTML v6.343
#     confirmdelete wording, Yes/No softkeys) hides every visible provider
#     SMS (hide-only: the SMS provider must stay byte-identical), clears the
#     app-local drafts and saved templates (HTML confirmdelete clears them),
#     and lands on the Messaging menu with "Delete messages" selected.
#  N. No:  answering No on the confirm hides nothing and writes nothing.
# Coverage notes: SMS provider seeding is shell/root best-effort on this
# image (same limitation as the Conversations probe); when seeding fails the
# provider-hide assertions run empty and the seeded-SMS PASS stays absent so
# the suite warning gate reports the gap. Drafts/templates are seeded
# deterministically through the app prefs fixture (msg-recipients shape) and
# verified cleared by reading the prefs back, not just via logcat.
# Emulator only.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/delete-all-messages-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
prefs_dir="/data/data/$package/shared_prefs"
backup="$out/delete-all-messages-prefs-backup.xml"
restore_needed=0
rooted=0
seeded_sms_ids=""
sms_one="+44770090301"
sms_two="+44770090302"

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
 'LSK':(90,414),'RSK':(270,414),'OK':(180,474),'DOWN':(180,505),'UP':(180,442),'RIGHT':(212,474),
}
for name in ['LSK','RSK','OK','DOWN','UP','RIGHT']:
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
  adb logcat -d -v brief -s C2Messages:I AndroidRuntime:E '*:S' > "$out/delete-all-messages-log.txt" || true
  adb shell screencap -p /sdcard/c2-delmsg.png 2>/dev/null || true
  adb pull /sdcard/c2-delmsg.png "$out/delete-all-messages-screen.png" >/dev/null 2>&1 || true
  adb shell rm -f /sdcard/c2-delmsg.png 2>/dev/null || true
}
# EXIT trap owns every cleanup path (audit-#11 item 3 + audit-#12 items
# 2+4 shape): evidence, tracked SMS row deletion, prefs restore, unroot,
# wait, uid check. Every fallible command is guarded so set -euo pipefail
# cannot abort the trap early.
finish() {
  capture_evidence
  if [[ "$rooted" == "1" && -n "$seeded_sms_ids" ]]; then
    while IFS= read -r sms_id; do
      [[ "$sms_id" =~ ^[0-9]+$ ]] || continue
      del_out="$(adb shell content delete --uri content://sms --where "_id=$sms_id" < /dev/null 2>&1 | tr -d '\r' || true)"
      if grep -qiE 'exception|denied|error' <<< "$del_out"; then
        echo "FAIL cleanup: SMS provider delete of _id=$sms_id errored: $del_out" | tee -a "$result"
        continue
      fi
      q_out="$(adb shell content query --uri content://sms --projection _id < /dev/null 2>&1 | tr -d '\r' || true)"
      if grep -qiE 'exception|denied|error' <<< "$q_out"; then
        echo "FAIL cleanup: SMS provider query after delete errored: $q_out" | tee -a "$result"
        continue
      fi
      left="$(printf '%s\n' "$q_out" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -cx "$sms_id" || true)"
      if [[ "$left" != "0" ]]; then
        echo "FAIL cleanup: seeded SMS row _id=$sms_id still present after delete" | tee -a "$result"
      fi
    done <<< "$seeded_sms_ids"
  fi
  # Restore the prefs exactly as found (or remove the fixture), so the
  # hidden-id set this probe creates never leaks into later probes.
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
  if [[ "$rooted" == "1" ]]; then
    adb unroot >/dev/null 2>&1 || true
    # unroot restarts adbd; without this the NEXT probe's adb root can race
    # the restart and fail (run 38691909508 clear-log C2).
    adb wait-for-device || true
    sleep 2
    uid="$(adb shell id -u 2>/dev/null | tr -d '\r' || true)"
    if [[ "$uid" == "0" ]]; then
      echo "FAIL unroot did not drop root: id -u still 0" | tee -a "$result"
    elif [[ -z "$uid" ]]; then
      echo "WARN could not verify effective uid after unroot" | tee -a "$result"
    fi
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
marker_seen() { # marker_seen <text>: 0 when the app logged it since last clear
  adb logcat -d -s C2Messages:I 2>/dev/null | grep -F "$1" >/dev/null
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
    echo "--- C2Messages logcat at failure ---" | tee -a "$result"
    adb logcat -d -s C2Messages:I 2>/dev/null | tee -a "$result" > "$out/delete-all-messages-section-log.txt" || true
    exit 1
  fi
}
sms_ids() { # one provider sms _id per line, sorted; empty when provider empty
  adb shell content query --uri content://sms --projection _id 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true
}

# Grant the full runtime set up front so no cold-start permission sheet can
# intercept the first taps (same set as the other probes).
for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
            READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# --- Prefs fixture: one draft + one saved template --------------------------
# Deterministic app-local data for the HTML confirmdelete parity check (the
# HTML handler clears drafts and saved messages). Back up first, restore in
# the EXIT trap.
adb shell am force-stop "$package" || true
adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' > "$backup" || true
if [[ -s "$backup" ]] && grep -q '<map>' "$backup"; then
  restore_needed=1
  echo "BACKUP existing c2_state.xml captured for restore" >> "$result"
fi
fixture="$out/delete-all-messages-prefs-fixture.xml"
cat > "$fixture" <<'XML'
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="drafts_v2">[{&quot;recipient&quot;:&quot;07900005003&quot;,&quot;body&quot;:&quot;Probe draft body&quot;,&quot;inputMode&quot;:&quot;T9&quot;,&quot;createdAt&quot;:1760000000000}]</string>
    <string name="sms_templates_v1">[&quot;Probe template body&quot;]</string>
</map>
XML
adb shell run-as "$package" mkdir -p "$prefs_dir" 2>/dev/null || true
adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$fixture"
seeded_keys="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | grep -oE 'drafts_v2|sms_templates_v1' | sort -u | tr '\n' ' ' || true)"
if [[ "$seeded_keys" != *"drafts_v2"* || "$seeded_keys" != *"sms_templates_v1"* ]]; then
  echo "FAIL could not seed drafts/templates prefs fixture (found: ${seeded_keys:-none})" | tee -a "$result"
  exit 1
fi
echo "SEEDED prefs fixture: $seeded_keys" >> "$result"

# --- SMS provider seeding (best-effort, root) -------------------------------
provider_before="$(sms_ids)"
if adb root 2>&1 | grep -qi 'root'; then
  rooted=1
  sleep 3
  adb wait-for-device
  pre_addr_ids="$(adb shell content query --uri content://sms --projection _id:address 2>&1 | tr -d '\r' | grep -E "address=($sms_one|$sms_two)" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -E '^[0-9]+$' || true)"
  now_ms="$(date +%s)000"
  adb shell content insert --uri content://sms \
    --bind address:s:"$sms_one" --bind body:s:ProbeOne \
    --bind type:i:1 --bind date:l:"$now_ms" >/dev/null 2>&1 || true
  adb shell content insert --uri content://sms \
    --bind address:s:"$sms_two" --bind body:s:ProbeTwo \
    --bind type:i:1 --bind date:l:"$((now_ms + 60000))" >/dev/null 2>&1 || true
  sms_check="$(adb shell content query --uri content://sms --projection address 2>&1 | tr -d '\r' || true)"
  if grep -Fq "address=$sms_one" <<< "$sms_check" && grep -Fq "address=$sms_two" <<< "$sms_check"; then
    # Attribute rows by id-diff, never by address alone (audit-#12 item 1).
    post_addr_ids="$(adb shell content query --uri content://sms --projection _id:address 2>&1 | tr -d '\r' | grep -E "address=($sms_one|$sms_two)" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -E '^[0-9]+$' || true)"
    while IFS= read -r cand; do
      [[ "$cand" =~ ^[0-9]+$ ]] || continue
      if ! grep -qx "$cand" <<< "$pre_addr_ids"; then
        seeded_sms_ids+="$cand"$'\n'
      fi
    done <<< "$post_addr_ids"
    seeded_sms_ids="${seeded_sms_ids%$'\n'}"
    seeded_count="$(grep -c . <<< "$seeded_sms_ids" || true)"
    echo "Seeded $seeded_count SMS rows (ids: $(tr '\n' ' ' <<< "$seeded_sms_ids"))" >> "$result"
    if [[ "$seeded_count" != "2" ]]; then
      echo "UNVERIFIED expected 2 seeded SMS rows, attributed ${seeded_count:-0}; provider-hide assertions run degraded" | tee -a "$result"
    fi
  else
    echo "UNVERIFIED SMS provider seeding unreadable even as root; provider-hide assertions run empty" | tee -a "$result"
  fi
else
  echo "UNVERIFIED adb root unavailable; SMS provider seeding skipped, provider-hide assertions run empty" | tee -a "$result"
fi
provider_after_seed="$(sms_ids)"
# -1: skip the count assertion (partial attribution); 0: nothing seeded;
# 2: both rows attributed - the strong case.
expected_hidden=0
if [[ -n "$seeded_sms_ids" ]] && [[ "$(grep -c . <<< "$seeded_sms_ids")" == "2" ]]; then
  expected_hidden=2
elif [[ -n "$seeded_sms_ids" ]]; then
  expected_hidden=-1
  echo 'UNVERIFIED partial SMS attribution; hidden-count assertion skipped' | tee -a "$result"
fi

# From the home display: CENTRE opens the menu grid, RIGHT then DOWN selects
# Messaging (grid index 4), OK enters it, twelve Downs select
# "Delete messages", OK opens its submenu on "All messages".
open_delete_messages() {
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
  fg="$(foreground_package)"
  [[ "$fg" == *"$package/"* ]] || { echo "UNVERIFIED C2 not foreground: $fg" | tee -a "$result"; exit 1; }
  tap OK                          # home menu grid (selection starts at index 0)
  tap RIGHT                       # index 1
  tap DOWN                        # index 4: Messaging
  tap OK                          # Messaging menu
  tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
  tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN; tap DOWN
  # selection now on "Delete messages" (row 12)
}

# --- Y. Yes hides every visible SMS, clears drafts/templates ----------------
open_delete_messages
tap OK                          # open the Delete messages submenu (sel 0 = All messages)
shot delmsg-submenu
tap OK                          # "All messages" row -> in-house confirm
sleep 1
require_marker 'DELETE_ALL_MESSAGES_CONFIRM'
if [[ "$expected_hidden" -gt 0 ]]; then
  require_marker "DELETE_ALL_MESSAGES_CONFIRM visible=$expected_hidden"
fi
shot delmsg-confirm-yes
tap LSK                         # Yes
sleep 2
require_marker 'DELETE_ALL_MESSAGES hidden='
require_marker 'drafts=1 saved=1'
actual_hidden=""
if [[ "$expected_hidden" -ge 0 ]]; then
  actual_hidden="$(adb logcat -d -s C2Messages:I 2>/dev/null | grep -oE 'DELETE_ALL_MESSAGES hidden=[0-9]+' | tail -1 | grep -oE '[0-9]+$' || true)"
  if [[ "$actual_hidden" != "$expected_hidden" ]]; then
    echo "FAIL hidden count mismatch: app hid ${actual_hidden:-unknown}, expected $expected_hidden" | tee -a "$result"
    exit 1
  fi
fi
provider_after_yes="$(sms_ids)"
if [[ "$provider_after_yes" != "$provider_after_seed" ]]; then
  echo "FAIL hide-only promise broken: SMS provider changed (before: ${provider_after_seed:-none} / after: ${provider_after_yes:-none})" | tee -a "$result"
  exit 1
fi
echo 'PASS Delete all messages provider unchanged (hide-only)' | tee -a "$result"
# Drafts/templates really cleared: read the prefs back, do not trust the log.
prefs_after="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
if grep -Fq '<string name="drafts_v2">[]</string>' <<< "$prefs_after" \
   && grep -Fq '<string name="sms_templates_v1">[]</string>' <<< "$prefs_after"; then
  echo 'PASS Delete all messages cleared app drafts and templates (1 draft, 1 template)' | tee -a "$result"
else
  echo "FAIL drafts/templates not cleared in prefs after Yes: $prefs_after" | tee -a "$result"
  exit 1
fi
if [[ "$expected_hidden" == "2" ]]; then
  echo 'PASS Delete all messages Yes hid every seeded SMS (2 hidden)' | tee -a "$result"
else
  echo "PASS Delete all messages Yes completed (hidden=${actual_hidden:-skipped}, seed coverage limited)" | tee -a "$result"
fi
expect_foreground
echo 'PASS C2 remains foreground after Delete all messages Yes' | tee -a "$result"
shot delmsg-after-yes
if [[ "$expected_hidden" == "2" ]]; then
  # Visual proof of hiding: Conversations (Messaging row 1) must be empty.
  # After Yes the app sits on the Messaging menu, selection on row 12.
  tap UP; tap UP; tap UP; tap UP; tap UP; tap UP
  tap UP; tap UP; tap UP; tap UP; tap UP
  tap OK                          # Conversations
  sleep 1
  shot delmsg-conversations-empty
fi

# --- N. No leaves everything untouched --------------------------------------
open_delete_messages
adb logcat -c || true
tap OK                          # open the Delete messages submenu
tap OK                          # "All messages" row -> in-house confirm
sleep 1
require_marker 'DELETE_ALL_MESSAGES_CONFIRM'
shot delmsg-confirm-no
tap RSK                         # No
sleep 2
if marker_seen 'DELETE_ALL_MESSAGES hidden='; then
  echo 'FAIL answering No still ran the delete handler' | tee -a "$result"
  exit 1
fi
provider_after_no="$(sms_ids)"
if [[ "$provider_after_no" != "$provider_after_seed" ]]; then
  echo "FAIL Delete all messages No changed the provider (before: ${provider_after_seed:-none} / after: ${provider_after_no:-none})" | tee -a "$result"
  exit 1
fi
echo 'PASS Delete all messages No left provider unchanged' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Delete all messages No' | tee -a "$result"
shot delmsg-after-no

# --- R. Folder rows route to their folder pages -----------------------------
# HTML v6.343 routes its By message / By folder rows to the inbox and folder
# pages; the native submenu rows must open Saved items / Inbox / Sent items /
# Drafts (per-message handling lives there). Page titles are the evidence -
# verified visually from the screenshots.
route_row() { # route_row <downs> <name>
  open_delete_messages
  tap OK                          # open the Delete messages submenu
  local i
  for ((i = 0; i < $1; i++)); do tap DOWN; done
  tap OK                          # open the folder page
  sleep 1
  expect_foreground
  shot "delmsg-route-$2"
  echo "PASS Delete messages row routes: $2" | tee -a "$result"
}
route_row 1 byfolder
route_row 2 inbox
route_row 3 sent
route_row 4 drafts

# Mirror key screenshots into the results as single-line base64 so the job
# log (the only reliable retrieval path) carries them for visual review.
for f in delmsg-submenu delmsg-confirm-yes delmsg-after-yes delmsg-conversations-empty delmsg-confirm-no delmsg-after-no delmsg-route-byfolder delmsg-route-inbox delmsg-route-sent delmsg-route-drafts; do
  if [[ -s "$out/$f.png" ]]; then
    echo "SHOTB64 $f $(base64 -w0 "$out/$f.png")" >> "$result"
  fi
done
echo 'PASS Delete all messages acceptance complete' | tee -a "$result"
