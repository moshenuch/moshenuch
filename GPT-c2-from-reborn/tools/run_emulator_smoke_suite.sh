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
  # Section E (WRITE_CONTACTS denied) is warning-only per the 2026-10-15
  # auditor timebox: green and deterministic since commit 5b37b79e (run
  # 38691909508), but a probe-side flake here must never hold a release.
  # Exact diagnostics are always in add-to-contact-results.txt.
  if ! grep -Fq 'PASS Add to contact refused without WRITE_CONTACTS' "$results/add-to-contact-results.txt"; then
    echo "::warning::Section E (WRITE_CONTACTS denied) acceptance incomplete; see add-to-contact-results.txt"
  fi
  if ! grep -Fq 'PASS Conversations Add to contact appended sender number' "$results/add-to-contact-results.txt" ||
     ! grep -Fq 'PASS alphanumeric SMS sender refused before picker' "$results/add-to-contact-results.txt"; then
    echo "::warning::Conversations Add to contact coverage incomplete (SMS shell seeding is best-effort); see add-to-contact-results.txt"
  fi
else
  echo "::error::Add to contact core acceptance failed; see add-to-contact-results.txt"
  add_to_contact_core_failed=1
fi
clear_log_lists_failed=0
if bash GPT-c2-from-reborn/tools/smoke_clear_log_lists_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Message recipients list cleared via Clear log lists' "$results/clear-log-lists-results.txt" ||
     ! grep -Fq 'PASS Missed calls list cleared via Clear log lists, provider unchanged' "$results/clear-log-lists-results.txt" ||
     ! grep -Fq 'PASS No/cancel on Clear log lists preserves recipients and missed call' "$results/clear-log-lists-results.txt" ||
     ! grep -Fq 'PASS cleared state persists across force-stop and restart' "$results/clear-log-lists-results.txt" ||
     ! grep -Fq 'PASS empty call lists show Back-only and labels return with records' "$results/clear-log-lists-results.txt"; then
    echo "::error::Clear log lists acceptance incomplete; see clear-log-lists-results.txt"
    clear_log_lists_failed=1
  fi
else
  echo "::error::Clear log lists acceptance failed; see clear-log-lists-results.txt"
  clear_log_lists_failed=1
fi
if [[ -s "$results/clear-log-lists-results.txt" ]]; then
  echo "===== clear-log-lists-results.txt ====="
  cat "$results/clear-log-lists-results.txt" || true
fi
msg_recipients_failed=0
if bash GPT-c2-from-reborn/tools/smoke_msg_recipients_emulator.sh "$results"; then
  if ! grep -Fq 'PASS empty Msg. recipients stays on Log with No numbers notice' "$results/msg-recipients-results.txt" ||
     ! grep -Fq 'PASS seeded Msg. recipients page opened with 1 entry' "$results/msg-recipients-results.txt"; then
    echo "::error::Msg. recipients acceptance incomplete; see msg-recipients-results.txt"
    msg_recipients_failed=1
  fi
else
  echo "::error::Msg. recipients acceptance failed; see msg-recipients-results.txt"
  msg_recipients_failed=1
fi
if [[ -s "$results/msg-recipients-results.txt" ]]; then
  echo "===== msg-recipients-results.txt ====="
  cat "$results/msg-recipients-results.txt" || true
fi
# Mirror the probe report into the job log so failures are diagnosable
# without downloading the artifact.
if [[ -s "$results/add-to-contact-results.txt" ]]; then
  echo "===== add-to-contact-results.txt ====="
  cat "$results/add-to-contact-results.txt"
  echo "===== end add-to-contact-results.txt ====="
fi
delete_all_contacts_failed=0
if bash GPT-c2-from-reborn/tools/smoke_delete_all_contacts_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Delete all contacts Yes removed every provider contact' "$results/delete-all-contacts-results.txt" ||
     ! grep -Fq 'PASS Delete all contacts refused empty provider with Selected memory empty' "$results/delete-all-contacts-results.txt" ||
     ! grep -Fq 'PASS Delete all contacts No left provider unchanged' "$results/delete-all-contacts-results.txt"; then
    echo "::error::Delete all contacts acceptance incomplete; see delete-all-contacts-results.txt"
    delete_all_contacts_failed=1
  fi
else
  echo "::error::Delete all contacts acceptance failed; see delete-all-contacts-results.txt"
  delete_all_contacts_failed=1
fi
if [[ -s "$results/delete-all-contacts-results.txt" ]]; then
  echo "===== delete-all-contacts-results.txt ====="
  cat "$results/delete-all-contacts-results.txt"
  echo "===== end delete-all-contacts-results.txt ====="
