GPT c2 from reborn — APK v1.33.0 / HTML reference v6.343

Native Android Java/Canvas — NOT an HTML WebView.

v1.33.0 — executable Main menu view and Organise
- Main menu > Options > Main menu view opens Nokia-style radio selection:
  List, Grid, Grid with labels, Tab.
- Each mode has a native 240x320 renderer:
  List scrolls five icon-and-label rows; Grid shows 3x3 firmware icons
  without captions; Grid with labels preserves the previous captioned 3x3
  screen; Tab shows the selected icon with nearby tabs and a large title.
- Direction keys behave according to view type: three-column row navigation
  in grid views; sequential movement in List and Tab.
- Main menu > Options > Organise is now functional. Choose an icon with
  centre Select or LSK, move the highlight to its destination, press
  Select/LSK again to reposition. An existing RSK press cancels a pending
  move or finishes organising.
- Icon order and selected view persist across app restarts, using validated
  Android SharedPreferences. Malformed/duplicate entries restore safe default.
- Reordering only changes screen position: opening an icon still launches
  Contacts, Messaging, Gallery, Settings, etc., correctly.
- Native home screen reference version label corrected from 6.342 to 6.343.
- Standalone Java model tests exercise modes, navigation, move/reorder,
  persistent-order validation and menu route correctness in GitHub Actions.

Previous changes retained:
- v1.32.0 working SMS Use detail extraction and actions, with parser tests.
- v1.31.0 live foreground unread-SMS refresh.
- v1.30.0 app-saved SMS templates and Saved items.
- v1.29.0 Nokia keypad Contacts Search.
- v1.28.0 pinned firmware-style Contact icons.
- v1.27.0 multi-number contact selection and action targeting.

Limitations:
- Modes are functional approximations based on Nokia S40 menu navigation.
  Exact pixel comparison with HTML v6.343 and real-device testing remain.
- Organise updates only the custom C2 launcher; Android launcher apps and
  phone data are unchanged.
- Debug signing still uses a fresh GitHub runner key. If Android rejects
  an in-place update, uninstall the older APK before installing this one.
- CI success verifies Java regression tests and Android packaging, not
  physical Nokia-keypad, touch or pixel-perfect behaviour.
