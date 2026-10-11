# Pixel 7 / Android 13 — Go to audit self-test and logs

Native APK **v1.36.0**; original HTML reference **v6.343**.

This addresses the four cases still unverified by the independent v1.34.0
emulator audit: persisted shortcut choices, Restore defaults confirmation,
hidden/reordered routes, and Video recorder launch.

## 1. Automated build-side checks

Open the [v1.36.0 GitHub Actions build](https://github.com/moshenuch/moshenuch/actions/workflows/build-GPT-c2-from-reborn.yml).
Choose its job **build**. The step is named:

**Test Go to state, dispatch, Restore defaults and video path**

The step performs:
- NativeGoToShortcuts Java regression tests using the EXACT
  `goto_order_v1` and `goto_visible_v1` JSON strings persisted in
  Android SharedPreferences. It saves an altered shortcut state, creates a
  NEW instance (simulated restart), reloads both arrays and checks equality.
- NativeGoToRouter dispatch with a spy handler, including hidden/reordered
  slot mapping, PHOTO capture flag `false` and VIDEO capture flag `true`.
- Restore defaults resets both visible flags and ordering. Invalid and
  duplicate IDs are rejected, including old JSON with whitespace.
- A Python source-wiring audit checks `C2PhoneView` really uses those tested
  serialization and routing methods, handles Restore defaults in confirmed
  Yes, cancels No, and forwards Video recorder through the Android
  `MediaStore.ACTION_VIDEO_CAPTURE` intent and correct request-code callback.

The tests are build gates: if any assertion fails, the APK is NOT published
by that run. Results appear in **Job Summary** and in an independently
downloadable workflow artifact:

`GPT-c2-from-reborn-GoTo-audit-v1.36.0`

The artifact contains plain-text `goto-audit-report.txt`.

**Interpretation:** These prove the model's serializing/deserializing and
routing functions and static Android code wiring. They do NOT prove
Android kept app data between emulator sessions or a camera app actually
recorded video. Those two claims need a device with retained storage and
a compatible camera app.

## 2. Optional debug runtime log (ADB)

To inspect real navigation, open an ADB terminal attached to your Pixel 7
Android 13 emulator or a physical Android phone:

```sh
adb logcat -c
adb logcat -s C2GoToAudit:I '*:S'
```

The event stream prints **only fixed shortcut IDs and state**, e.g.:

```text
C2GoToAudit: load restored=true order=[0,1,2,3,4,5,6,7,8] shown=[0,1,2,3,4,5,6,7,8]
C2GoToAudit: move from=4 to=3 order=[0,1,2,4,3,5,6,7,8] shown=[0,1,2,4,3,5,6,7,8]
C2GoToAudit: toggle item=0 enabled=false ...
C2GoToAudit: save order=[0,1,2,4,3,5,6,7,8] shown=[1,2,4,3,5,6,7,8]
C2GoToAudit: launch destination=4 ...
C2GoToAudit: capture VIDEO ...
C2GoToAudit: Android capture REQUEST VIDEO
C2GoToAudit: Android VIDEO intent accepted
C2GoToAudit: Android capture result VIDEO OK
```

The values are examples; your sequence may differ. A launched intent being
accepted does **not** guarantee a video was captured; the explicit result
line reports whether the Android camera returned OK or Cancel/Error.
The Gallery must also be checked for actual recorded video.

## 3. Manual audit procedure

1. Install the **v1.36.0 debug APK**. Open **Go to > Options > Organise**.
2. Move **Video recorder** above Camera and hide Lock keypad.
3. Return to Go to. Select the moved Video recorder shortcut.
4. Observe `launch destination=4` and `capture VIDEO`, then
   `Android capture REQUEST VIDEO` and intent acceptance/failure.
5. If the emulator offers a video camera, record a clip and return.
   Look for the `Android capture result VIDEO OK` line and
   **Gallery > Video clips** entry. If no handler exists, report as an
   environmental limitation, not automatically a Nokia UI bug.
6. From **Go to > Organise > Options > Restore defaults**, choose
   **No**, then verify menu order stays changed. Repeat and choose
   **Yes**: order and visible items return to nine defaults, with
   `restore confirmed YES` and `save` in the log.
7. When the emulator keeps app data across process restarts, force-stop
   and reopen the app. Observe `load restored=true` with the
   previously saved order/visibility. **If the emulator deletes app data
   between audit sessions, do not claim this case passed physically.**
   The build-side JSON roundtrip is the appropriate substitute.
8. Review the CI artifact for the exact checks and keep the downloaded
   report alongside your emulator findings.

## 4. Debug Contacts and SMS seed is already available

The original v1.34.0 audit predates **v1.35.0**, which added:

**Menu > Settings > Demo data (debug) > Seed / reset sample data**

This loads 12 fictional alphabetic contacts and 12 SMS in six threads,
with incoming/read/unread, outgoing, unsaved sender, multiple phone
numbers and Use detail extraction examples. All stay **inside the app**:
they are not real Android provider contacts/SMS. The app displays
`DEMO`, blocks real SMS sends, calls and contact edits, and can clear
the demo records with confirmation.

See [full emulator SMS/Contacts guide](EMULATOR-DEMO-AUDIT-GUIDE.md).

## Evidence classification

| Topic | What CI can establish | Requires device |
| --- | --- | --- |
| Saved order/visibility | Actual JSON encoding and simulated restart roundtrip | Android SharedPreferences surviving real process restart |
| Restore defaults | Reset model + static confirmation wiring | Visible Yes/No softkey flow |
| Hidden/reordered routes | All nine real shared dispatcher routes exercised | End-to-end touch/softkey actions |
| Video recorder | Router passes `true`, app selects video intent and callback | Compatible camera app actually opens, records and returns a clip |
| Seeded messages | Fixture and SMS parser regression tests | Nokia screen behaviour on emulator |
