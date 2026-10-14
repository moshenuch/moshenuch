#!/usr/bin/env bash
# Summarize the actual player Back acceptance exercised by smoke_video_emulator.sh.
# Do not send a second Back event after that probe has closed the player.
set -euo pipefail
report_dir="${1:-GPT-c2-from-reborn/build/emulator-smoke}"
mkdir -p "$report_dir"
result="$report_dir/android-back-results.txt"
video_result="$report_dir/video-results.txt"
back_log="$report_dir/video-back-log.txt"
play_log="$report_dir/video-playback-log.txt"

if [[ ! -s "$video_result" || ! -s "$back_log" || ! -s "$play_log" ]]; then
  echo "UNVERIFIED: video-player Back fixture/logs unavailable; no Android Back event sent." | tee "$result"
  exit 0
fi
if grep -Fq 'PASS in-house video player prepared selected recording' "$video_result" &&
   grep -Fq 'PASS Android KEYCODE_BACK routed to Nokia Gallery player close' "$video_result" &&
   grep -Fq 'PASS C2 remains foreground after Android Back' "$video_result" &&
   grep -Fq 'VIDEO_PREPARED size=' "$play_log" &&
   grep -Fq 'NOKIA_RSK_VIDEO_CLOSE' "$back_log"; then
  echo "PASS: Android Back closed the prepared Nokia video player and C2 stayed foreground." | tee "$result"
else
  echo "UNVERIFIED: in-house player, Back close, or foreground evidence incomplete." | tee "$result"
fi
