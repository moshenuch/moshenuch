#!/usr/bin/env bash
set -euo pipefail
apk="$(find apk-validation -type f -name '*.apk' -print -quit)"
if [[ -z "$apk" ]]; then
  echo "Validation APK artifact missing"
  find apk-validation -maxdepth 3 -type f || true
  exit 1
fi
adb install -r "$apk"
adb shell am start -n com.moshenuch.c2reborn/.MainActivity
sleep 5
adb shell pidof com.moshenuch.c2reborn
if adb logcat -d -s AndroidRuntime:E | grep -A 8 'FATAL EXCEPTION' | grep -q 'com.moshenuch.c2reborn'; then
  echo "Native app crashed on Android 6 (API 23)"
  exit 1
fi
echo "Android 6 native launch passed; EXIF visual correctness remains untested."
