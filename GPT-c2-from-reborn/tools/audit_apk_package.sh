#!/usr/bin/env bash
# Inspect the exact Gradle-generated APK BEFORE copying or publishing it.
# This deliberately does not modify, recompress, zip, or resign the APK.
set -euo pipefail
apk="${1:?Usage: audit_apk_package.sh /path/to/app-debug.apk}"
version_file="app/build.gradle"
[[ -f "$apk" && -f "$version_file" ]] || { echo "APK or version file absent"; exit 1; }
expected_version="$(awk -F'"' '/versionName/ { print $2; exit }' "$version_file")"
[[ -n "$expected_version" ]] || { echo "Missing versionName"; exit 1; }

bytes="$(stat -c%s "$apk")"
[[ "$bytes" -gt 100000 ]] || { echo "APK unexpectedly small: $bytes bytes"; exit 1; }
unzip -tqq "$apk"
unzip -Z1 "$apk" | grep -Fx 'AndroidManifest.xml' >/dev/null
unzip -Z1 "$apk" | grep -Fx 'classes.dex' >/dev/null

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$sdk_root" && -d "$sdk_root/build-tools" ]] || {
  echo "Android SDK build-tools missing"; exit 1;
}
build_tools="$(find "$sdk_root/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
[[ -x "$build_tools/aapt" && -x "$build_tools/zipalign" && -x "$build_tools/apksigner" ]] || {
  echo "aapt, zipalign or apksigner unavailable in $build_tools"; exit 1;
}
badging="$("$build_tools/aapt" dump badging "$apk")"
package_line="$(grep '^package: ' <<< "$badging")"
[[ "$package_line" == *"name='com.moshenuch.c2reborn'"* ]] || {
  echo "Unexpected APK package: $package_line"; exit 1;
}
[[ "$package_line" == *"versionName='$expected_version'"* ]] || {
  echo "Unexpected APK version: $package_line"; exit 1;
}
"$build_tools/zipalign" -c -v 4 "$apk" >/dev/null
"$build_tools/apksigner" verify --verbose "$apk" >/dev/null

sha="$(sha256sum "$apk" | cut -d' ' -f1)"
echo "PASS APK ZIP, AndroidManifest.xml, classes.dex, package/version, alignment, signature"
echo "APK VERSION=$expected_version SIZE=$bytes SHA256=$sha"
{
  echo "### APK packaging validation"
  echo "APK: `$(basename "$apk")`"
  echo "Version: `$expected_version`"
  echo "Size: `$bytes` bytes"
  echo "SHA-256: `$sha`"
  echo "ZIP, manifest, DEX, package/version, zipalign and apksigner: PASS"
  echo "These checks do not prove that a third-party emulator upload interface accepts this APK."
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
