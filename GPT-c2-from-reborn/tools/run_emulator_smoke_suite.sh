#!/usr/bin/env bash
# One script so android-emulator-runner cannot split a multiline if/fi.
set -euo pipefail
apk="GPT-c2-from-reborn/app/build/outputs/apk/debug/app-debug.apk"
results="GPT-c2-from-reborn/build/emulator-smoke"
# Installation and native Activity startup are required to pass.
bash GPT-c2-from-reborn/tools/smoke_install_emulator.sh "$apk" "$results"
# Recording hardware may be unsupported by emulated camera; always retain
# video logs but don't lose a successfully built/distributed APK as a result.
if ! bash GPT-c2-from-reborn/tools/smoke_video_emulator.sh "$results"; then
  echo "::warning::Native video probe failed; inspect video-results.txt and video-camera-log.txt"
fi
