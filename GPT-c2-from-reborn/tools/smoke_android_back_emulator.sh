#!/usr/bin/env bash
# Android Back acceptance probe. Kept separate from recording so failures
# identify navigation problems rather than camera problems.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
result="$report_dir/android-back-results.txt"

# The native app writes C2Back diagnostics when the player receives Back.
adb logcat -c || true
adb shell input keyevent KEYCODE_BACK
sleep 2
adb logcat -d -v brief > "$report_dir/android-back-log.txt" || true

if grep -q "C2Back" "$report_dir/android-back-log.txt"; then
  echo "PASS Android Back event reached C2" | tee "$result"
else
  echo "UNVERIFIED Android Back event produced no C2Back diagnostic" | tee "$result"
fi
