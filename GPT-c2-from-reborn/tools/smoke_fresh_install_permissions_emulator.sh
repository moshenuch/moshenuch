#!/usr/bin/env bash
# Android 11 acceptance for the fresh-install permission lifecycle: a cold
# first launch with NO pre-granted runtime permissions. BLOCKING in the
# suite.
#  D. Deny path: every OS sheet denied. The app must survive, render home,
#    open Contacts without READ_CONTACTS and stay foreground. The dampened
#    startup stays silent on the second cold start; opening Contacts then
#    re-asks with the OS sheet exactly once.
#  A. Allow path: every OS sheet allowed. The app must end with the phone
#    and storage permissions granted, survive, and render home.
# The sheets are Android's own (com.android.permissioncontroller): C2 never
# draws its own permission UI. Verification reads dumpsys package grants,
# process liveness and logcat. Emulator only.
set -euo pipefail
apk="${1:?Usage: smoke_fresh_install_permissions_emulator.sh path/to/app-debug.apk [results-dir]}"
out="${2:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$out"
result="$out/fresh-install-permissions-results.txt"
: > "$result"
package=com.moshenuch.c2reborn

qemu="$(adb shell getprop ro.kernel.qemu 2>/dev/null | tr -d '\r'; adb shell getprop ro.boot.qemu 2>/dev/null | tr -d '\r')"
if ! grep -q 1 <<< "$qemu"; then
  echo "REFUSING: fresh-install permission probe runs on emulators only" | tee -a "$result"
  exit 1
fi

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
named={'OK':(180,474),'BACK':(270,414),'DOWN':(180,505),'5':(180,610)}
for name in ['OK','BACK','DOWN','5']:
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
shot() { adb exec-out screencap -p > "$out/$1.png" 2>/dev/null || true; }
capture_evidence() {
  adb logcat -d -v brief -s AndroidRuntime:E '*:S' > "$out/fresh-install-permissions-log.txt" 2>&1 || true
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
expect_alive() {
  local pid
  pid="$(adb shell pidof -s "$package" 2>/dev/null | tr -d '\r[:space:]' || true)"
  if [[ -z "$pid" ]]; then
    echo "FAIL C2 process died ($1)" | tee -a "$result"
    exit 1
  fi
  if adb logcat -d 2>/dev/null | grep -q "FATAL EXCEPTION.*$package"; then
    echo "FAIL C2 crashed ($1): FATAL EXCEPTION in logcat" | tee -a "$result"
    exit 1
  fi
}

# tap_perm_button <Allow|Deny>: 0 when a permission sheet was showing and the
# button was tapped, 1 when no sheet is visible.
tap_perm_button() {
  local want="$1" tries=0 coords=""
  while [[ $tries -lt 3 ]]; do
    adb shell uiautomator dump /sdcard/c2-ui.xml >/dev/null 2>&1 || true
    adb pull /sdcard/c2-ui.xml /tmp/c2-ui.xml >/dev/null 2>&1 || true
    coords="$(python3 - "$want" <<'PY'
import sys, re
want = sys.argv[1]
try:
    xml = open('/tmp/c2-ui.xml', encoding='utf-8', errors='replace').read()
except OSError:
    sys.exit(0)
if 'permissioncontroller' not in xml and 'packageinstaller' not in xml:
    sys.exit(0)
for m in re.finditer(r'<node[^>]*/?>', xml):
    node = m.group(0)
    text = re.search(r'text="([^"]*)"', node)
    if not text or text.group(1).strip().lower() != want.lower():
        continue
    b = re.search(r'bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"', node)
    if not b:
        continue
    x1, y1, x2, y2 = map(int, b.groups())
    print(f"{(x1+x2)//2} {(y1+y2)//2}")
    sys.exit(0)
PY
)"
    [[ -n "$coords" ]] && break
    tries=$((tries + 1))
    sleep 1
  done
  [[ -n "$coords" ]] || return 1
  echo "PERMSHEET $want $coords" >> "$result"
  adb shell input tap $coords
  sleep 2
  return 0
}

