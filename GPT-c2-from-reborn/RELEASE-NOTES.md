GPT c2 from reborn — APK v1.23.0 / HTML reference v6.342

Native Android Java/Canvas app. NOT a WebView or pixel-exact HTML port.

Keypad long-press / speed-dial reliability pass:
- The on-screen 360x780 Nokia keypad now handles ACTION_DOWN / MOVE / UP,
  CANCEL and secondary pointers, not just raw ACTION_UP.
- Holding a home-screen digit 2–9 for 700 ms triggers a speed-dial lookup.
  Ordinary short taps remain unchanged: a tap enters the selected digit.
- Long holds are disabled when locked, displaying Options, showing a
  confirmation dialog or leaving the home screen.
- Dragging off the key or cancelling a gesture cancels the pending hold.
  Releasing after a completed hold does not also enter the pressed digit.
- When an assigned speed dial is enabled, holding its digit fills the
  native dial screen with the assigned number. A separate Call key
  press is required to initiate an Android call.
- Call settings > Speed dialling now opens a real Off/On selector with
  source Nokia radio artwork, stores the preference in app settings,
  and shows the current value in the Call settings list.
- Matches the companion upstream ground-up screen's distinction between
  enabled/disabled speed dialling and opening an offline dial screen.
  Default remains Off, as in that reference. The full 3.7 MB upstream
  main index HTML was not compared byte-for-byte in this update.
- Calling code no longer navigates to a false active-call screen when
  Android rejects the call intent. If CALL_PHONE is unavailable, the
  external Android dialler is opened and the app explicitly states
  that a call has not yet been placed.

Earlier v1.22 app-local Groups, v1.21 speed-dial assignments, messaging,
Gallery, music, recorder and other native functionality remain included.

KNOWN LIMITATIONS AND SAFETY:
- This long-press behaviour applies to the APK's on-screen numeric keys;
  Android physical hardware key long-press mapping is not yet implemented.
- Holding a speed-dial digit DOES NOT place a phone call automatically.
  The user must explicitly press Call from the filled native dial screen.
- Android's call status remains owned by the telephony app; the Nokia call
  screen does not yet observe authoritative connected/disconnected events.
- Visual and behavioural comparison against the giant main HTML and
  physical-device touchscreen tests remain outstanding.
