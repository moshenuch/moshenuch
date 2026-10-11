GPT c2 from reborn — APK v1.21.0 / HTML reference v6.342

Native Android Java/Canvas (no WebView). Still a partial Nokia C2-01 recreation.

Contacts / Speed dials native audit and implementation:
- Contacts > Speed dials is now a functional list of slots 2–9.
- Each slot shows the saved app-local contact name or number. An empty
  slot opens a contact selection list; choosing a contact assigns its number.
- A contact's Options > Speed dial opens the native slot selection screen,
  so numbers can also be assigned directly from contact details.
- Speed dials > Options provides working Details, Call, Assign / Change,
  Remove (with Yes/No confirmation) and local Memory status.
- The Call hardware-style key also starts a call to the currently assigned
  slot when on the Speed dials list. No call happens during assignment.
- Slot-to-number mappings persist in app SharedPreferences JSON; refreshes
  of the Android Contacts list do not invalidate mappings by list index.
- Speed dials are APP-LOCAL and never modify Android's system speed-dial
  database or contacts provider. Removing a slot never deletes a contact.
- Contact Options > Use number fills the Nokia-style dialling screen with
  the selected number without placing a call.
- Contact Options > Copy number now writes the number into Android's
  clipboard, instead of only showing a transient toast.
- Contacts > Synchronise all no longer claims a cloud sync succeeded.
  It refreshes the readable Android contacts and explicitly says cloud
  synchronisation was not performed; if permission is unavailable, it
  reports that clearly.
- Contacts > Memory status reports actual visible contact count/source
  and app speed dial assignments, without invented phone/SIM capacity.
- Unimplemented Groups, SIM service-number, mass deletion and settings
  screens now state their limitations without claiming work was done.

Features retained: v1.20 playback audio focus/repeat/shuffle,
v1.19 real audio library/Equaliser, Gallery, Voice Recorder, Camera,
SMS drafts/marking and original Nokia font/icon assets.

LIMITATIONS:
- Nokia press-and-hold digit dialling, group memberships, cloud account
  synchronisation, Android system speed-dial integration and many nested
  Contacts options remain incomplete.
- Android call placement requires system permission and compatible device.
- No Android emulator or physical-device screenshot tests in this pass;
  successful Java compilation alone does not establish UI/HTML parity.
