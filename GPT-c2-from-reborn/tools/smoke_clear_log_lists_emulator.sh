#!/usr/bin/env bash
# Android 11 acceptance for Log > Clear log lists (v2.03.8). Both sections are
# BLOCKING in the suite:
#  C1. Message recipients: with one seeded recipient, Clear log lists >
#      Message recipients + confirm clears the list (marker
#      CLEAR_LOG_LISTS action=cllRecipients) and Log > Msg. recipients then
#      reports empty (marker MSG_RECIPIENTS_EMPTY).
#  C2. Missed calls: with one provider-seeded missed call, Clear log lists >
#      Missed calls + confirm hides it in the app (marker
#      CLEAR_LOG_LISTS action=cllMissed hidden=1) while the Android call-log
#      provider row stays untouched; the Missed calls page is screenshotted
#      empty afterwards.
# The app's private c2_state.xml is backed up and restored via trap; the
# provider row is deleted at the end.
set -uo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/clear-log-lists-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
seed_number="07900001234"
missed_number="07123456789"
section="boot"
failures=0
inserted_id_global=""

size="$(adb shell wm size | sed -n 's/.*: //p' | tail -1 | tr -d '\r')"
if [[ ! "$size" =~ ^([0-9]+)x([0-9]+)$ ]]; then
  echo 'UNVERIFIED display geometry unavailable' | tee -a "$result"; exit 1
