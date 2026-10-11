#!/usr/bin/env python3
"""CI guard: tested My shortcuts model must be wired into native Nokia screens.

This statically verifies the production Java code paths; physical touch and
Android camera execution still require real emulator/handset testing.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
source = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text(
    encoding="utf-8"
)
model = (root / "app/src/main/java/com/moshenuch/c2reborn/NativeHomeShortcuts.java").read_text(
    encoding="utf-8"
)

def method(signature: str) -> str:
    i = source.find(signature)
    if i < 0:
        raise AssertionError("Missing production method: " + signature)
    brace = source.find("{", i + len(signature))
    if brace < 0:
        raise AssertionError("Missing body: " + signature)
    depth = 0
    for n in range(brace, len(source)):
        if source[n] == "{":
            depth += 1
        elif source[n] == "}":
            depth -= 1
            if depth == 0:
                return source[brace:n+1]
    raise AssertionError("Unterminated method " + signature)

def check(label: str, ok: bool) -> None:
    if not ok:
        raise AssertionError("FAIL: " + label)
    print("PASS: " + label)

load = method("private void loadState()")
save = method("private void saveHomeShortcuts()")
open_selected = method("private void openSelected()")
press = method("private void press(String key)")
dispatch = method("private void openAssignedHomeAction(int action)")
softbar = method("private String[] softLabels()")
items = method("private String[] itemsFor(String pg)")
option_list = method("private void openOptionsForPage()")
option_choose = method("private void chooseOption()")
confirm = method("private void executeConfirmed()")

check("Original shortcut settings screen still accessible from Settings",
      'else if (sel == 6) go("shortcuts")' in open_selected)
check("Each of four shortcut settings rows opens a live assignment screen",
      'if ("shortcuts".equals(page))' in open_selected and
      'startHomeShortcutAssignment(' in open_selected and
      'go("shortcutNavigation")' in open_selected)
check("Navigation submenu uses tested key slot mapping",
      'NativeHomeShortcuts.navigationSlot(sel)' in open_selected)
check("Assignment picker lists production model action names",
      'case "shortcutAssign":' in items and 'NativeHomeShortcuts.ACTION_NAMES' in items)
check("Assignment picker validates model and persists state",
      'homeShortcuts.assign(shortcutAssignmentSlot, sel)' in open_selected and
      "saveHomeShortcuts();" in open_selected)
check("Invalid Menu-removal assignment is rejected with user notice",
      'Keep Menu on at least one home key' in open_selected)
check("Home keys are restored from real Android SharedPreferences",
      'homeShortcuts.restore(' in load and
      'prefs.getString("home_shortcuts_v1"' in load)
check("Model serialization is written to exact live SharedPreferences key",
      'putString("home_shortcuts_v1", homeShortcuts.serialize())' in save and
      ".apply()" in save)
check("Home soft labels derive from actual user assignments",
      'homeShortcuts.label(NativeHomeShortcuts.LSK)' in softbar and
      'homeShortcuts.label(NativeHomeShortcuts.RSK)' in softbar and
      'homeShortcuts.label(NativeHomeShortcuts.CENTRE)' in softbar)
for key in ("OK","LSK","RSK","UP","DOWN","LEFT","RIGHT"):
    check("Home " + key + " dispatches the actual saved binding",
          '"'+key+'".equals(key)' in press and
          "homeShortcuts.assigned(NativeHomeShortcuts." + {
              "OK":"CENTRE","LSK":"LSK","RSK":"RSK",
              "UP":"UP","DOWN":"DOWN","LEFT":"LEFT","RIGHT":"RIGHT"
          }[key] + ")" in press)
check("Camera and Video home shortcuts launch existing native capture",
      'case NativeHomeShortcuts.CAMERA: startCameraCapture(false)' in dispatch and
      'case NativeHomeShortcuts.VIDEO_RECORDER: startCameraCapture(true)' in dispatch)
check("Default None action performs no simulated external action",
      'case NativeHomeShortcuts.NONE: break;' in dispatch)
check("Restore defaults must be confirmed",
      'requestConfirm("resetHomeShortcuts"' in option_choose and
      '"resetHomeShortcuts".equals(action)' in confirm and
      'homeShortcuts.restoreDefaults();' in confirm and
      "saveHomeShortcuts();" in confirm)
check("Assign and Reset are real Settings Options",
      'optionItems = new String[]{"Assign","Restore defaults","Help"}' in option_list)
check("Model blocks unassigning final Menu key",
      "if (!menuReachable())" in model and
      "bindings[slot] = old" in model)

print("Nokia My shortcuts: all production wiring checks passed")