# settle_sheets <Allow|Deny> <max>: answer up to <max> consecutive sheets.
settle_sheets() {
  local choice="$1" max="$2" n=0
  while [[ $n -lt $max ]] && tap_perm_button "$choice"; do
    n=$((n + 1))
  done
  echo "answered $n permission sheet(s) with $choice" >> "$result"
  ANSWERED=$n
}

perm_granted() { # perm_granted <permission>: 0 when dumpsys says granted=true
  adb shell dumpsys package "$package" 2>/dev/null | tr -d '\r' \
    | grep -F "android.permission.$1: granted=true" >/dev/null
}

fresh_install() {
  adb uninstall "$package" >/dev/null 2>&1 || true
  adb install -r -t "$apk" >> "$result" 2>&1 || {
    echo "FAIL fresh install failed" | tee -a "$result"; exit 1; }
  adb shell pm path "$package" 2>/dev/null | grep -Fq 'package:' || {
    echo "FAIL package missing after fresh install" | tee -a "$result"; exit 1; }
}

launch() {
  adb shell am start -W -n "$package/.MainActivity" >> "$result"
  sleep 4
}

# --- D. Deny every sheet ----------------------------------------------------
fresh_install
adb logcat -c || true
launch
settle_sheets Deny 6
if [[ $ANSWERED -eq 0 ]]; then
  echo 'FAIL no permission sheet appeared on a fresh install' | tee -a "$result"
  exit 1
fi
expect_alive 'deny path after sheets'
expect_foreground
shot freshinstall-deny-home
if perm_granted READ_CONTACTS; then
  echo 'FAIL READ_CONTACTS granted despite Deny' | tee -a "$result"; exit 1
fi
echo 'PASS Fresh install deny-all: app survived, home rendered, no crash' | tee -a "$result"

# Contacts must open gracefully without READ_CONTACTS (fail-closed empty
# list, never a SecurityException).
tap OK                          # menu grid
tap OK                          # Contacts
sleep 2
expect_alive 'deny path Contacts page'
expect_foreground
echo 'PASS Fresh install deny-all: Contacts opens without contacts permission and stays foreground' | tee -a "$result"
shot freshinstall-deny-contacts
tap BACK
tap BACK

# Dampened startup (2.03.19): after a plain denial the phone cascade must
# NOT re-fire on cold start. Zero sheets.
adb shell am force-stop "$package"
launch
settle_sheets Deny 2
if [[ $ANSWERED -ne 0 ]]; then
  echo "FAIL second cold start after denial re-showed $ANSWERED sheet(s); dampened startup must stay silent" | tee -a "$result"
  exit 1
fi
expect_alive 'deny path second launch'
expect_foreground
echo 'PASS Fresh install deny-all: second cold start shows no permission sheets' | tee -a "$result"

# Opening a feature that needs a denied permission re-asks with the OS
# sheet (once per feature per session; never C2-drawn UI).
tap OK                          # menu grid
tap OK                          # Contacts
sleep 2
settle_sheets Deny 2
if [[ $ANSWERED -ne 1 ]]; then
  echo "FAIL opening Contacts after denial showed $ANSWERED sheet(s); expected exactly 1" | tee -a "$result"
  exit 1
fi
expect_alive 'deny path feature re-ask'
expect_foreground
echo 'PASS Fresh install deny-all: opening Contacts after denial re-asks with the OS sheet' | tee -a "$result"
shot freshinstall-deny-contacts-reask

# Direct entry, dial pad: a digit on home opens the dial page; the
# CALL_PHONE group re-ask fires exactly once (audit #40).
tap BACK
tap BACK
tap 5
sleep 2
settle_sheets Deny 2
if [[ $ANSWERED -ne 1 ]]; then
  echo "FAIL opening the dial pad after denial showed $ANSWERED sheet(s); expected exactly 1" | tee -a "$result"
  exit 1