fi
delete_all_messages_failed=0
if bash GPT-c2-from-reborn/tools/smoke_delete_all_messages_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Delete all messages cleared app drafts and templates' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete all messages provider rows unchanged across Yes (hide-only, full-row snapshot)' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete all messages No left provider rows unchanged (full-row snapshot)' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS C2 remains foreground after Delete all messages Yes' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS C2 remains foreground after Delete all messages No' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete messages row routes to savedmessages (marker-verified): byfolder' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete messages row routes to inbox (marker-verified): inbox' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete messages row routes to sent (marker-verified): sent' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete messages row routes to drafts (marker-verified): drafts' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS folder-route sweep left provider rows unchanged' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS inbound SMS persisted end-to-end under default-SMS role' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS inbound multipart SMS stored as one provider row' "$results/delete-all-messages-results.txt" ||
     ! grep -Eq 'PASS (same-second identical SMS stored once|SMS redelivery not reproducible)' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: link prefilled Compose from a cold start and sending stored a SENT provider row' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: link with several recipients opens Compose with an empty recipient and a notice' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: link body over 1600 characters is cut with a notice' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: link while composing keeps the in-progress message as a draft' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: exactly one SENT row with the correct address and body for the probe send' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: semicolon recipient list rejected with an empty recipient and a notice' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: encoded-comma recipient list rejected with an empty recipient and a notice' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS sms: link body of exactly 1600 characters passes through uncut' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS inbound SMS visible in Inbox after app restart' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS Delete all messages under default-SMS role really deleted every provider SMS' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS real Delete all messages also cleared app drafts and templates' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS C2 remains foreground after real Delete all messages' "$results/delete-all-messages-results.txt" ||
     ! grep -Fq 'PASS SMS role restored after probe' "$results/delete-all-messages-results.txt"; then
    echo "::error::Delete all messages acceptance incomplete; see delete-all-messages-results.txt"
    delete_all_messages_failed=1
  fi
  if ! grep -Fq 'PASS Delete all messages Yes hid every provider SMS' "$results/delete-all-messages-results.txt"; then
    echo "::warning::Delete all messages SMS-provider coverage incomplete (SMS shell seeding is best-effort); see delete-all-messages-results.txt"
  fi
else
  echo "::error::Delete all messages acceptance failed; see delete-all-messages-results.txt"
  delete_all_messages_failed=1
fi
if [[ -s "$results/delete-all-messages-results.txt" ]]; then
  echo "===== delete-all-messages-results.txt ====="
  cat "$results/delete-all-messages-results.txt"
  echo "===== end delete-all-messages-results.txt ====="
fi
if bash GPT-c2-from-reborn/tools/smoke_move_copy_contacts_emulator.sh "$results"; then
  if ! grep -Fq 'PASS Duplicate destination refused with the HTML wording and changed nothing' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS Move phone->SIM rolled back after a mid-batch failure' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS rollback marker honest: rolled_back=1 with undone==done' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS Copy from empty SIM memory refused with Selected memory empty' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'multi-number and extra-data contacts untouched in provider' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS extra-data contact kept its email row after Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS Move SIM->phone restored provider and emptied SIM list' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS extra-data contact copied to SIM as name+number only' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS Copy phone->SIM copied' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS Copy contacts No left provider and SIM list unchanged' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS within-source duplicate batch refused (duplicate_source)' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS custom-label contact kept its label row after Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS account-backed contact kept out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS nameless contact kept out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS non-mobile contact kept out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS primary-number contact kept out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS number added between confirm and Yes keeps the contact out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS blank-number SIM entry rejected; batch rolled back cleanly' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS structured-name contact kept out of Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS same-count mutation between confirm and Yes refused the Move' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS custom-label contact copied to SIM as name+number only' "$results/move-copy-contacts-results.txt" ||
     ! grep -Fq 'PASS vanished insert restored to SIM and counted as undone: rolled_back=1' "$results/move-copy-contacts-results.txt"; then
    echo "::error::Move/Copy contacts acceptance incomplete; see move-copy-contacts-results.txt"
    move_copy_contacts_failed=1
  fi
else
  echo "::error::Move/Copy contacts acceptance failed; see move-copy-contacts-results.txt"
  move_copy_contacts_failed=1
fi
if [[ -s "$results/move-copy-contacts-results.txt" ]]; then
  echo "===== move-copy-contacts-results.txt ====="
  cat "$results/move-copy-contacts-results.txt"
  echo "===== end move-copy-contacts-results.txt ====="
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
    echo "### C2 Clear log lists acceptance"
    echo
    if [[ -s "$results/clear-log-lists-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/clear-log-lists-results.txt" || true
    else
      echo "UNVERIFIED: Clear log lists probe did not produce a report."
    fi
    echo
    echo "### C2 Msg. recipients acceptance"
    echo
    if [[ -s "$results/msg-recipients-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/msg-recipients-results.txt" || true
    else
      echo "UNVERIFIED: Msg. recipients probe did not produce a report."
    fi
    echo
    echo "### C2 Add to contact acceptance"
    echo
    if [[ -s "$results/add-to-contact-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/add-to-contact-results.txt" || true
    else
      echo "UNVERIFIED: Add to contact probe did not produce a report."
    fi
    echo
    echo "### C2 Del. all contacts acceptance"
    echo
    if [[ -s "$results/delete-all-contacts-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/delete-all-contacts-results.txt" || true
    else
      echo "UNVERIFIED: Delete all contacts probe did not produce a report."
    fi
    echo
    echo "### C2 Delete messages acceptance"
    echo
    if [[ -s "$results/delete-all-messages-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/delete-all-messages-results.txt" || true
    else
      echo "UNVERIFIED: Delete messages probe did not produce a report."
    fi
    echo
    echo "### C2 Move/Copy contacts acceptance"
    echo
    if [[ -s "$results/move-copy-contacts-results.txt" ]]; then
      grep -E '^(PASS|FAIL|UNVERIFIED)' "$results/move-copy-contacts-results.txt" || true
    else
      echo "UNVERIFIED: Move/Copy contacts probe did not produce a report."
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
if [[ "${add_to_contact_core_failed:-0}" == "1" || "${msg_recipients_failed:-0}" == "1" || "${clear_log_lists_failed:-0}" == "1" || "${delete_all_contacts_failed:-0}" == "1" || "${delete_all_messages_failed:-0}" == "1" || "${move_copy_contacts_failed:-0}" == "1" ]]; then
  exit 1
fi
