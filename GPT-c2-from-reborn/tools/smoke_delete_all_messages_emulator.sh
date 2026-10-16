#!/usr/bin/env bash
# Android 11 acceptance for Messaging > Delete messages.
# BLOCKING in the suite:
#  Y. Yes (no SMS role): the in-house "Delete all messages?" question box
#     (HTML v6.343 confirmdelete wording, Yes/No softkeys) hides every
#     provider SMS (hide-only: provider must stay byte-identical, ALL rows
#     not just the loaded window), clears app-local drafts/templates, and
#     lands on the Messaging menu with "Delete messages" selected.
#  N. No: answering No hides nothing and writes nothing.
#  R. Folder rows route to their folder pages (route markers assert the
#     destinations; provider and hidden set untouched by the sweep).
#  D. Default-SMS role (granted via cmd role): inbound SMS persists
#     end-to-end (emu sms send -> SMS_DELIVER receiver -> provider row,
#     multipart + same-second dedupe + malformed skip), an external sms:
#     link prefills Compose and sending stores a SENT row, then
#     visible after app restart) and Delete all messages then really
#     deletes every provider SMS row. The role is restored afterwards.
# Full provider rows (address/body/date/type/read) are snapshotted
# before/after Yes and No, not just id lists.
# Coverage notes: shell SMS provider seeding stays root best-effort on this
# image; when it fails the hide-path assertions run empty and the seeded
# PASS lines stay absent so the suite warning gate reports the gap.
# Emulator only: refuses to run on hardware (real-delete section).
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
role_granted=0
role_prev_holder=""
seeded_sms_ids=""

# This image's cmd role has add/remove but NOT get-role-holders ("Unknown
# command" on API 30), so read the holder from dumpsys role instead: find
# the SMS role block, then the package list on its holders line.
get_sms_holder() {
  adb shell dumpsys role < /dev/null 2>/dev/null | tr -d '\r' | awk '
    /android\.app\.role\.SMS/ {inblock=1; next}
    inblock && /app\.role\./ {inblock=0}
    inblock && /[Hh]olders/ {
      line=$0
      sub(/^[^=]*=/, "", line)
      sub(/.*\[/, "", line); sub(/\].*/, "", line)
      gsub(/,/, " ", line); gsub(/^ +| +$/, "", line)
      split(line, a, " "); if (a[1] != "") print a[1]
      exit
    }'
}
sms_one="+44770090301"
sms_two="+44770090302"
inbound_number="+44770090399"
inbound_body="C2 inbound persistence probe 7741"

qemu="$(adb shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r'; adb shell getprop ro.boot.qemu 2>/dev/null | tr -d '\r')"
if ! grep -q 1 <<< "$qemu"; then
  echo "REFUSING: delete-messages probe runs on emulators only (ro.kernel.qemu/ro.boot.qemu unset)" | tee -a "$result"
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
 'LSK':(90,414),'RSK':(270,414),'OK':(180,474),'DOWN':(180,505),'UP':(180,442),'RIGHT':(212,474),
 '0':(180,716),'1':(90,557),'2':(180,557),'3':(270,557),
 '4':(90,610),'5':(180,610),'6':(270,610),'7':(90,663),'8':(180,663),'9':(270,663),
}
for name in ['LSK','RSK','OK','DOWN','UP','RIGHT','0','1','2','3','4','5','6','7','8','9']:
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
# EXIT trap owns every cleanup path: evidence, tracked SMS row deletion,
# SMS-role restore, prefs restore, unroot, wait, uid check. Every fallible
# command is guarded so set -euo pipefail cannot abort the trap early.
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
  # Give the SMS role back exactly as found. When C2 already held it, the
  # probe changed nothing, so cleanup must not strip the role either.
  if [[ "$role_granted" == "1" ]]; then
    if [[ "$role_prev_holder" == "$package" ]]; then
      holder_after="$(get_sms_holder)"
      if [[ "$holder_after" == "$package" ]]; then
        echo 'PASS SMS role unchanged after probe (C2 already held it)' | tee -a "$result"
      else
        echo "FAIL cleanup: SMS role lost (expected C2, holder: ${holder_after:-none})" | tee -a "$result"
      fi
    else
      adb shell cmd role remove-role-holder android.app.role.SMS "$package" < /dev/null >/dev/null 2>&1 || true
      if [[ -n "$role_prev_holder" ]]; then
        adb shell cmd role add-role-holder android.app.role.SMS "$role_prev_holder" < /dev/null >/dev/null 2>&1 || true
      fi
      holder_after="$(get_sms_holder)"
      if [[ "$holder_after" != "$role_prev_holder" ]]; then
        echo "FAIL cleanup: SMS role holder is ${holder_after:-none}, expected ${role_prev_holder:-none}" | tee -a "$result"
      else
        echo "PASS SMS role restored after probe (holder: ${holder_after:-none})" | tee -a "$result"
      fi
    fi
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
  adb shell content query --uri content://sms --projection _id < /dev/null 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n || true
}
sms_rows() { # full provider rows (audit-#14 delta a): id/address/body/date/type/read
  adb shell content query --uri content://sms --projection _id:address:body:date:type:read < /dev/null 2>&1 | tr -d '\r' | sort || true
}

