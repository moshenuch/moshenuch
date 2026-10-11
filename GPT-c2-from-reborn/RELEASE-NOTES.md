GPT c2 from reborn — APK v1.38.0 / HTML reference v6.343

Native Android Java/Canvas; NOT a WebView.

v1.38.0 — executable Settings > My shortcuts

Previously, the My shortcuts settings displayed Left selection key, Right
selection key, Navigation key and Home screen key, but every item opened an
inert information page ("Shortcut assignment is simulated natively").

- All four My shortcuts items now open functional native assignment screens.
- Left selection key and Right selection key can open distinct user-selected
  destinations. Their on-screen Nokia softkey labels update immediately.
- Navigation key opens four assignments: Up, Down, Left and Right. Those
  keys work directly on Home. The original unassigned directions remain
  the default, so existing home navigation is unchanged until configured.
- Home screen key configures centre OK. The default remains Menu.
- The user may select one of: Menu, Go to, Names, Messaging, Contacts,
  Gallery, Camera, Video recorder, Organiser, Calculator, Alarm clock,
  Settings, Profiles, None.
- Native Camera/Video recorder actions use the existing Android capture
  launch; other options navigate to their existing native screens.
- The main Menu must stay accessible via at least one Home key. Attempts
  to unassign the LAST Menu binding are rejected. To customize centre OK,
  first assign Menu to a different Home key; then reassign centre OK.
- Settings > My shortcuts > Options > Restore defaults presents Yes/No
  confirmation and returns Left=Go to, Right=Names, Centre=Menu and
  all four navigation keys=None, without touching the user's phone data.
- All seven key bindings persist across app restarts under an app-private
  versioned SharedPreferences key. Malformed preference data resets to
  safe defaults. Home bindings do not modify global Android OS buttons.
- Selecting key assignments uses the existing Nokia bitmap radio icons.
- Debuggable builds emit fixed numeric binding arrays under the safe
  C2HomeKeys Android logcat tag. No contact/SMS data is logged.
- New build-side regression tests prove original default behaviour, exact
  JSON save/reload after simulated app restart, assigned action labels,
  navigation-slot mapping, Menu-reachability invariant, preference corruption
  and reset. Python source-wiring audits verify live Settings screens,
  handler dispatch and confirmation. CI publishes a separate report artifact.

Previously implemented functionality retained:
- v1.37.0 correct SMS conversation Mark all/read/unread bulk semantics.
- v1.36.0 Go to shortcut self-tests, persistence and capture diagnostics.
- v1.35.0 debuggable 12-Contact / 12-SMS fixture seed.
- v1.34.0 Go to Organise with safe persistent order and real capture actions.
- v1.33.0 four main menu layouts and Menu Organise.
- v1.32.0 SMS Use detail; v1.29.0 Nokia Contacts search.

Verification/limitations:
- Exact HTML v6.343 pixel parity and physical Android key handling are not
  certified by Java CI or source audits. The key-assignment UI is a native,
  functional interpretation of My shortcuts, not claimed screenshot-exact.
- Android Back/End remain standard system/home actions. Home assignments
  are active only while the native C2 Home screen is visible.
- Changing centre OK from Menu is blocked until another Home key has Menu.
- The native app still uses GitHub-hosted debug signing, which changes
  certificate between runner builds. Installation may require uninstalling
  the prior APK; stable signing is a separate unconfigured PR.
