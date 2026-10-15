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
#  C3. No/cancel on the confirm keeps both lists intact.
#  C4. Cleared state survives a force-stop + relaunch.
#  C5. Softkey regression: empty All/Missed/Received/Dialled pages show
#      Back only; labels return when records exist (SOFTBAR markers).
# The app's private c2_state.xml is backed up and restored via trap; seeded
# provider rows are tracked in c2_inserted_ids.txt and deleted at the end
# with exact-id verification.
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
received_number="07234567890"
dialled_number="07345678901"
return_number="07456789012"
rm -f "$out/c2_inserted_ids.txt"

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

provider_ids_for() { # exact _id list for a number (number anchored at EOL)
  adb shell content query --uri content://call_log/calls --projection _id:number 2>&1 | tr -d '\r' \
    | sed -n "s/.*_id=\([0-9][0-9]*\), number=$1$/\1/p"
}
provider_row_count() {
  adb shell content query --uri content://call_log/calls --projection _id 2>&1 | tr -d '\r' \
    | grep -c '^Row:' || true
}
seed_call_row() { # seed_call_row <number> <type>: insert as root, echo new _id
  local num="$1" typ="$2" ins_out new_id
  ins_out="$(adb shell content insert --uri content://call_log/calls \
    --bind number:s:$num --bind type:i:$typ \
    --bind date:l:"$(adb shell date +%s | tr -d '\r')000" \
    --bind duration:l:0 2>&1 | tr -d '\r')"
  if grep -qiE 'exception|denied|error' <<< "$ins_out"; then
    echo "FAIL[$section] provider insert for $num errored: $ins_out" >> "$result"
    exit 1
  fi
  new_id="$(provider_ids_for "$num" | tail -1)"
  if [[ ! "$new_id" =~ ^[0-9]+$ ]]; then
    echo "FAIL[$section] provider insert for $num produced no readable row" >> "$result"
    exit 1
  fi
  echo "$new_id" >> "$out/c2_inserted_ids.txt"
  echo "$new_id"
}

