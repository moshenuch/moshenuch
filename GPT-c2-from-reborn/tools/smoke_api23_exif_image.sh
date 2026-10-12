#!/usr/bin/env bash
set -euo pipefail
app="$(find apk-validation -name 'app-debug.apk' -print -quit)"
test_apk="$(find apk-validation -name 'app-debug-androidTest.apk' -print -quit)"
if [[ -z "$app" || -z "$test_apk" ]]; then
  echo "Missing app or test APK"
  find apk-validation -type f
  exit 1
fi
adb install -r "$app"
adb install -r "$test_apk"
adb shell am instrument -w -r   -e class com.moshenuch.c2reborn.ExifOrientationInstrumentedTest   com.moshenuch.c2reborn.test/androidx.test.runner.AndroidJUnitRunner   | tee /tmp/c2-exif-instrumentation.txt
grep -q 'OK (1 test)' /tmp/c2-exif-instrumentation.txt
echo "Android 6 EXIF JPEG rotation verified on device."