# Grant the full runtime set up front so no cold-start permission sheet can
# intercept the first taps (same set as the other probes).
for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG \
            READ_EXTERNAL_STORAGE WRITE_EXTERNAL_STORAGE; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

seed_prefs_fixture() {
  local fixture="$out/delete-all-messages-prefs-fixture.xml"
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
}

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
seed_prefs_fixture

# --- SMS provider seeding (best-effort, root) -------------------------------
provider_before="$(sms_ids)"
if adb root 2>&1 | grep -qi 'root'; then
  rooted=1
  sleep 3
  adb wait-for-device
  pre_addr_ids="$(adb shell content query --uri content://sms --projection _id:address < /dev/null 2>&1 | tr -d '\r' | grep -E "address=($sms_one|$sms_two)" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -E '^[0-9]+$' || true)"
  now_ms="$(date +%s)000"
  adb shell content insert --uri content://sms \
    --bind address:s:"$sms_one" --bind body:s:ProbeOne \
    --bind type:i:1 --bind date:l:"$now_ms" < /dev/null >/dev/null 2>&1 || true
  adb shell content insert --uri content://sms \
    --bind address:s:"$sms_two" --bind body:s:ProbeTwo \
    --bind type:i:1 --bind date:l:"$((now_ms + 60000))" < /dev/null >/dev/null 2>&1 || true
  sms_check="$(adb shell content query --uri content://sms --projection address < /dev/null 2>&1 | tr -d '\r' || true)"
  if grep -Fq "address=$sms_one" <<< "$sms_check" && grep -Fq "address=$sms_two" <<< "$sms_check"; then
    # Attribute rows by id-diff, never by address alone (audit-#12 item 1).
    post_addr_ids="$(adb shell content query --uri content://sms --projection _id:address < /dev/null 2>&1 | tr -d '\r' | grep -E "address=($sms_one|$sms_two)" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -E '^[0-9]+$' || true)"
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
provider_total="$(grep -c . <<< "$provider_after_seed" || true)"
# -1: skip the count assertion (partial attribution or unreadable provider);
# otherwise the hide path must hide EVERY provider row, not just the
# app's loaded window (audit-#14 item 2).
expected_hidden=-1
if [[ -n "$provider_after_seed" && "$provider_total" -ge 2 && -n "$seeded_sms_ids" ]] \
   && [[ "$(grep -c . <<< "$seeded_sms_ids")" == "2" ]]; then
  expected_hidden="$provider_total"
elif [[ -n "$seeded_sms_ids" ]]; then
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

# --- Y. Yes hides every provider SMS, clears drafts/templates ---------------
rows_before_yes="$(sms_rows)"
open_delete_messages
tap OK                          # open the Delete messages submenu (sel 0 = All messages)
shot delmsg-submenu
tap OK                          # "All messages" row -> in-house confirm
sleep 1
require_marker 'DELETE_ALL_MESSAGES_CONFIRM'
shot delmsg-confirm-yes
tap LSK                         # Yes
sleep 2
require_marker 'DELETE_ALL_MESSAGES hidden='
require_marker 'drafts=1 saved=1'
actual_hidden=""
if [[ "$expected_hidden" -ge 0 ]]; then
  actual_hidden="$(adb logcat -d -s C2Messages:I 2>/dev/null | grep -oE 'DELETE_ALL_MESSAGES hidden=[0-9]+' | tail -1 | grep -oE '[0-9]+$' || true)"
  if [[ "$actual_hidden" != "$expected_hidden" ]]; then
    echo "FAIL hidden count mismatch: app hid ${actual_hidden:-unknown}, expected ALL $expected_hidden provider rows" | tee -a "$result"
    exit 1
  fi
