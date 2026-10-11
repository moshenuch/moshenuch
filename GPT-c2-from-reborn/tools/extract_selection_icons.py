#!/usr/bin/env python3
"""Extract source-exact Nokia icon PNGs from the pinned C2 Reborn v6.342 icon pool."""
import base64
import json
from pathlib import Path
from urllib.request import urlopen

COMMIT = "a8d1dab8273ff4dbdcb63cbc1ec1cac3354e10b8"
URL = "https://raw.githubusercontent.com/mbaiforinsticnt-dev/c2-reborn/" + COMMIT + "/icon-pool/1-pool.json"
# Firmware catalogue calls these p0456 etc.; icon-pool keys omit the p prefix.
TARGETS = {"0456": "CHECK_OFF", "0457": "CHECK_ON",
           "0460": "RADIO_OFF", "0461": "RADIO_ON"}
OUT = Path(__file__).resolve().parents[1] / "app/src/main/assets/nokia_fw"
OUT.mkdir(parents=True, exist_ok=True)
with urlopen(URL, timeout=35) as response:
    data = json.loads(response.read().decode("utf-8"))
entries = data.get("icons", [])
if not isinstance(entries, list):
    raise RuntimeError("Unexpected icon pool structure")
found = {}
for item in entries:
    if not isinstance(item, dict):
        continue
    key = str(item.get("key", "")).strip().lower()
    if key not in TARGETS:
        continue
    if str(item.get("mime", "png")) != "png":
        raise RuntimeError("Source icon is not PNG: " + key)
    raw = item.get("image", "")
    if raw.startswith("data:image/"):
        raw = raw.split(",", 1)[-1]
    image = base64.b64decode(raw, validate=True)
    if image[:8] != bytes([137, 80, 78, 71, 13, 10, 26, 10]):
        raise RuntimeError("Invalid PNG magic: " + key)
    (OUT / (TARGETS[key] + ".png")).write_bytes(image)
    found[key] = len(image)
missing = sorted(set(TARGETS) - set(found))
if missing:
    nearby = [str(x.get("key", "")) for x in entries if isinstance(x, dict)
              and any(t in str(x.get("key", "")) for t in ("456", "457", "460", "461"))][:30]
    raise RuntimeError("Missing firmware sprites: " + str(missing) + "; similar keys: " + str(nearby))
print("Original Nokia PNGs restored:", found)
