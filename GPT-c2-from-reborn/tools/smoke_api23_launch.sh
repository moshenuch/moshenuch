#!/usr/bin/env bash
set -euo pipefail
apk="$(find apk-validation -type f -name 'app-debug.apk' -print -quit)"
if [[ -z "$apk" ]]; then
  echo "Validation APK artifact missing"
  find apk-validation -maxdepth 3 -type f || true
  exit 1
fi
api="$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
echo "Testing native APK launch on API ${api}"
echo "Installing application APK: $apk"
adb install -r "$apk"
adb shell pm path com.moshenuch.c2reborn
adb shell am start -n com.moshenuch.c2reborn/.MainActivity
sleep 5
adb shell pidof com.moshenuch.c2reborn
if adb logcat -d -s AndroidRuntime:E | grep -A 8 'FATAL EXCEPTION' | grep -q 'com.moshenuch.c2reborn'; then
  echo "Native app crashed on Android API ${api}"
  exit 1
fi
echo "Android API ${api} native launch passed; EXIF visual correctness remains untested."
