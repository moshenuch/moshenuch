GPT c2 from reborn — APK v1.40.0 / HTML reference v6.343

Native Android Java/Canvas application — NOT an HTML WebView.

v1.40.0 — repair clipped "Assigned: ..." notices and Phone detail overlap

Reported in the user's Pixel 4 / Android 11 emulator screenshot/logcat audit
of APK v1.38.0:
1. A transient "Assigned: Messaging" notification rendered without its
   first letter ("ssigned: Messaging"), and visually blended into the list
   behind it.
2. Settings > Phone, currently a placeholder detail page, displayed a
   title and body that overlapped visually.

Source-verified root causes:
- drawNotice set Paint.Align.CENTER but called drawWrappedText with x=48
  as if x were the LEFT boundary. This caused text to extend left of its
  intended position. Its background used translucent alpha=225, making
  underlying list labels bleed through.
- drawItemDetail used the first text baseline y=48, while drawStatus
  placed the header/title text baseline at y=37 and the header ended
  at y=41. The 13px body text ascenders overlapped the title region.

Fixes:
- Toast/notice now has a fully opaque background and left-aligned text,
  with x=48 correctly treated as the left boundary of wrapped text.
- The notice's two-line body is clipped inside its own dark background,
  not drawn across or visually mixed with the underlying settings list.
- All generic information detail screens (including Settings > Phone)
  now use a dedicated text baseline at y=72, below a divider at y=53
  and clear of the Nokia header title at y=37. Up to 11 text lines remain
  within the 286px upper softkey boundary.
- The Phone stub now explicitly says its phone settings have not yet
  been implemented and that no Android system settings are changed,
  instead of implying those settings were functional.
- Pure-Java NativeOverlayLayout coordinates, a geometry regression suite
  and a source-wiring audit run in CI before Android Gradle compilation.
- CI publishes artifact GPT-c2-from-reborn-Overlay-layout-audit-v1.40.0.
  The checks are build-side source/layout tests, not screenshots.

All other functionality is retained:
- v1.39.0 Conversations sender/SMS icon spacing corrected; the full first
  letter of Leah Hart, leading + of unknown numbers and NOKIA now have an
  8px margin after the SMS icon.
- v1.38.0 functioning My shortcuts Home key assignments.
- v1.37.0 SMS bulk Mark as read/unread semantics.
- v1.36.0 Go to build-side tests and debug log.
- v1.35.0 reversible debug seed with 12 contacts and 12 messages.

Manual Pixel 4 / Android 11 retest:
1. Settings > My shortcuts > Left selection key > Messaging; the entire
   "Assigned: Messaging" notice must display, including the first A.
2. Observe an opaque notice box; list text should not show through it.
3. Settings > Phone; title remains in the black status/title header and
   informational body begins well beneath it with no letter collisions.
4. Verify other item-detail pages remain readable and all Nokia softkeys
   remain accessible.
5. Confirm the earlier v1.39.0 conversation icon overlap fix persists.
6. Continue auditing Centre-key Menu rule, Restore defaults, Templates/Saved
   and Organise persistence.

Limitations:
- No Android emulator screenshots were produced by CI; visual fidelity
  and touch-level verification still require a Pixel emulator or handset.
- No Phone settings were implemented by this release; only inaccurate
  stub wording and layout are corrected.
- Ephemeral debug signing may require uninstalling a previous C2 APK
  before installing v1.40.0 if Android reports a signature conflict.
