#!/usr/bin/env bash
# Android 11 acceptance for Log > Msg. recipients (v2.03.7). Both sections are
# BLOCKING in the suite:
#  R1. Empty: with no recipients stored, Log > Msg. recipients stays on the
#      Log page and shows the "No numbers" notice (marker MSG_RECIPIENTS_EMPTY).
#  R2. Seeded: with one recipient in the app's private prefs (written with
#      run-as, debug build only), Log > Msg. recipients opens the "Message
#      recipients" page (marker MSG_RECIPIENTS_OPEN count=1); a screenshot is
#      captured for visual review.
# The seeded prefs file is removed afterwards so later runs stay deterministic.
set -euo pipefail
out="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/msg-recipients-results.txt"
: > "$result"
package=com.moshenuch.c2reborn
seed_number="07900001234"

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
marker_seen() { # marker_seen <text>: 0 when the app logged it since last clear
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
    echo "FAIL expected app log marker: $1" | tee -a "$result"
    echo "--- C2Log logcat at failure ---" | tee -a "$result"
    adb logcat -d -s C2Log:I 2>/dev/null | tee -a "$result" > "$out/msg-recipients-section-log.txt" || true
    echo "--- end logcat ---" | tee -a "$result"
    exit 1
  fi
}
launch() {
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 2
}
open_log_page() { # from home: Menu > grid Down Down (Contacts->Gallery->Log) > OK
  tap LSK
  tap DOWN
  tap DOWN
  tap OK
  sleep 1
}

for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG; do
  adb shell pm grant "$package" "android.permission.$perm" 2>/dev/null || true
done

# --- R1: empty store ---------------------------------------------------
adb shell am force-stop "$package" || true
adb logcat -c || true
launch
open_log_page
# Log page rows: 0 All calls ... 4 Msg. recipients
tap DOWN
tap DOWN
tap DOWN
tap DOWN
tap OK
require_marker 'MSG_RECIPIENTS_EMPTY'
sleep 1
shot msg-recipients-empty
echo "PASS empty Msg. recipients stays on Log with No numbers notice" | tee -a "$result"

# --- R2: one seeded recipient ------------------------------------------
adb shell am force-stop "$package" || true
seed_ts="$(adb shell date +%s | tr -d '\r')000"
prefs_dir="/data/data/$package/shared_prefs"
# Build the prefs XML locally, push as the shell user, then copy it in as the
# app user: run-as cannot write /data/local/tmp but can read it.
printf '%s\n' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
  '<map>' \
  "    <string name=\"msg_recipients_v1\">[{\"number\":\"$seed_number\",\"ts\":$seed_ts}]</string>" \
  '</map>' > "$out/c2_state_seed.xml"
adb push "$out/c2_state_seed.xml" /data/local/tmp/c2_state_seed.xml >> "$result" 2>&1
adb shell run-as "$package" sh -c "mkdir -p $prefs_dir && cp /data/local/tmp/c2_state_seed.xml $prefs_dir/c2_state.xml" || true
adb shell rm -f /data/local/tmp/c2_state_seed.xml || true
adb logcat -c || true
launch
open_log_page
tap DOWN
tap DOWN
tap DOWN
tap DOWN
tap OK
require_marker 'MSG_RECIPIENTS_OPEN count=1'
sleep 1
shot msg-recipients-page
echo "PASS seeded Msg. recipients page opened with 1 entry" | tee -a "$result"

# --- restore clean state ------------------------------------------------
adb shell am force-stop "$package" || true
adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
echo "PASS recipients prefs fixture removed after probe" | tee -a "$result"