fi
expect_alive 'deny path dial re-ask'
expect_foreground
echo 'PASS Fresh install deny-all: opening the dial pad after denial re-asks with the OS sheet' | tee -a "$result"
shot freshinstall-deny-dial-reask

# Direct entry, call log (new process so the calls group is unasked):
# menu -> Log -> All calls re-asks with the OS sheet exactly once.
adb shell am force-stop "$package"
launch
settle_sheets Deny 2
if [[ $ANSWERED -ne 0 ]]; then
  echo "FAIL third cold start re-showed $ANSWERED sheet(s); dampened startup must stay silent" | tee -a "$result"
  exit 1
fi
tap OK                          # menu grid (selection index 0)
tap DOWN                        # grid row 2
tap DOWN                        # grid row 3: Log
tap OK                          # Log page
tap OK                          # All calls
sleep 2
settle_sheets Deny 2
if [[ $ANSWERED -ne 1 ]]; then
  echo "FAIL opening the call log after denial showed $ANSWERED sheet(s); expected exactly 1" | tee -a "$result"
  exit 1
fi
expect_alive 'deny path call-log re-ask'
expect_foreground
echo 'PASS Fresh install deny-all: opening the call log after denial re-asks with the OS sheet' | tee -a "$result"
shot freshinstall-deny-calllog-reask

# --- A. Allow every sheet ---------------------------------------------------
fresh_install
# Seed one contact as shell (needs no app permission) so the allow path has
# provider content to read.
adb shell content insert --uri content://com.android.contacts/raw_contacts \
  --bind starred:i:0 >/dev/null 2>&1 || true
seed_id="$(adb shell content query --uri content://com.android.contacts/raw_contacts \
  --projection _id 2>/dev/null | tr -d '\r' | grep -oE '_id=[0-9]+' | cut -d= -f2 | sort -n | tail -1)"
[[ -n "$seed_id" ]] || { echo 'UNVERIFIED contact seed failed' | tee -a "$result"; exit 1; }
adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$seed_id" \
  --bind mimetype:s:vnd.android.cursor.item/name \
  --bind data1:s:AAAC2FreshInstall >/dev/null 2>&1 || true
adb shell content insert --uri content://com.android.contacts/data \
  --bind raw_contact_id:i:"$seed_id" \
  --bind mimetype:s:vnd.android.cursor.item/phone_v2 \
  --bind data1:s:07900005010 --bind data2:i:2 >/dev/null 2>&1 || true

adb logcat -c || true
launch
settle_sheets Allow 6
if [[ $ANSWERED -eq 0 ]]; then
  echo 'FAIL no permission sheet appeared on the allow path' | tee -a "$result"
  exit 1
fi
expect_alive 'allow path after sheets'
expect_foreground
shot freshinstall-allow-home
for perm in READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS \
            CALL_PHONE ANSWER_PHONE_CALLS READ_CALL_LOG READ_EXTERNAL_STORAGE; do
  if ! perm_granted "$perm"; then
    echo "FAIL $perm not granted after Allow on every sheet" | tee -a "$result"
    exit 1
  fi
done
echo 'PASS Fresh install allow-all: every phone and storage permission granted through the OS sheets' | tee -a "$result"
tap OK                          # menu grid
tap OK                          # Contacts
sleep 2
expect_alive 'allow path Contacts page'
expect_foreground
shot freshinstall-allow-contacts
tap BACK
tap BACK
echo 'PASS Fresh install allow-all: app survived, Contacts opened, no crash' | tee -a "$result"

# Cleanup: remove the seeded contact so later probes start clean.
adb shell content delete --uri content://com.android.contacts/raw_contacts \
  --where "_id=$seed_id" >/dev/null 2>&1 || true

echo 'PASS Fresh install permission lifecycle acceptance complete' | tee -a "$result"