prefs_dir="/data/data/$package/shared_prefs"
backup="$out/c2_state_backup_cll.xml"
restore_needed=0
# audit-#11 item 3: root release is guaranteed at the END of the EXIT trap
# on every path - the FAIL branches below record cleanup_status instead of
# exiting early, so they can no longer skip the unroot. unroot restarts
# adbd; the wait+sleep stops the NEXT probe's adb root racing the restart
# (run 38691909508 clear-log C2).
cleanup_status=0
ensure_unroot() {
  adb unroot >/dev/null 2>&1 || true
  adb wait-for-device || true
  sleep 2
  # audit-#12 item 4: prove root was actually dropped; record when it was not.
  local uid
  uid="$(adb shell id -u 2>/dev/null | tr -d '\r' || true)"
  if [[ "$uid" == "0" ]]; then
    echo "FAIL unroot did not drop root: id -u still 0" | tee -a "$result"
    cleanup_status=1
  elif [[ -z "$uid" ]]; then
    echo "WARN could not verify effective uid after unroot" | tee -a "$result"
  fi
}
cleanup_state() {
  local remaining_ids=()
  adb shell am force-stop "$package" || true
  if [[ "$restore_needed" == "1" ]]; then
    adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$backup" || true
    if adb shell "run-as $package cat $prefs_dir/c2_state.xml" 2>/dev/null | tr -d '\r' | cmp -s - "$backup"; then
      echo "PASS original c2_state.xml restored after probe (bytes verified)" | tee -a "$result"
    else
      echo "FAIL restore of c2_state.xml did not verify byte-for-byte" | tee -a "$result"
      cleanup_status=1
    fi
  else
    adb shell run-as "$package" rm -f "$prefs_dir/c2_state.xml" || true
    echo "PASS prefs fixture removed after probe" | tee -a "$result"
  fi
  # Seeded provider rows are tracked one _id per line. Delete each by its
  # own _id and verify with an EXACT id match: grep -c "_id=N" would
  # false-positive _id=1 against _id=10, and bare || true swallowed provider
  # errors, letting a failed delete report as clean.
  if [[ -s "$out/c2_inserted_ids.txt" ]]; then
    while IFS= read -r row_id; do
      [[ -n "$row_id" ]] || continue
      del_out="$(adb shell content delete --uri content://call_log/calls --where "_id=$row_id" 2>&1 | tr -d '\r' || true)"
      if grep -qiE 'exception|denied|error' <<< "$del_out"; then
        echo "FAIL cleanup: provider delete of _id=$row_id errored: $del_out" | tee -a "$result"
        cleanup_status=1
        remaining_ids+=("$row_id")
        continue
      fi
      query_out="$(adb shell content query --uri content://call_log/calls --projection _id 2>&1 | tr -d '\r' || true)"
      if grep -qiE 'exception|denied|error' <<< "$query_out"; then
        echo "FAIL cleanup: provider query after delete errored: $query_out" | tee -a "$result"
        cleanup_status=1
        remaining_ids+=("$row_id")
        continue
      fi
      leftover="$(printf '%s\n' "$query_out" | sed -n 's/.*_id=\([0-9][0-9]*\).*/\1/p' | grep -cx "$row_id" || true)"
      if [[ "$leftover" != "0" ]]; then
        echo "FAIL cleanup: seeded call-log row _id=$row_id still present after delete" | tee -a "$result"
        cleanup_status=1
        remaining_ids+=("$row_id")
      else
        echo "PASS seeded call-log row _id=$row_id deleted (exact id match)" | tee -a "$result"
      fi
    done < "$out/c2_inserted_ids.txt"
    # audit-#12 item 3: when anything remains, keep the id file - rewritten
    # to just the remaining ids so a later cleanup pass knows exactly what is
    # still in the provider. Only remove it once every row is gone.
    if [[ "${#remaining_ids[@]}" -gt 0 ]]; then
      printf '%s\n' "${remaining_ids[@]}" > "$out/c2_inserted_ids.txt"
      echo "WARN cleanup: ${#remaining_ids[@]} seeded call-log id(s) remain; kept in c2_inserted_ids.txt" | tee -a "$result"
    else
      rm -f "$out/c2_inserted_ids.txt"
    fi
  fi
  ensure_unroot
  if [[ "$cleanup_status" != "0" ]]; then
    exit "$cleanup_status"
  fi
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
  # the CI emulator (google_apis userdebug) allows adb root. Root is held from
  # here to the end of the probe (C3-C5 also touch the provider); the EXIT
  # trap unroots.
  root_ok=0
  for root_try in 1 2 3; do
    adb wait-for-device || true
    root_out="$(adb root 2>&1 || true)"
    echo "C2 adb root attempt $root_try: $root_out" >> "$result"
    sleep 3
    adb wait-for-device || true
    if [[ "$(adb shell id -u 2>/dev/null | tr -d '\r[:space:]')" == "0" ]]; then
      root_ok=1
      break
    fi
  done
  if [[ "$root_ok" != "1" ]]; then
    echo "FAIL[C2] adb root unavailable on this emulator image after 3 attempts" | tee -a "$result"
    exit 1
  fi
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
  echo "$inserted_id" >> "$out/c2_inserted_ids.txt"
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

# --- C3: No/cancel preserves recipients and missed call -------------------
(
  section="C3"
  adb shell am force-stop "$package" || true
  # Fresh state: one recipient, no hidden-call flags, so the C2-seeded
  # provider missed row is visible again.
  seed_ts="$(adb shell date +%s | tr -d '\r')000"
  printf '%s\n' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' \
    '<map>' \
    "    <string name=\"msg_recipients_v1\">[{\"number\":\"$seed_number\",\"ts\":$seed_ts}]</string>" \
    '</map>' > "$out/c2_state_seed_cll.xml"
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$out/c2_state_seed_cll.xml"
  adb logcat -c || true
  launch
  open_log_page
  open_clear_page
  # row 4 = Message recipients; open the confirm, then answer No (RSK).
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK
  sleep 1
  tap RSK
  sleep 2
  if marker_seen 'CLEAR_LOG_LISTS action=cllRecipients'; then
    echo "FAIL[C3] recipients cleared despite answering No to the confirm" | tee -a "$result"
    exit 1
  fi
  # Recipients list must still hold the seeded entry.
  tap RSK
  tap UP
  tap OK
  require_marker 'MSG_RECIPIENTS_OPEN count=1' || exit 1
  # Missed calls: answer No as well; provider row and visible entry stay.
  tap RSK
  tap DOWN
  tap OK
  tap DOWN
  tap OK
  sleep 1
  tap RSK
  sleep 2
  if marker_seen 'CLEAR_LOG_LISTS action=cllMissed'; then
    echo "FAIL[C3] missed list cleared despite answering No to the confirm" | tee -a "$result"
    exit 1
  fi
  if [[ -z "$(provider_ids_for "$missed_number")" ]]; then
    echo "FAIL[C3] provider missed-call row vanished after No/cancel" | tee -a "$result"
    exit 1
  fi
  tap RSK
  tap UP
  tap UP
  tap UP
  tap UP
  tap OK
  require_marker 'SOFTBAR page=missed soft=Options/Details/Back' || exit 1
  shot clear-log-lists-no-keeps-missed
  echo "PASS No/cancel on Clear log lists preserves recipients and missed call" | tee -a "$result"
) || failures=$((failures + 1))

# --- C4: cleared state persists across force-stop/restart -----------------
(
  section="C4"
  # Entering on the Missed page from C3; clear both lists with Yes this time.
  exp_missed="$(adb shell content query --uri content://call_log/calls --projection _id:type 2>&1 | tr -d '\r' | grep -c 'type=3' || true)"
  tap RSK
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK
  tap DOWN
  tap OK
  tap OK
  require_marker "CLEAR_LOG_LISTS action=cllMissed hidden=$exp_missed" || exit 1
  tap RSK
  tap OK
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK
  tap OK
  require_marker 'CLEAR_LOG_LISTS action=cllRecipients' || exit 1
  adb shell am force-stop "$package" || true
  adb logcat -c || true
  launch
  open_log_page
  tap DOWN
  tap DOWN
  tap DOWN
  tap DOWN
  tap OK
  require_marker 'MSG_RECIPIENTS_EMPTY' || exit 1
  # Empty recipients does not navigate: still on the Log page with the
  # selection on Msg. recipients (the No numbers notice auto-dismisses).
  # Step UP to Missed calls directly - no RSK, which would leave for the
  # menu (run 38684431984).
  sleep 2
  tap UP
  tap UP
  tap UP
  tap OK
  sleep 1
  require_marker 'SOFTBAR page=missed soft=//Back' || exit 1
  shot clear-log-lists-missed-empty-after-restart
  echo "PASS cleared state persists across force-stop and restart" | tee -a "$result"
) || failures=$((failures + 1))

# --- C5: empty-list softkey regression ------------------------------------
(
  section="C5"
  adb shell am force-stop "$package" || true
  # Fresh state (no hidden flags) so every provider row is visible.
  printf '%s\n' '<?xml version="1.0" encoding="utf-8" standalone="yes" ?>' '<map>' '</map>' \
    > "$out/c2_state_seed_cll.xml"
  adb shell "run-as $package sh -c 'cat > $prefs_dir/c2_state.xml'" < "$out/c2_state_seed_cll.xml"
  recv_id="$(seed_call_row "$received_number" 1)"
  dial_id="$(seed_call_row "$dialled_number" 2)"
  if [[ ! "$recv_id" =~ ^[0-9]+$ || ! "$dial_id" =~ ^[0-9]+$ ]]; then
    echo "FAIL[C5] provider seeding failed (received='$recv_id' dialled='$dial_id')" | tee -a "$result"
    exit 1
  fi
  echo "C5 provider seeded received _id=$recv_id dialled _id=$dial_id" >> "$result"
  exp_all="$(provider_row_count)"
  adb logcat -c || true
  launch
  open_log_page
  # Non-empty: every call-list page shows Options/Details/Back.
  tap OK
  require_marker 'SOFTBAR page=calllog soft=Options/Details/Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=missed soft=Options/Details/Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=received soft=Options/Details/Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=dialled soft=Options/Details/Back' || exit 1
  # Clear everything via All call lists.
  tap RSK
  tap DOWN
  tap DOWN
  tap OK
  tap OK
  tap OK
  require_marker "CLEAR_LOG_LISTS action=cllAll hidden=$exp_all" || exit 1
  # Empty: every call-list page shows Back only.
  tap RSK
  tap UP
  tap UP
  tap UP
  tap UP
  tap UP
  tap OK
  require_marker 'SOFTBAR page=calllog soft=//Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=missed soft=//Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=received soft=//Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=dialled soft=//Back' || exit 1
  shot clear-log-lists-dialled-empty
  # Records return -> labels return: seed a fresh missed row and restart.
  new_missed_id="$(seed_call_row "$return_number" 3)"
  if [[ ! "$new_missed_id" =~ ^[0-9]+$ ]]; then
    echo "FAIL[C5] provider return-seed failed ('$new_missed_id')" | tee -a "$result"
    exit 1
  fi
  echo "C5 provider seeded return missed _id=$new_missed_id" >> "$result"
  adb shell am force-stop "$package" || true
  adb logcat -c || true
  launch
  open_log_page
  tap OK
  require_marker 'SOFTBAR page=calllog soft=Options/Details/Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=missed soft=Options/Details/Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=received soft=//Back' || exit 1
  tap RSK
  tap DOWN
  tap OK
  require_marker 'SOFTBAR page=dialled soft=//Back' || exit 1
  echo "PASS empty call lists show Back-only and labels return with records" | tee -a "$result"
) || failures=$((failures + 1))

exit "$((failures > 0))"
