# Contacts / Speed dials audit — APK v1.21.0 / HTML reference v6.342

## Confirmed defects in previous native APK
1. Contacts > Speed dials always opened a generic details page claiming
   "No speed dials assigned", with no ability to assign a number.
2. Contact details > Options > Speed dial only displayed an inert notice.
3. Contacts > Synchronise all reported "Synchronisation complete" without
   performing any cloud, SIM or contact synchronization.
4. Contact Options > Copy number did not copy anything to Android's clipboard;
   it merely displayed the phone number. Use number did not open dial editor.
5. Contacts Memory status was static "Phone memory / SIM card", without
   actual retrieved contact counts or any memory measurement.
6. Contacts > Groups / SIM service numbers / mass delete reported static
   language that could be mistaken for real provider operations.

## Changes
- Two native slot-list routes: `speedDials` (2–9) and
  `speedDialAssign` (assign directly from a selected contact).
- `speedDialPicker` selects a current Android/app-local contact number
  to assign to the selected slot.
- Persisted mapping `speed_dials_v1` stores eight independent phone-number
  assignments as JSON SharedPreferences; no Android Contacts database writes.
- Correct slot labels use `contactNameFor(number)` on each display, so
  changed contact names display dynamically.
- Executable options: Call, Details, Assign / Change, Remove with existing
  Nokia-style Yes/No confirmation, and Memory status.
- Contacts refresh now accurately reports an Android provider re-read and
  explicitly states cloud sync was NOT performed; permission denial is
  surfaced, rather than claiming successful sync.
- Actual Android ClipboardManager is used only on explicit Copy number.
  Use number opens the dial screen with the number pre-filled (no call).
- Unsupported high-impact operations avoid pretending to modify Android.

## Targeted QA (not yet carried out on a handset)
1. With 2+ contacts, assign slots 2 and 8 from Contacts > Speed dials.
   Restart app, check both assignments persist and resolve contact names.
2. From Contact details > Speed dial assign slot 9, then change the phone
   contact's display name and refresh; check number mapping survives.
3. Open assigned slot: verify Details shows correct number and scope.
   Select Call and test Android permission/telephone route.
4. Remove slot 8 > No retains number, Yes removes it; original Android
   contact must remain unchanged.
5. Copy number into clipboard using Contacts > Options, paste elsewhere.
   Use number should show phone number in native dial screen, not auto-call.
6. Verify Synchronise all never asserts a cloud sync happened.
7. Test denied Contacts permission and empty contact list paths.
8. Visually compare menus/softkey behaviour with HTML v6.342 and original
   Nokia handset. These UI parity checks have not been completed.

## Explicit omissions
Groups, speed-dial long-press from Home, SIM sync, cloud accounts and
mass provider Contacts modifications remain unavailable. Build success
does not demonstrate physical device functionality.
