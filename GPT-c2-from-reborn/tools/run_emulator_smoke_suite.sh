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
     ! grep -Fq 'PASS C2 remains foreground after Delete No' "$results/photo-delete-results.txt" ||
     ! grep -Fq 'PASS Delete Yes removed exact newly captured JPEG' "$results/photo-delete-results.txt" ||
     ! grep -Fq 'PASS C2 remains foreground after Delete Yes' "$results/photo-delete-results.txt" ||
     ! grep -Fq 'PASS all pre-existing JPEGs preserved' "$results/photo-delete-results.txt"; then
    echo "::warning::Photo Delete Yes probe completed without full acceptance evidence"
  fi
else
  echo "::warning::Photo Delete Yes acceptance failed; see photo-delete-results.txt"
fi
if ! bash GPT-c2-from-reborn/tools/smoke_gallery_capacity_emulator.sh "$results"; then
  echo "::warning::Gallery capacity fixture failed"
fi
add_to_contact_core_failed=0
if bash GPT-c2-from-reborn/tools/smoke_add_to_contact_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Add to contact appended exact dialled number' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS pre-existing contact number' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS C2 remains foreground after Add to contact' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS UK international-form duplicate refused' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS UK 0044-form duplicate refused' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS duplicate Add to contact refused' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS picker cancel via Back left provider unchanged' "$results/add-to-contact-results.txt"; then
    echo "::error::Add to contact core acceptance incomplete; see add-to-contact-results.txt"
    add_to_contact_core_failed=1
  fi
  # Section E (WRITE_CONTACTS denied) is warning-only per the v2.03.x
  # timebox: the startup permission sheet timing made it flaky twice.
  if ! grep -Fq 'PASS Add to contact refused without WRITE_CONTACTS' "$results/add-to-contact-results.txt"; then
    echo "::warning::Section E (WRITE_CONTACTS denied) coverage incomplete; warning-only per timebox"
  fi
  if ! grep -Fq 'PASS Conversations Add to contact appended sender number' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS alphanumeric SMS sender refused before picker' "$results/add-to-contact-results.txt"; then
    echo "::warning::Conversations Add to contact coverage incomplete (SMS shell seeding is best-effort); see add-to-contact-results.txt"
  fi
else
  echo "::error::Add to contact core acceptance failed; see add-to-contact-results.txt"
  add_to_contact_core_failed=1
fi
# Mirror the probe report into the job log so failures are diagnosable
# without downloading the artifact.
if [[ -s "$results/add-to-contact-results.txt" ]]; then
  echo "===== add-to-contact-results.txt ====="
  cat "$results/add-to-contact-results.txt"
  echo "===== end add-to-contact-results.txt ====="
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
    echo "### C2 Add to contact acceptance"
    echo
    if [[ -s "$results/add-to-contact-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/add-to-contact-results.txt" || true
    else
      echo "UNVERIFIED: Add to contact probe did not produce a report."
    fi
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
if [[ "${add_to_contact_core_failed:-0}" == "1" ]]; then
  exit 1
fi
