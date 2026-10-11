#!/usr/bin/env python3
"""Restore Nokia Sans TTF faces used by the original C2 Reborn project.

This script downloads the upstream font bundle at the pinned HTML 6.342
commit and selects font faces by their embedded TrueType name table.
It fails closed if no Latin-capable family is identified.
"""
import io
import os
import struct
import zipfile
from pathlib import Path
from urllib.request import urlopen

SOURCE_COMMIT = "a8d1dab8273ff4dbdcb63cbc1ec1cac3354e10b8"
URL = ("https://raw.githubusercontent.com/mbaiforinsticnt-dev/c2-reborn/"
       + SOURCE_COMMIT + "/fonts/2-Nokia-Sans-family-13-fonts.zip")
TARGET = Path(__file__).resolve().parents[1] / "app/src/main/assets/nokia_font"
TARGET.mkdir(parents=True, exist_ok=True)

def names(ttf):
    """Read TrueType/OpenType name table (family, style, full name)."""
    if len(ttf) < 12:
        return ""
    table_count = struct.unpack_from(">H", ttf, 4)[0]
    off = None
    for i in range(table_count):
        t = 12 + 16 * i
        if t + 16 > len(ttf):
            return ""
        tag, checksum, offset, length = struct.unpack_from(">4sIII", ttf, t)
        if tag == b"name":
            off = offset
            break
    if off is None or off + 6 > len(ttf):
        return ""
    _, count, storage = struct.unpack_from(">HHH", ttf, off)
    found = []
    for i in range(count):
        p = off + 6 + i * 12
        if p + 12 > len(ttf):
            break
        plat, enc, lang, name_id, n, rel = struct.unpack_from(">HHHHHH", ttf, p)
        if name_id not in (1, 2, 4, 6, 16, 17):
            continue
        a = off + storage + rel
        if a + n > len(ttf):
            continue
        value = ttf[a:a+n]
        try:
            text = value.decode("utf-16-be" if plat in (0, 3) else "mac_roman")
        except (UnicodeError, LookupError):
            continue
        if text and text not in found:
            found.append(text)
    return " ".join(found)

with urlopen(URL, timeout=45) as response:
    archive = response.read()

candidate = []
with zipfile.ZipFile(io.BytesIO(archive)) as z:
    for entry in z.infolist():
        if entry.is_dir() or not entry.filename.lower().endswith((".ttf", ".otf")):
            continue
        data = z.read(entry)
        family = names(data)
        desc = (entry.filename + " " + family).lower()
        print("FONT SOURCE:", entry.filename, "|", family[:170])
        if "nokia" not in desc or "s40a" in desc:
            continue
        # All candidates must be embedded Nokia font faces, not script-only S40A.
        regular_score = (30 if "sans" in desc else 0) + (25 if "regular" in desc else 0) - (10 if "title" in desc else 0) - (25 if "bold" in desc or "semibold" in desc else 0)
        bold_score = (30 if "sans" in desc else 0) + (40 if "bold" in desc or "semi bold" in desc or "semibold" in desc else 0) + (5 if "title" in desc else 0)
        candidate.append((regular_score, bold_score, entry.filename, data, family))
if not candidate:
    raise RuntimeError("No usable Nokia Sans font in the pinned upstream zip")

# Verified archive entries from CI: the broad name-table score incorrectly
# preferred Condensed Medium and Bold Italic. Pin the actual face filenames.
regular_path = "nokia-sans-regular.ttf"
bold_path = "nokia-sans-bold.ttf"
regular = [item for item in candidate
           if Path(item[2]).name.lower() == regular_path]
bold = [item for item in candidate
        if Path(item[2]).name.lower() == bold_path]
if len(regular) != 1 or len(bold) != 1:
    raise RuntimeError("Missing or ambiguous Nokia Sans regular/bold source faces")
r = regular[0]
b = bold[0]
(TARGET / "regular.ttf").write_bytes(r[3])
(TARGET / "bold.ttf").write_bytes(b[3])
print("NOKIA FONT SELECTION: regular =", r[2], "bold =", b[2])
print("NOKIA FONT BYTES:", len(r[3]), len(b[3]))
