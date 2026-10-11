# Nokia firmware choice glyphs — APK 1.9.0 / HTML 6.342

Original source documentation: ICON-CATALOGUE.md passes 144–146.
Firmware sprites: p0456=CHECK_OFF, p0457=CHECK_ON, p0460=RADIO_OFF, p0461=RADIO_ON.
The upstream icon pool stores keys as 0456, 0457, 0460, 0461 (no p prefix).
The extractor downloads bytes at pinned HTML source commit
a8d1dab8273ff4dbdcb63cbc1ec1cac3354e10b8 and verifies PNG signature.

Native wiring:
- Contacts multi-mark and SMS multi-mark use the two source checkboxes.
- Conversations multi-mark preserves sender/recipient bubble icon on the left
  and renders checkbox status near the right edge.
- Call filtering Off/On screens use firmware radio glyphs instead of text-only rows.
- Source images cached in RebornSelectionIcons, sizes 18–24 px by page.

Limitations:
- All source-exact claims are limited to PNG identity; screenshot geometry
  and device behaviour still require device/emulator evidence.
- Other settings, menus, disabled states, Nokia font geometry and the
  nonimplemented Options controls remain to be audited/ported.
