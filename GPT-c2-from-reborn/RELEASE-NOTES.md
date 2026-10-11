GPT c2 from reborn — APK v1.34.0 / HTML reference v6.343

Native Android Java/Canvas (NOT a WebView).

v1.34.0 — executable Go to > Organise and real camera shortcuts
- Go to > Options > Organise now opens a working Nokia-style organiser screen
  listing all nine Go to shortcut destinations with firmware checkboxes.
- Select/Toggle shows or hides individual shortcuts. At least one shortcut
  must remain shown to avoid an unusable Go to list.
- Organise > Options > Move up / Move down shifts the highlighted shortcut
  within the Go to list. Restore defaults uses Yes/No confirmation.
- Go to shortcut order and visibility persist in Android SharedPreferences.
  Corrupt, duplicate, empty or invalid stored data reverts to safe defaults.
- Go to uses stable destination IDs rather than list positions. Hiding or
  reordering Lock keypad, Profiles, Camera, Alarm clock and the other
  shortcuts will never accidentally open the wrong destination.
- Go to > Camera now launches the existing native Android photo capture
  workflow instead of displaying a placeholder detail page.
- Go to > Video recorder now launches the existing native Android video
  capture workflow, with successful results added to native Gallery.
- Return navigation clamps selection if the previously highlighted shortcut
  was hidden; organisers are explicitly app-local and do not change
  Android system launcher shortcuts.
- Java smoke tests now verify visibility, reordering, persistence validation,
  correct destination IDs, last-shortcut protection, and default reset.

Earlier improvements preserved:
- v1.33.0 native Main menu modes (List, Grid, Grid with labels, Tab), Organise.
- v1.32.0 SMS Use detail and parser CI tests.
- v1.31.0 foreground live SMS unread icon refresh.
- v1.30.0 saved SMS templates and Messages Saved items.
- v1.29.0 Contacts multi-tap search.
- v1.28.0 pinned Nokia contact icon assets.

Limitations:
- This is native functional parity for Go to shortcuts, not an assertion
  that every icon and font matches the v6.343 HTML pixel-for-pixel.
- Camera still relies on the user's Android camera provider; some phones
  may lack a compatible camera/video intent handler.
- Some other Settings > My shortcuts assignments remain placeholders;
  this version targets Go to > Organise specifically.
- GitHub CI verifies Java model tests and APK packaging, not physical
  camera operation, SMS behavior or handset screenshot parity.
- The legacy GitHub debug-signing process may require uninstalling an
  earlier C2 APK before installation; stable signing is a separate draft PR.
