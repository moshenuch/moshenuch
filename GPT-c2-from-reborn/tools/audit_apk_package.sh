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
# Android APK ZIP entries must use conventional ZIP methods. Report contents
# instead of trying to repack a build whose uploader previously rejected it.
python3 - "$apk" <<'PY'
import sys, zipfile
apk = sys.argv[1]
with zipfile.ZipFile(apk) as z:
    infos = z.infolist()
    names = [e.filename for e in infos]
    if len(names) != len(set(names)):
        raise SystemExit("FAIL: duplicate APK ZIP member names")
    unexpected = sorted({e.compress_type for e in infos} - {
        zipfile.ZIP_STORED, zipfile.ZIP_DEFLATED})
    if unexpected:
        raise SystemExit("FAIL: unsupported ZIP compression methods: " + str(unexpected))
    if "AndroidManifest.xml" not in names or "classes.dex" not in names:
        raise SystemExit("FAIL: required APK members missing")
    # An entry name alone does not establish that the bundled font is usable.
    for font in ("assets/nokia_font/regular.ttf", "assets/nokia_font/bold.ttf"):
        if font not in names:
            raise SystemExit("FAIL: Nokia font missing: " + font)
        info = z.getinfo(font)
        if info.file_size < 1000:
            raise SystemExit("FAIL: Nokia font is unexpectedly small: " + font)
        with z.open(font) as stream:
            signature = stream.read(4)
        if signature not in (b"\\x00\\x01\\x00\\x00", b"true", b"OTTO"):
            print("WARN: Nokia font does not have a standard sfnt header: " + font)
            print("WARN: Asset is present, but its font format requires separate verification.")
    print("PASS ZIP methods: stored/deflated; members:", len(names))
    print("ZIP compressed:", sum(e.compress_size for e in infos),
          "uncompressed:", sum(e.file_size for e in infos))
PY
unzip -Z1 "$apk" | grep -Fx 'AndroidManifest.xml' >/dev/null
unzip -Z1 "$apk" | grep -Fx 'classes.dex' >/dev/null
# The Nokia font is a product requirement. NokiaFontFaces silently falls back
# to Android's default typeface when these assets are absent, so a build that
# loses them must fail here, before publication, not in a visual review.
unzip -Z1 "$apk" | grep -Fx 'assets/nokia_font/regular.ttf' >/dev/null
unzip -Z1 "$apk" | grep -Fx 'assets/nokia_font/bold.ttf' >/dev/null

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
echo "PASS APK ZIP, AndroidManifest.xml, classes.dex, Nokia font assets, package/version, alignment, signature"
echo "APK VERSION=$expected_version SIZE=$bytes SHA256=$sha"
{
  echo "### APK packaging validation"
  echo "APK: `$(basename "$apk")`"
  echo "Version: `$expected_version`"
  echo "Size: `$bytes` bytes"
  echo "SHA-256: `$sha`"
  echo "ZIP, manifest, DEX, Nokia font assets, package/version, zipalign and apksigner: PASS"
  echo "These checks do not prove that a third-party emulator upload interface accepts this APK."
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
