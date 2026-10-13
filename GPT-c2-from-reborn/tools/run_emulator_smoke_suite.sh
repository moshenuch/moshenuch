#!/usr/bin/env bash
# One script so android-emulator-runner cannot split a multiline if/fi.
set -euo pipefail
apk="GPT-c2-from-reborn/app/build/outputs/apk/debug/app-debug.apk"
results="GPT-c2-from-reborn/build/emulator-smoke"
bash GPT-c2-from-reborn/tools/smoke_install_emulator.sh "$apk" "$results"
if bash GPT-c2-from-reborn/tools/smoke_video_emulator.sh "$results"; then
  echo "PASS: Emulator recorded, finalized and independently decoded an MP4." | tee "$results/video-suite-status.txt"
else
  echo "FAIL (non-blocking): Emulator recording/finalization/MP4 validation did not pass. Oppo hardware remains UNTESTED." | tee "$results/video-suite-status.txt"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_photo_emulator.sh "$results"; then
  echo "::warning::Native photo or Gallery-index probe failed"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_gallery_capacity_emulator.sh "$results"; then
  echo "::warning::Gallery capacity fixture failed"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_video_report_emulator.sh "$results"; then
  echo "::warning::Video report receiver test failed"
fi
# This probe is diagnostic until it is paired with a deterministic player-open
# fixture. It must not turn an unavailable navigation fixture into an app failure.
if ! bash GPT-c2-from-reborn/tools/smoke_android_back_emulator.sh "$results"; then
  echo "::warning::Android Back diagnostic failed"
fi