fi
mapfile -t points < <(python3 - "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}" <<'PY'
import sys
w,h=map(int,sys.argv[1:])
scale=min(w/360,h/780)
ox=(w-360*scale)/2
oy=(h-780*scale)/2
named={
 'LSK':(90,414),'RSK':(270,414),'OK':(180,474),'DOWN':(180,505),'UP':(180,443),
}
for name in ['LSK','RSK','OK','DOWN','UP']:
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
shot() {
  adb exec-out screencap -p > "$out/$1.png" 2>/dev/null || true
}
marker_seen() {
  adb logcat -d -s C2Log:I 2>/dev/null | grep -qF "$1"
}
require_marker() {
  local tries=0
  while ! marker_seen "$1"; do
    tries=$((tries + 1))
    if [[ "$tries" -ge 6 ]]; then break; fi
    sleep 1
  done
  if ! marker_seen "$1"; then
    echo "FAIL[$section] expected app log marker: $1" | tee -a "$result"
    adb logcat -d -s C2Log:I 2>/dev/null > "$out/clear-log-lists-section-log.txt" || true
    shot "clear-log-lists-fail-$section"
    failures=$((failures + 1))
    return 1
  fi
  return 0
}
launch() {
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
}
open_log_page() { # home: CENTRE(OK) opens the menu; grid Down Down > OK = Log
  tap OK
  tap DOWN
  tap DOWN
  tap OK
  sleep 1
}
open_clear_page() { # on the Log page: row 5 = Clear log lists
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK
  sleep 1
}

prefs_dir="/data/data/$package/shared_prefs"
backup="$out/c2_state_backup_cll.xml"
restore_needed=0
cleanup_state() {
  adb shell am force-stop "$package" || true
  if [[ "$restore_needed" == "1" ]]; then
    adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup" || true
    if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | cmp -s - "$backup"; then
      echo "PASS original c2_state.xml restored after probe (bytes verified)" | tee -a "$result"
    else
      echo "FAIL restore of c2_state.xml did not verify byte-for-byte" | tee -a "$result"
      exit 1
    fi
  else
    adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
    echo "PASS prefs fixture removed after probe" | tee -a "$result"
  fi
  inserted_id_global="$(cat "$out/c2_inserted_id.txt" 2>/dev/null || true)"
  if [[ -n "$inserted_id_global" ]]; then
    adb shell content delete --uri content://call_log/calls --where "_id=$inserted_id_global" >/dev/null 2>&1 || true
    leftover="$(adb shell content query --uri content://call_log/calls --projection _id 2>&1 | tr -d '\r' | grep -c "_id=$inserted_id_global" || true)"
    rm -f "$out/c2_inserted_id.txt"
    if [[ "$leftover" != "0" ]]; then
      echo "FAIL cleanup: seeded call-log row _id=$inserted_id_global still present after delete" | tee -a "$result"
      exit 1
    fi
    echo "PASS seeded call-log row _id=$inserted_id_global deleted" | tee -a "$result"
  fi
  adb unroot >/dev/null 2>&1 || true
  adb wait-for-device || true
}
trap cleanup_state EXIT
adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' > "$backup" || true
if [[ -s "$backup" ]] && grep -q '<map>' "$backup"; then
  restore_needed=1
  echo "BACKUP existing c2_state.xml captured for restore" >> "$result"
fi

for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG WRITE_CALL_LOG; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# --- C1: clear Message recipients --------------------------------------
(
  section="C1"
  adb shell am force-stop "$package" || true
  seed_ts="$(adb shell date +%s | tr -d '\r')000"
  printf '%s\n' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
    '<map>' \
    "    <string name=\"msg_recipients_v1\">[{\"number\":\"$seed_number\",\"ts\":$seed_ts}]</string>" \
    '</map>' > "$out/c2_state_seed_cll.xml"
  adb shell run-as "$package" mkdir -p "$prefs_dir" 2>/dev/null || true
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$out/c2_state_seed_cll.xml"
  adb logcat -c || true
  launch
  open_log_page
  open_clear_page
  shot clear-log-lists-page
  # row 4 = Message recipients
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK   # opens the confirm dialog
  shot clear-log-lists-confirm
  tap OK   # confirm YES
  shot clear-log-lists-cleared
  require_marker 'CLEAR_LOG_LISTS action=cllRecipients' c1 || exit 1
  # Back restores the Log selection (row 5, Clear log lists); step UP once
  # to row 4 (Msg. recipients) explicitly, then verify the list is empty.
  tap RSK
  shot clear-log-lists-back-on-log
  tap UP
  tap OK
  require_marker 'MSG_RECIPIENTS_EMPTY' c1 || exit 1
  echo "PASS Message recipients list cleared via Clear log lists" | tee -a "$result"
) || failures=$((failures + 1))

# --- C2: clear Missed calls, provider untouched -------------------------
(
  section="C2"
  adb shell am force-stop "$package" || true
  now_ms="$(adb shell date +%s | tr -d '\r')000"
  # The call_log provider refuses the shell user (uid 2000 lacks the call-log
  # permissions) and the app does not declare WRITE_CALL_LOG, so seed as root:
  # the CI emulator (google_apis userdebug) allows adb root. Root is scoped to
  # this section; the EXIT trap unroots.
  if ! adb root 2>&1 | grep -qi 'root'; then
    echo "FAIL[C2] adb root unavailable on this emulator image" | tee -a "$result"
    exit 1
  fi
  sleep 3
  adb wait-for-device
  before_missed="$(adb shell content query --uri content://call_log/calls --projection number:type 2>&1 | tr -d '\r' | grep -c 'type=3' || true)"
  expected_hidden=$((before_missed + 1))
  insert_out="$(adb shell content insert --uri content://call_log/calls \
    --bind number:s:$missed_number --bind type:i:3 --bind date:l:$now_ms \
    --bind duration:l:0 2>&1 | tr -d '\r')"
  echo "C2 insert output: ${insert_out:-<empty>}" >> "$result"
  # API 30 prints nothing on success; locate the row by number instead.
  inserted_id="$(adb shell content query --uri content://call_log/calls --projection _id:number:type 2>&1 | tr -d '\r' \
    | sed -n "s/.*_id=\([0-9]*\), number=$missed_number,.*/\1/p")"
  if [[ ! "$inserted_id" =~ ^[0-9]+$ ]]; then
    echo "FAIL[C2] provider missed-call insert did not produce a readable row" | tee -a "$result"
    exit 1
  fi
  echo "C2 provider seeded _id=$inserted_id (missed rows before: $before_missed)" >> "$result"
  echo "$inserted_id" > "$out/c2_inserted_id.txt"
  adb logcat -c || true
  launch
  open_log_page
  open_clear_page
  # row 1 = Missed calls
  tap DOWN
  tap OK   # confirm dialog
  tap OK   # confirm YES
  shot clear-log-lists-missed-cleared
  require_marker "CLEAR_LOG_LISTS action=cllMissed hidden=$expected_hidden" c2 || exit 1
  after="$(adb shell content query --uri content://call_log/calls --projection _id:number:type 2>&1 | tr -d '\r' \
    | sed -n "s/.*_id=\([0-9]*\), number=$missed_number,.*/\1/p")"
  if [[ "$after" != "$inserted_id" ]]; then
    echo "FAIL[C2] provider row _id=$inserted_id changed by app-side clear: now='$after'" | tee -a "$result"
    exit 1
  fi
  # In-app Missed calls page must be empty now. Back restores the Log
  # selection (row 5, Clear log lists), so step UP four times to row 1
  # (Missed calls) explicitly.
  tap RSK
  tap UP
  tap UP
  tap UP
  tap UP
  tap OK
  sleep 1
  shot clear-log-lists-missed-empty
  echo "PASS Missed calls list cleared via Clear log lists, provider unchanged" | tee -a "$result"
) || failures=$((failures + 1))

exit "$((failures > 0))"