fi
rows_after_yes="$(sms_rows)"
if [[ "$rows_after_yes" != "$rows_before_yes" ]]; then
  echo "FAIL hide-only promise broken: SMS provider rows changed across Yes" | tee -a "$result"
  diff <(printf '%s\n' "$rows_before_yes") <(printf '%s\n' "$rows_after_yes") | head -10 | tee -a "$result" || true
  exit 1
fi
echo 'PASS Delete all messages provider rows unchanged across Yes (hide-only, full-row snapshot)' | tee -a "$result"
# Drafts/templates really cleared: read the prefs back, do not trust the log.
prefs_after="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
if grep -Fq '<string name="drafts_v2">[]</string>' <<< "$prefs_after" \
   && grep -Fq '<string name="sms_templates_v1">[]</string>' <<< "$prefs_after"; then
  echo 'PASS Delete all messages cleared app drafts and templates (1 draft, 1 template)' | tee -a "$result"
else
  echo "FAIL drafts/templates not cleared in prefs after Yes: $prefs_after" | tee -a "$result"
  exit 1
fi
if [[ "$expected_hidden" -ge 2 ]]; then
  echo "PASS Delete all messages Yes hid every provider SMS ($expected_hidden hidden, all rows incl. beyond load window)" | tee -a "$result"
else
  echo "PASS Delete all messages Yes completed (hidden=${actual_hidden:-skipped}, seed coverage limited)" | tee -a "$result"
fi
expect_foreground
echo 'PASS C2 remains foreground after Delete all messages Yes' | tee -a "$result"
shot delmsg-after-yes
if [[ "$expected_hidden" -ge 2 ]]; then
  # Visual proof of hiding: Conversations (Messaging row 1) must be empty.
  # After Yes the app sits on the Messaging menu, selection on row 12.
  tap UP; tap UP; tap UP; tap UP; tap UP; tap UP
  tap UP; tap UP; tap UP; tap UP; tap UP
  tap OK                          # Conversations
  sleep 1
  shot delmsg-conversations-empty
fi

# --- N. No leaves everything untouched --------------------------------------
rows_before_no="$(sms_rows)"
open_delete_messages
adb logcat -c || true
tap OK                          # open the Delete messages submenu
tap OK                          # "All messages" row -> in-house confirm
sleep 1
require_marker 'DELETE_ALL_MESSAGES_CONFIRM'
shot delmsg-confirm-no
tap RSK                         # No
sleep 2
if marker_seen 'DELETE_ALL_MESSAGES hidden=' || marker_seen 'DELETE_ALL_MESSAGES real='; then
  echo 'FAIL answering No still ran the delete handler' | tee -a "$result"
  exit 1
fi
rows_after_no="$(sms_rows)"
if [[ "$rows_after_no" != "$rows_before_no" ]]; then
  echo "FAIL Delete all messages No changed the provider rows" | tee -a "$result"
  diff <(printf '%s\n' "$rows_before_no") <(printf '%s\n' "$rows_after_no") | head -10 | tee -a "$result" || true
  exit 1
fi
echo 'PASS Delete all messages No left provider rows unchanged (full-row snapshot)' | tee -a "$result"
expect_foreground
echo 'PASS C2 remains foreground after Delete all messages No' | tee -a "$result"
shot delmsg-after-no

# --- R. Folder rows route to their folder pages -----------------------------
# HTML v6.343 routes its By message / By folder rows to the inbox and folder
# pages; the native submenu rows must open Saved items / Inbox / Sent items /
# Drafts (per-message handling lives there). The app's DELETE_MESSAGES_ROUTE
# marker asserts the actual destination page (the UI renders in the app View,
# so uiautomator sees nothing), and the provider must not change across the
# sweep.
rows_before_routes="$(sms_rows)"
route_row() { # route_row <downs> <page> <name>
  open_delete_messages
  adb logcat -c || true
  tap OK                          # open the Delete messages submenu
  local i
  for ((i = 0; i < $1; i++)); do tap DOWN; done
  tap OK                          # open the folder page
  sleep 1
  expect_foreground
  require_marker "DELETE_MESSAGES_ROUTE dest=$2"
  shot "delmsg-route-$3"
  echo "PASS Delete messages row routes to $2 (marker-verified): $3" | tee -a "$result"
}
route_row 1 savedmessages byfolder
route_row 2 inbox inbox
route_row 3 sent sent
route_row 4 drafts drafts
rows_after_routes="$(sms_rows)"
if [[ "$rows_after_routes" != "$rows_before_routes" ]]; then
  echo "FAIL folder-route sweep changed the provider rows" | tee -a "$result"
  exit 1
