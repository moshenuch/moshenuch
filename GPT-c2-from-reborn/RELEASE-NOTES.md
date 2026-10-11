GPT c2 from reborn — APK v1.28.0 / HTML reference v6.343

Native Android Java/Canvas, not a WebView.

Changes in v1.28.0 — source-exact Contact details artwork
- Replaced the placeholder circle on the native Contact details screen with
  the original CONTbust portrait PNG extracted directly from the HTML
  v6.343 listIcons map. The original 84x84 asset is rendered at 42x42 Nokia
  CSS-screen pixels in the correct upper-right contact header position.
- The Nokia CONTphone telephone PNG is shown beside every contact number,
  including selected secondary phone-number rows.
- Restored contact-name alignment and the light selected-number row appearance
  of the upstream Contact details screen. Names/long phone-number strings
  are ellipsized to fit the 240-wide native LCD without cropping.
- Retained v1.27.0 Up/Down selection and scrolling through multiple phone
  numbers; all Contact detail actions continue to use the selected number.
- Exact PNGs are cached and decoded inside native Android Java. No network
  access, WebView wrapper or replacement stock icons are involved.

Upstream artwork provenance:
- HTML blob: 2aabdff3423d1fdf76d1e1c5b7608348f916ce60
- Source variables: listIcons.CONTbust (84x84) and listIcons.CONTphone (56x56).
- Published website version: v6.343 (2026-10-11).

Retained previous functions:
- v1.27.0 individually selectable/usable multi-number Contacts.
- v1.26.0 full contact alias matching.
- v1.24–1.25 SMS read/unread, menu actions and contact saving.
- Gallery, Groups, media and speed-dial native Java components.

Limitations:
- Only Contact details' two sprites are verified source-exact in this update.
  Other screens and menus still need icon-by-icon visual comparison.
- Device screenshots and interactions have not been tested on actual hardware.
- CI builds and checks Android packaging; it is not a physical handset test.
- APK signing remains ephemeral until separate signing PR #2 is configured;
  uninstall previous version if Android reports a signing conflict.
