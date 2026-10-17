#!/usr/bin/env python3
"""CI guard: Add to contact must add a number to an existing Android contact.

Statically verifies the production wiring from the dialer and Conversations
Options through the Nokia contact picker to the ContactsContract insert.
Physical provider writes still require emulator/handset testing.
"""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
view = (root / "app/src/main/java/com/moshenuch/c2reborn/C2PhoneView.java").read_text(
    encoding="utf-8"
)
backend = (root / "app/src/main/java/com/moshenuch/c2reborn/AndroidBackend.java").read_text(
    encoding="utf-8"
)
ukphone = (root / "app/src/main/java/com/moshenuch/c2reborn/UkPhoneNumbers.java").read_text(
    encoding="utf-8"
)

def method(src: str, signature: str) -> str:
    i = src.find(signature)
    if i < 0:
        raise AssertionError("Missing production method: " + signature)
    brace = src.find("{", i + len(signature))
    if brace < 0:
        raise AssertionError("Missing body: " + signature)
    depth = 0
    for n in range(brace, len(src)):
        if src[n] == "{":
            depth += 1
        elif src[n] == "}":
            depth -= 1
            if depth == 0:
                return src[brace:n + 1]
    raise AssertionError("Unterminated method " + signature)

def check(label: str, ok: bool) -> None:
    if not ok:
        raise AssertionError("FAIL: " + label)
    print("PASS: " + label)

add = method(backend, "public boolean addNumberToContact(long rawContactId, String number)")
check("Backend inserts into the selected raw contact only",
      "ContactsContract.Data.RAW_CONTACT_ID, rawContactId" in add)
check("Backend writes a real phone-number data row",
      "ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE" in add
      and "ContactsContract.CommonDataKinds.Phone.NUMBER" in add)
check("Backend refuses without WRITE_CONTACTS or a usable target",
      "Manifest.permission.WRITE_CONTACTS" in add and "rawContactId <= 0" in add)

begin = method(view, "private void beginAddToContact(String number)")
check("Entry refreshes contacts and requires an existing contact",
      "refreshContactsFromPhone()" in begin and "contacts.isEmpty()" in begin)
check("Entry opens the dedicated contact picker",
      'go("addToContactPicker")' in begin)

open_selected = method(view, "private void openSelected()")
check("Picker selection delegates to the pending-number insert",
      'if ("addToContactPicker".equals(page))' in open_selected
      and "addPendingNumberToContact(contacts.get(sel))" in open_selected
      and '"No contact selected"' in open_selected)

add_pending = method(view, "private void addPendingNumberToContact(Contact target)")
check("Picker adds the pending number through the backend and refreshes",
      "backend.addNumberToContact(writeRawId," in add_pending
      and "refreshContactsFromPhone()" in add_pending)
check("Picker writes to a writable raw contact of the aggregate, never its first raw id",
      "writableRawContactId(target.contactId)" in add_pending
      and "RAW_CONTACT_IS_READ_ONLY" in backend
      and "RawContacts.DELETED" in backend
      and '"ADD_TO_CONTACT_REFUSED reason=read_only_contact"' in add_pending
      and "addNumberToContact(target.rawContactId" not in add_pending)
check("Picker never writes in demo mode or to app-local contacts",
      '"DEMO: Contacts are read-only"' in add_pending and "!target.device" in add_pending)
check("Picker refuses numbers already saved on the chosen contact",
      '"Number already saved for "' in add_pending
      and "pendingDigits.equals(ukNormalisedDigits(savedNumber))" in add_pending)
check("Duplicate check treats UK trunk 0 and +44/0044/44 forms as the same number",
      'digits.length() == 14 && digits.startsWith("0044")' in ukphone
      and 'digits.length() == 12 && digits.startsWith("44")' in ukphone
      and "UkPhoneNumbers.ukNormalisedDigits(number)" in view
      and "ukNormalisedDigits(pendingAddToContactNumber)" in add_pending)
check("Probe markers: picker open, refusal reasons and insert are logged",
      '"ADD_TO_CONTACT_PICKER_OPEN"' in view
      and '"ADD_TO_CONTACT_REFUSED reason=duplicate"' in add_pending
      and '"ADD_TO_CONTACT_REFUSED reason=no_write_permission"' in add_pending
      and '"ADD_TO_CONTACT_REFUSED reason=invalid_sender"' in view
      and '"ADD_TO_CONTACT_INSERTED rawContactId="' in add_pending)

main = (root / "app/src/main/java/com/moshenuch/c2reborn/MainActivity.java").read_text(
    encoding="utf-8"
)
check("Deliberate selection without WRITE_CONTACTS asks with the OS sheet",
      "askWriteContactsForAdd(target.contactId)" in add_pending)
check("MainActivity wires the write ask: one request code, pending target, retry or refuse",
      "REQ_CONTACTS_WRITE" in main
      and "pendingWriteAddContactId" in main
      and "retryAddToContactSelection(contactId)" in main
      and "onWriteContactsDeniedForAdd()" in main)
retry = method(view, "void retryAddToContactSelection(long contactId)")
check("Grant retry re-runs the insert on the remembered contact",
      "addPendingNumberToContact(target)" in retry
      and '"No contact selected"' in retry)
denied = method(view, "void onWriteContactsDeniedForAdd()")
check("Deny keeps the no_write_permission refusal and notice",
      '"ADD_TO_CONTACT_REFUSED reason=no_write_permission"' in denied
      and '"Contacts write permission unavailable"' in denied)
check("Entry rejects senders without a usable phone number",
      "digitsOnly(number).length() < 3" in begin
      and '"This sender cannot be saved as a phone number"' in begin)
check("Conversations options list offers Add to contact (menu item reachable)",
      '"Call","Add to contact","Conversation details"' in view)

items = method(view, "private String[] itemsFor(String pg)")
check("Picker page lists current contacts",
      'case "addToContactPicker":' in items)

choose = method(view, "private void chooseOption()")
check("Dialer Add to contact routes the typed number",
      "beginAddToContact(dial)" in choose)
check("Conversations Add to contact routes the sender address",
      "beginAddToContact(smsThreads.get(sel).address)" in choose)
check("Old not-implemented stub is gone",
      "Adding a number to an existing contact is not implemented" not in view)

options = method(view, "private void openOptionsForPage()")
check("Picker offers the Select option",
      '"addToContactPicker".equals(page)' in options)

print("Add-to-contact wiring audit complete")
