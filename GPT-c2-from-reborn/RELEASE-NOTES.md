GPT c2 from reborn — APK v1.15.0 / HTML reference v6.342

Native Android Java/Canvas recreation, no WebView. This is a partial port.

Gallery and Media audit/remediation:
- Replaced the hardcoded empty Images/Video clips pages with real user-selected
  local media collections. Selection uses Android ACTION_OPEN_DOCUMENT with no
  broad storage permissions. The original physical files are never changed.
- Gallery > Images displays chosen image titles in the Nokia list and renders
  selected images inside a native Canvas Nokia LCD preview, downsampled to
  screen size to limit memory usage.
- Gallery > Video clips displays chosen video titles; opening launches an
  installed Android video player using a granted content URI.
- In Albums' Options, Open, Add from phone, Details, Remove from gallery,
  Clear folder and Memory status have executable, scoped behavior.
- Deletions require Yes/No confirmation and remove only references from
  this APK's Gallery list. The original Android image or video remains intact.
- Selected URIs/titles are persisted in app data. The picker grants persisted
  read access where supported; revoked access produces an unavailable view.
- Root Gallery Options now supports View photos and View videos, Details,
  Memory status, and Open, while nonported commands report Not implemented.
- Root Media Options now routes View photos and View videos to the same
  native Gallery collections rather than claiming simulated media success.
- Invented memory-card free/used figures and other static media success claims
  were removed.

Existing v1.14.1 pinned Nokia Sans Regular/Bold typography, exact Nokia bitmap
icons, messaging/call-log/organiser repairs remain preserved.

LIMITATIONS:
- This is a user-selected Android document library, not a 1:1 Nokia firmware
  Gallery or a full device-media scanner.
- Gallery's remaining categories (Themes/Graphics/Tones/Recordings/etc),
  Camera/Video camera, FM Radio, Voice recorder and Equaliser are not implemented.
- App can browse media only after explicit file selection; preview and video
  launcher behavior must be verified on real devices.
- Exact HTML options, gallery animation, typography and pixel matching still
  require screen-by-screen/device testing.