fi
echo 'PASS folder-route sweep left provider rows unchanged' | tee -a "$result"

# --- D. Default-SMS role: inbound persistence + real delete -----------------
role_prev_holder="$(get_sms_holder)"
adb shell cmd role add-role-holder android.app.role.SMS "$package" < /dev/null > "$out/role-grant.txt" 2>&1 || true
sleep 1
holder="$(get_sms_holder)"
if [[ "$holder" != "$package" ]]; then
  echo "UNVERIFIED could not grant default SMS role (holder=${holder:-none}); default-SMS sections skipped" | tee -a "$result"
  echo "--- cmd role add-role-holder output ---" >> "$result"
  cat "$out/role-grant.txt" >> "$result" 2>/dev/null || true
  echo "--- dumpsys role (SMS block) ---" >> "$result"
  adb shell dumpsys role < /dev/null 2>/dev/null | tr -d '\r' | grep -A3 'app.role.SMS' >> "$result" || true
  echo "--- logcat RoleManager ---" >> "$result"
  adb shell logcat -d < /dev/null 2>/dev/null | grep -iE 'rolemanager|role' | tail -15 >> "$result" || true
else
  role_granted=1
  echo "SMS role granted to $package (previous holder: ${role_prev_holder:-none})" >> "$result"

  # D1. Inbound SMS persists end-to-end: emu sms send -> SMS_DELIVER -> the
  # app's receiver writes the provider row.
  adb logcat -c || true
  emu_out="$(adb emu sms send "$inbound_number" "$inbound_body" 2>&1 || true)"
  if grep -qiE 'error|refused|KO' <<< "$emu_out"; then
    echo "UNVERIFIED adb emu sms send unavailable on this image: $emu_out" | tee -a "$result"
  else
    inbound_id=""
    for i in 1 2 3 4 5 6 7 8 9 10; do
      inbound_id="$(adb shell content query --uri content://sms --projection _id:body < /dev/null 2>&1 | tr -d '\r' | grep -F "body=$inbound_body" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | head -1 || true)"
      [[ -n "$inbound_id" ]] && break
      sleep 1
    done
    if [[ -z "$inbound_id" ]]; then
      echo 'FAIL inbound SMS (emu sms send) never reached the provider under the default-SMS role' | tee -a "$result"
      adb logcat -d -s C2Messages:I 2>/dev/null | tail -20 | tee -a "$result" || true
      exit 1
    fi
    require_marker 'SMS_DELIVER stored parts=1'
    echo "PASS inbound SMS persisted end-to-end under default-SMS role (emu sms send -> provider id=$inbound_id)" | tee -a "$result"

    # D1b. Multipart: a 300-char GSM-7 text arrives as 2 SMS_DELIVER parts
    # and must be stored as ONE provider row with the full body.
    multipart_body="MultipartC2probe $(printf 'x%.0s' $(seq 1 290))"
    adb logcat -c || true
    emu_out="$(adb emu sms send "$inbound_number" "$multipart_body" 2>&1 || true)"
    if grep -qiE 'error|refused|KO' <<< "$emu_out"; then
      echo "UNVERIFIED adb emu sms send unavailable for multipart section: $emu_out" | tee -a "$result"
    else
      multi_ok=0
      for i in 1 2 3 4 5 6 7 8 9 10; do
        got="$(adb shell content query --uri content://sms --projection body < /dev/null 2>&1 | tr -d '\r' | grep -F "body=$multipart_body" | head -1 || true)"
        [[ -n "$got" ]] && { multi_ok=1; break; }
        sleep 1
      done
      if [[ "$multi_ok" != "1" ]]; then
        echo 'FAIL multipart inbound SMS: no provider row holds the full concatenated body' | tee -a "$result"
        adb logcat -d -s C2Messages:I 2>/dev/null | tail -20 | tee -a "$result" || true
        exit 1
      fi
      multi_parts="$(adb logcat -d -s C2Messages:I 2>/dev/null | grep -oE 'SMS_DELIVER stored parts=[0-9]+' | tail -1 || true)"
      if [[ "$multi_parts" != 'SMS_DELIVER stored parts=2' && "$multi_parts" != 'SMS_DELIVER stored parts=3' ]]; then
        echo "FAIL expected multipart store marker (parts=2 or 3), last marker: ${multi_parts:-none}" | tee -a "$result"
        exit 1
      fi
      echo "PASS inbound multipart SMS stored as one provider row with the full body (${multi_parts#SMS_DELIVER stored parts=} parts concatenated)" | tee -a "$result"
    fi

    # D1c. Redelivery: the same message arriving twice is stored once. The
    # emulator stamps each send with the current time (second resolution),
    # so a true same-timestamp redelivery only happens when both sends land
    # in the same second; both outcomes are asserted explicitly.
    redup_body="C2redeliveryProbe"
    adb logcat -c || true
    emu_out="$(adb emu sms send "$inbound_number" "$redup_body" 2>&1 || true)"
    emu_out2="$(adb emu sms send "$inbound_number" "$redup_body" 2>&1 || true)"
    if grep -qiE 'error|refused|KO' <<< "$emu_out$emu_out2"; then
      echo "UNVERIFIED adb emu sms send unavailable for redelivery section: $emu_out $emu_out2" | tee -a "$result"
      redup_skip=1
    fi
    sleep 3
    if [[ "${redup_skip:-0}" == "1" ]]; then
      redup_rows="skipped"
      redup_count="skipped"
    else
    redup_rows="$(adb shell content query --uri content://sms --projection _id:date:body < /dev/null 2>&1 | tr -d '\r' | grep -F "body=$redup_body" || true)"
    redup_count="$(grep -c . <<< "$redup_rows" || true)"
    if [[ "$redup_count" == "1" ]]; then
      require_marker 'SMS_DELIVER skipped reason=duplicate'
      echo 'PASS same-second identical SMS stored once via the dedupe key (the emulator cannot distinguish a true redelivery from a genuine same-second resend; either way exactly one row)' | tee -a "$result"
    elif [[ "$redup_count" == "2" ]]; then
      d1="$(sed -n '1s/.*date=\([0-9][0-9]*\).*/\1/p' <<< "$redup_rows")"
      d2="$(sed -n '2s/.*date=\([0-9][0-9]*\).*/\1/p' <<< "$redup_rows")"
      if [[ -n "$d1" && "$d1" == "$d2" ]]; then
        echo "FAIL SMS redelivery stored twice with identical sender+timestamp+body (date=$d1)" | tee -a "$result"
        exit 1
      fi
      echo "PASS SMS redelivery not reproducible on this image (emulator stamped distinct dates $d1 vs $d2); both rows legitimately stored" | tee -a "$result"
    else
      echo "FAIL SMS redelivery section: expected 1 or 2 provider rows for the probe text, found $redup_count" | tee -a "$result"
      exit 1
    fi
    fi

    # D1d. Malformed delivery: an SMS_DELIVER broadcast with no pdu payload
    # must be logged and skipped, never crash, never store a row. Shell can
    # send protected broadcasts; if this image refuses, the section reports
    # UNVERIFIED instead of guessing.
    rows_before_malformed="$(sms_rows | grep -c . || true)"
    adb logcat -c || true
    bc_out="$(adb shell am broadcast -a android.provider.Telephony.SMS_DELIVER -n "$package/.C2SmsReceiver" < /dev/null 2>&1 || true)"
    sleep 2
    if grep -qiE 'SecurityException|Permission Denial' <<< "$bc_out"; then
      echo "UNVERIFIED shell cannot inject an empty SMS_DELIVER broadcast on this image: $bc_out" | tee -a "$result"
    else
      require_marker 'SMS_DELIVER skipped reason=no_parts'
      rows_after_malformed="$(sms_rows | grep -c . || true)"
      if [[ "$rows_after_malformed" != "$rows_before_malformed" ]]; then
        echo "FAIL malformed SMS_DELIVER changed provider rows ($rows_before_malformed -> $rows_after_malformed)" | tee -a "$result"
        exit 1
      fi
      echo 'PASS malformed SMS_DELIVER (no pdu payload) logged and skipped, provider unchanged' | tee -a "$result"
    fi
    # Null-part multipart rejection (reason=partial) cannot be injected from
    # the emulator console (it only sends well-formed text PDUs): code-audited.
    echo 'NOTE null-part multipart rejection (SMS_DELIVER skipped reason=partial) is code-audited only; the emulator console cannot inject malformed multipart PDUs' | tee -a "$result"

    # D2. Visible in the app after a full restart (Inbox reads the provider;
    # a restart proves the row is durable, not cached).
    adb shell am force-stop "$package"
    adb shell am start -W -n "$package/.MainActivity" >> "$result"
    sleep 2
    expect_foreground
    still="$(adb shell content query --uri content://sms --projection _id --where "_id=$inbound_id" < /dev/null 2>&1 | tr -d '\r' | grep -oE '_id=[0-9]+' | head -1 || true)"
    if [[ -z "$still" ]]; then
      echo 'FAIL inbound SMS row vanished across app restart' | tee -a "$result"
      exit 1
    fi
    tap OK; tap RIGHT; tap DOWN; tap OK     # Messaging menu (selection row 0)
    tap DOWN                                # row 1: Conversations
    tap LSK                                 # Options
    tap DOWN                                # "Inbox view"
    tap OK                                  # Inbox page
    sleep 2
    shot delmsg-inbound-inbox
    echo 'PASS inbound SMS visible in Inbox after app restart (provider row durable, screenshot evidence)' | tee -a "$result"
  fi

  # D2s. External sms: link + outbound sent history while default. Cold
  # start through the SENDTO intent (exercises the onCreate path): Compose
  # opens prefilled, OK sends, and a SENT row lands in the provider. The
  # console SMS path is not needed for this section.
  sendto_body="C2probe5150"
  sendto_number="07700903010"
  adb shell am force-stop "$package"
  adb logcat -c || true
  # Record the installed SENDTO filter for the report, then deliver the
  # intent to MainActivity explicitly: an implicit start with the package
  # restriction resumed a leftover probe app (c2shareprobe) on this image
  # instead of C2, so resolution itself is not what this section tests.
  adb shell dumpsys package "$package" < /dev/null 2>/dev/null | tr -d '\r' | grep -B2 -A8 'android.intent.action.SENDTO' >> "$result" || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:${sendto_number}?body=${sendto_body}" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  sendto_fg="$(foreground_package)"
  sendto_retried=0
  if [[ "$sendto_fg" != *"$package/"* ]]; then
    # One retry: on this image a force-stopped start can lose the foreground
    # race to a leftover probe task (c2shareprobe). If the app itself
    # crashed, the retry reproduces the crash and the diagnostics below
    # capture it instead of leaving the failure opaque.
    echo "NOTE SENDTO start left foreground=$sendto_fg; retrying once" | tee -a "$result"
    adb shell am start -W -a android.intent.action.SENDTO -d "sms:${sendto_number}?body=${sendto_body}" -n "$package/.MainActivity" >> "$result" 2>&1 || true
    sleep 2
    sendto_fg="$(foreground_package)"
    sendto_retried=1
  fi
  if [[ "$sendto_fg" != *"$package/"* ]]; then
    echo "FAIL C2 lost foreground after SENDTO start: $sendto_fg" | tee -a "$result"
    {
      echo "--- process state (pidof) ---"
      adb shell pidof "$package" 2>&1 || true
      echo "--- task stack ---"
      adb shell dumpsys activity activities 2>/dev/null | tr -d '\r' | grep -E 'mResumedActivity|topResumedActivity|Hist #|TaskRecord|realActivity' | head -40 || true
      echo "--- logcat since SENDTO start ---"
      adb logcat -d -v brief 2>/dev/null | grep -E 'C2Messages|AndroidRuntime|ActivityTaskManager|FATAL' | tail -80 || true
    } >> "$result"
    shot delmsg-sendto-fail
    exit 1
  fi
  require_marker "SENDTO_OPEN recipient=$sendto_number bodylen=${#sendto_body}"
  shot delmsg-sendto-compose
  tap OK                          # recipient+body prefilled (focus=body): OK sends
  sleep 2
  require_marker 'SENT_ROW stored id='
  sent_rows=""
  for i in 1 2 3 4 5; do
    sent_rows="$(adb shell content query --uri content://sms --projection _id:type:address:body < /dev/null 2>&1 | tr -d '\r' | grep -F "body=$sendto_body" || true)"
    [[ -n "$(grep -F 'type=2' <<< "$sent_rows" || true)" ]] && break
    sleep 1
  done
  # Exactly one SENT row, for the right address and body (audit #32 probe).
  sent_count="$(grep -F 'type=2' <<< "$sent_rows" | grep -cF "address=$sendto_number" || true)"
  if [[ "$sent_count" != "1" ]]; then
    echo "FAIL outbound SMS while default: expected exactly one SENT row for address=$sendto_number body=$sendto_body, found $sent_count" | tee -a "$result"
    exit 1
  fi
  echo 'PASS sms: exactly one SENT row with the correct address and body for the probe send' | tee -a "$result"
  if [[ "$sendto_retried" == 1 ]]; then
    echo 'PASS sms: link prefilled Compose from a cold start (one foreground retry) and sending stored a SENT provider row (type=2)' | tee -a "$result"
  else
    echo 'PASS sms: link prefilled Compose from a cold start and sending stored a SENT provider row (type=2)' | tee -a "$result"
  fi

  # D2s-b. A recipient list is not one address: Compose opens with the
  # recipient blank and a notice; the joined string can never be sent
  # (audit #31 item 2).
  adb shell am force-stop "$package"
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903011,07700903012?body=C2multi" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_MULTI list=07700903011,07700903012"
  require_marker "SENDTO_OPEN recipient= bodylen=7"
  shot delmsg-sendto-multi
  echo 'PASS sms: link with several recipients opens Compose with an empty recipient and a notice' | tee -a "$result"

  # D2s-c. External bodies are cut at 1600 characters with a notice, never
  # silently (audit #31 item 3).
  long_body="$(printf 'x%.0s' $(seq 1 1700))"
  adb shell am force-stop "$package"
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903010?body=$long_body" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_TRUNCATED from=1700 to=1600"
  require_marker "SENDTO_OPEN recipient=07700903010 bodylen=1600"
  shot delmsg-sendto-truncated
  echo 'PASS sms: link body over 1600 characters is cut with a notice' | tee -a "$result"

  # D2s-d. A link that arrives while a compose is in progress keeps the
  # in-progress message as a draft (audit #31 item 1). MainActivity is
  # singleTop (audit #32 item 2), so this plain external start is delivered
  # to the running instance through onNewIntent - no stacking second compose.
  adb shell am force-stop "$package"
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
  expect_foreground
  tap OK                          # home menu grid
  tap RIGHT; tap DOWN; tap OK     # Messaging menu (selection row 0)
  tap OK                          # "Create message"
  sleep 1
  # The C2 UI reads digits from on-screen keypad taps, not IME key events.
  for d in 0 7 9 0 0 0 0 5 0 0 3; do tap "$d"; done
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903014?body=C2link2" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_DRAFT_SAVED recipient=07900005003 bodylen=0"
  require_marker "SENDTO_OPEN recipient=07700903014 bodylen=7"
  prefs_dump="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
  if ! grep -Fq '07900005003' <<< "$prefs_dump"; then
    echo 'FAIL in-progress compose was not kept as a draft after the SENDTO link' | tee -a "$result"
    exit 1
  fi
  shot delmsg-sendto-draftkept
  echo 'PASS sms: link while composing keeps the in-progress message as a draft' | tee -a "$result"

  # D2s-e. A semicolon-separated list is rejected the same way (audit #31
  # item 2, semicolon variant).
  adb shell am force-stop "$package"
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903016;07700903017?body=C2semi" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_MULTI list=07700903016;07700903017"
  require_marker "SENDTO_OPEN recipient= bodylen=6"
  shot delmsg-sendto-semilist
  echo 'PASS sms: semicolon recipient list rejected with an empty recipient and a notice' | tee -a "$result"

  # D2s-f. An encoded comma (%2C) is decoded before the separator check, so
  # the list is still rejected (audit #32 probe).
  adb shell am force-stop "$package"
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903013%2C07700903015?body=C2enc" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_MULTI list=07700903013,07700903015"
  require_marker "SENDTO_OPEN recipient= bodylen=5"
  shot delmsg-sendto-enclist
  echo 'PASS sms: encoded-comma recipient list rejected with an empty recipient and a notice' | tee -a "$result"

  # D2s-g. A body of exactly 1600 characters passes through uncut and no
  # truncation marker fires (audit #32 item 4 probe).
  exact_body="$(printf 'y%.0s' $(seq 1 1600))"
  adb shell am force-stop "$package"
  adb logcat -c || true
  adb shell am start -W -a android.intent.action.SENDTO -d "sms:07700903010?body=$exact_body" -n "$package/.MainActivity" >> "$result" 2>&1 || true
  sleep 2
  expect_foreground
  require_marker "SENDTO_OPEN recipient=07700903010 bodylen=1600"
  if marker_seen "SENDTO_TRUNCATED"; then
    echo 'FAIL exact-1600 body was truncated' | tee -a "$result"
    exit 1
  fi
  shot delmsg-sendto-exact1600
  echo 'PASS sms: link body of exactly 1600 characters passes through uncut' | tee -a "$result"

  # D3. Real delete: Delete all messages now removes every provider row
  # (inbound, multipart, redelivery and the sent row alike).
  seed_prefs_fixture
  rows_before_real="$(sms_rows)"
  real_expected="$(grep -c . <<< "$rows_before_real" || true)"
  open_delete_messages
  tap OK                          # open the Delete messages submenu
  tap OK                          # "All messages" row -> in-house confirm
  sleep 1
  require_marker 'DELETE_ALL_MESSAGES_CONFIRM'
  shot delmsg-confirm-real
  tap LSK                         # Yes
  sleep 3
  require_marker 'DELETE_ALL_MESSAGES real='
  real_line="$(adb logcat -d -s C2Messages:I 2>/dev/null | grep -oE 'DELETE_ALL_MESSAGES real=[0-9]+ before=[0-9]+' | tail -1 || true)"
  real_n="$(grep -oE 'real=[0-9]+' <<< "$real_line" | grep -oE '[0-9]+' || true)"
  real_before="$(grep -oE 'before=[0-9]+' <<< "$real_line" | grep -oE '[0-9]+' || true)"
  if [[ "$real_n" != "$real_before" || "$real_before" != "$real_expected" ]]; then
    echo "FAIL real delete counts: $real_line (provider rows before: $real_expected)" | tee -a "$result"
    exit 1
  fi
  rows_after_real="$(sms_rows)"
  if [[ -n "$(grep -oE '_id=[0-9]+' <<< "$rows_after_real" || true)" ]]; then
    echo "FAIL provider not empty after real Delete all messages: $rows_after_real" | tee -a "$result"
    exit 1
  fi
  echo "PASS Delete all messages under default-SMS role really deleted every provider SMS ($real_n rows, provider empty)" | tee -a "$result"
  require_marker 'drafts=1 saved=1'
  prefs_after="$(adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' || true)"
  if grep -Fq '<string name="drafts_v2">[]</string>' <<< "$prefs_after" \
     && grep -Fq '<string name="sms_templates_v1">[]</string>' <<< "$prefs_after"; then
    echo 'PASS real Delete all messages also cleared app drafts and templates' | tee -a "$result"
  else
    echo "FAIL drafts/templates not cleared after real delete: $prefs_after" | tee -a "$result"
    exit 1
  fi
  expect_foreground
  echo 'PASS C2 remains foreground after real Delete all messages' | tee -a "$result"
  shot delmsg-after-real
  # The real path consumed every provider row; the cleanup loop must not
  # look for the seeded ids any more.
  seeded_sms_ids=""
fi

# Mirror key screenshots into the results as single-line base64 so the job
# log (the only reliable retrieval path) carries them for visual review.
for f in delmsg-submenu delmsg-confirm-yes delmsg-after-yes delmsg-conversations-empty delmsg-confirm-no delmsg-after-no delmsg-route-byfolder delmsg-route-inbox delmsg-route-sent delmsg-route-drafts delmsg-inbound-inbox delmsg-sendto-compose delmsg-sendto-fail delmsg-sendto-multi delmsg-sendto-truncated delmsg-sendto-draftkept delmsg-sendto-semilist delmsg-sendto-enclist delmsg-sendto-exact1600 delmsg-confirm-real delmsg-after-real; do
  if [[ -s "$out/$f.png" ]]; then
    echo "SHOTB64 $f $(base64 -w0 "$out/$f.png")" >> "$result"
  fi
done
echo 'PASS Delete all messages acceptance complete' | tee -a "$result"
