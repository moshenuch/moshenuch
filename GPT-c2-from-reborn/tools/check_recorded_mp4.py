#!/usr/bin/env python3
"""Independent ISO BMFF/ffprobe validation for the CI-recorded C2 video.

Checks the file itself, not C2's saved toast or MediaMetadataRetriever logs.
Checks a real moov atom and one decodable video frame/audio sample when
ffprobe/ffmpeg are present. A virtual microphone can provide silent audio.
"""
import json
import shutil
import struct
import subprocess
import sys
from pathlib import Path


def top_level_atoms(path):
    size = path.stat().st_size
    if size < 1024:
        raise ValueError(f"MP4 is too small to be usable: {size} bytes")
    atoms = []
    with path.open("rb") as handle:
        offset = 0
        while offset < size:
            if size - offset < 8:
                raise ValueError(f"Truncated MP4 atom at offset {offset}")
            handle.seek(offset)
            length, kind = struct.unpack(">I4s", handle.read(8))
            header = 8
            if length == 1:
                if size - offset < 16:
                    raise ValueError("Truncated extended atom length")
                length = struct.unpack(">Q", handle.read(8))[0]
                header = 16
            elif length == 0:
                length = size - offset
            if length < header or length > size - offset:
                raise ValueError(f"Invalid {kind!r} atom at {offset}: {length}")
            atoms.append((kind.decode("latin1"), offset, length))
            offset += length
    return atoms, size


def run(argv, timeout=35):
    result = subprocess.run(argv, text=True, capture_output=True,
                            timeout=timeout, check=False)
    if result.returncode:
        raise ValueError(f"{argv[0]} failed (exit {result.returncode}): "
                         + result.stderr[-800:])
    return result.stdout


def validate(path):
    atoms, size = top_level_atoms(path)
    kinds = [kind for kind, _, _ in atoms]
    for required in ("ftyp", "mdat", "moov"):
        if required not in kinds:
            raise ValueError(f"Missing required MP4 atom: {required} "
                             f"(found {kinds})")
    print(f"PASS complete ISO BMFF MP4 ({size} bytes); "
          f"top-level atoms: {', '.join(kinds)}")

    if not shutil.which("ffprobe"):
        print("WARN ffprobe unavailable: streams and playback not checked")
        return

    probe = json.loads(run([
        "ffprobe", "-v", "error", "-show_streams", "-show_format",
        "-of", "json", str(path)
    ]))
    streams = probe.get("streams", [])
    video = [s for s in streams if s.get("codec_type") == "video"]
    audio = [s for s in streams if s.get("codec_type") == "audio"]
    duration = float(probe.get("format", {}).get("duration") or 0)
    if not video or not audio:
        raise ValueError(f"Expected video and audio streams, found "
                         f"{[s.get('codec_type') for s in streams]}")
    if duration < 1 or int(video[0].get("width") or 0) <= 0 or (
            int(video[0].get("height") or 0) <= 0):
        raise ValueError(f"Bad video duration or dimensions: {duration}s")
    print(f"PASS ffprobe video: {video[0].get('codec_name')} "
          f"{video[0].get('width')}x{video[0].get('height')}; "
          f"audio: {audio[0].get('codec_name')}; duration: {duration:.2f}s")

    if shutil.which("ffmpeg"):
        run(["ffmpeg", "-nostdin", "-v", "error", "-i", str(path),
             "-map", "0:v:0", "-frames:v", "1", "-f", "null", "-"])
        run(["ffmpeg", "-nostdin", "-v", "error", "-i", str(path),
             "-map", "0:a:0", "-t", "1", "-f", "null", "-"])
        print("PASS ffmpeg decoded one video frame and one second of audio")
    else:
        print("WARN ffmpeg unavailable: decode check skipped")
    print("NOTE a decoded audio track can still be silent in an emulator.")


if __name__ == "__main__":
    try:
        validate(Path(sys.argv[1]))
    except (IndexError, OSError, ValueError, subprocess.TimeoutExpired) as e:
        print("FAIL MP4 container/stream validation: " + str(e),
              file=sys.stderr)
        sys.exit(1)
