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
# Grant only permissions normally grantable at runtime, before UI navigation.
# A permission dialog covering the Nokia interface invalidates coordinate taps.
for permission in CAMERA READ_CONTACTS WRITE_CONTACTS READ_SMS SEND_SMS RECEIVE_SMS READ_CALL_LOG RECORD_AUDIO CALL_PHONE; do
  adb shell pm grant com.moshenuch.c2reborn "android.permission.$permission" >/dev/null 2>&1 || true
done
adb shell am start -W -n com.moshenuch.c2reborn/.MainActivity
# Avoid treating a slow emulator foreground transition as a product failure.
running=false
for attempt in 1 2 3 4 5 6; do
  if adb shell pidof com.moshenuch.c2reborn >/dev/null 2>&1; then
    running=true
    break
  fi
  sleep 2
done
if [[ "$running" != true ]]; then
  echo "App process not running after launch"
  adb logcat -d -s AndroidRuntime:E '*:S' | tail -n 60 || true
  exit 1
fi
if adb logcat -d -s AndroidRuntime:E | grep -A 8 'FATAL EXCEPTION' | grep -q 'com.moshenuch.c2reborn'; then
  echo "Native app crashed on Android API ${api}"
  exit 1
fi
echo "Android API ${api} native launch passed; EXIF visual correctness remains untested."
