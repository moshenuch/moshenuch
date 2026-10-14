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

# Independent destructive test touches only a newly captured emulator JPEG.
# Run after photo Send so the share receiver does not interrupt deletion.
if bash GPT-c2-from-reborn/tools/smoke_photo_delete_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Delete No retained exact newly captured JPEG' "$results/photo-delete-results.txt" ||
     ! grep -Fq 'PASS Delete Yes removed exact newly captured JPEG' "$results/photo-delete-results.txt" ||
     ! grep -Fq 'PASS all pre-existing JPEGs preserved' "$results/photo-delete-results.txt"; then
    echo "::warning::Photo Delete Yes probe completed without full acceptance evidence"
  fi
else
  echo "::warning::Photo Delete Yes acceptance failed; see photo-delete-results.txt"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_gallery_capacity_emulator.sh "$results"; then
  echo "::warning::Gallery capacity fixture failed"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_video_report_emulator.sh "$results"; then
  echo "::warning::Video report receiver test failed"
fi
# The video probe records a fresh clip, opens it in the in-house Gallery
# player, dispatches Android KEYCODE_BACK, and checks the player close log
# and foreground Activity. Summarize that same run; never send a second Back.
bash GPT-c2-from-reborn/tools/smoke_android_back_emulator.sh "$results"
if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
  {
    echo "### C2 Android 11 video and Back acceptance"
    echo
    cat "$results/video-suite-status.txt"
    echo
    cat "$results/android-back-results.txt"
    echo
    echo "### C2 Photo Delete Yes acceptance"
    echo
    if [[ -s "$results/photo-delete-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/photo-delete-results.txt" || true
    else
      echo "UNVERIFIED: photo deletion probe did not produce a report."
    fi
    echo
    echo "The emulator cannot establish Oppo handset recording, audible microphone capture, or physical system gesture behaviour."
  } >> "$GITHUB_STEP_SUMMARY"
fi
