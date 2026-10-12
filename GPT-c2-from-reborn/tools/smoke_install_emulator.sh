#!/usr/bin/env bash
# Install and launch the actual Gradle APK on Android 11 (API 30).
# This is an install/launch smoke test, NOT camera, video or visual parity testing.
set -euo pipefail
apk="${1:?Usage: smoke_install_emulator.sh path/to/app-debug.apk}"
report_dir="${2:-GPT-c2-from-reborn/build/emulator-smoke}"
package="com.moshenuch.c2reborn"
activity="$package/.MainActivity"
mkdir -p "$report_dir"

# Capture logs even if installation or app launch fails.
capture_logs() {
  adb logcat -d -v threadtime -t 2500 > "$report_dir/logcat.txt" 2>&1 || true
}
trap capture_logs EXIT

test -s "$apk" || { echo "::error::Built APK missing or empty"; exit 1; }
adb wait-for-device
adb shell getprop sys.boot_completed | tr -d '\r' | grep -Fxq '1' || {
  echo "::error::Emulator not fully booted"; exit 1;
}

echo "Installing APK: $apk"
adb install -r -t "$apk" | tee "$report_dir/install.txt"
adb shell pm path "$package" | tee "$report_dir/installed-package.txt"
grep -Fq 'package:' "$report_dir/installed-package.txt" || {
  echo "::error::APK package not found after adb install"; exit 1;
}

echo "Launching native Activity"
timeout 45s adb shell am start -W -n "$activity" | tee "$report_dir/launch.txt"
if grep -Eiq 'Error:|Exception|Status: timeout' "$report_dir/launch.txt"; then
  echo "::error::Native Activity failed to launch"
  exit 1
fi

# Allow the launch and permission sheet to settle; the Activity must survive.
sleep 5
pid="$(adb shell pidof -s "$package" 2>/dev/null | tr -d '\r[:space:]' || true)"
if [[ -z "$pid" ]]; then
  echo "::error::C2 process not running after launch"
  exit 1
fi

# A screenshot and Android logs are available as CI artifacts for the audit.
adb shell screencap -p /sdcard/c2-native-smoke.png
adb pull /sdcard/c2-native-smoke.png "$report_dir/screen.png" >/dev/null
adb shell rm -f /sdcard/c2-native-smoke.png

version="$(adb shell dumpsys package "$package" | grep -m1 'versionName=' | tr -d '\r' || true)"
{
  echo "PASS: Android emulator accepted and installed APK"
  echo "PASS: native MainActivity launched, process PID $pid"
  echo "Installed $version"
  echo "Screenshot: $report_dir/screen.png"
  echo "Limitations: no camera, microphone, Gallery persistence or visual-parity test executed."
} | tee "$report_dir/results.txt"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### Android 11 APK install and launch smoke test"
    echo '~~~text'
    cat "$report_dir/results.txt"
    echo '~~~'
  } >> "$GITHUB_STEP_SUMMARY"
fi
