# Pixel 7 / Android 13 — audit data walkthrough (native APK v1.35.0)

## Turn on fictional demo records (no Android provider changes)

1. Install the **debug** APK v1.35.0. If Android refuses the upgrade because
   of the GitHub debug signing certificate, uninstall the older C2 APK first.
2. Inside the Nokia display open **Menu > Settings**.
3. Scroll to the **last item**, **Demo data (debug)**.
4. Choose **Seed / reset sample data**.
5. The Nokia display shows **DEMO**, then enters **Conversations** automatically.

This works with **no phone Contacts/SMS records and no READ_CONTACTS/READ_SMS
permissions**. Fixtures are isolated from actual Android content providers.
They survive an app restart while demo mode is on. Seed again to reset them.

## Dataset expectations

| Dataset | What to check |
| --- | --- |
| 12 Contacts, Aaron West through Zoe Clarke | Scrolling, Nokia alphabetical list, multi-tap name search |
| Aaron West: two numbers, +44 7700 900101/102 | Up/Down selects primary and secondary; SMS to 102 resolves Aaron |
| Leah Hart: two numbers, +44 7700 900110/111 | Number search on a secondary number |
| 12 SMS total across six threads | Conversation ordering, entry selection, message view |
| 3 unread incoming SMS | Closed envelope, red conversation dot, status-bar unread icon |
| Read incoming and sent SMS | Open and sent envelope variants |
| Unknown +447700900799 | "Save a contact" option, unknown-sender handling |
| "NOKIA" alphanumeric sender | Non-number SMS sender handling |

## SMS Use detail exercise

1. In **Conversations**, open **Aaron West** (the newest thread).
2. Highlight the newest inbound **DEMO** message.
3. Choose **Options > Use detail**.
4. Confirm the picker offers:
   - **Phone** numbers, including +44 7700 900122
   - **E-mail** audit@example.org
   - **Web** https://example.org/audit/12345678
5. Confirm it does *not* treat the six-digit code **123456** or the
   date **2026-10-11** as phone numbers.
6. Test **Details**, **Copy**, **Call**, **Send message**, **Save a contact**.
   Real calls, SMS transmissions, and Contacts writes are deliberately BLOCKED
   in demo mode. Send message opens an editable composer, but Send reports
   the demo restriction instead of transmitting anything.

## Conversation and read/unread exercise

1. Note red dots and the unopened envelope beside Aaron West, Leah Hart
   and the unknown number.
2. Open Aaron's latest incoming SMS. It should be read within this APK
   after opening, and its red dot should clear if no other unread messages
   remain for that thread.
3. Open **Options > Mark as unread** on that received SMS. Confirm the
   conversation red dot and status envelope return.
4. Navigate between Aaron's received and sent items; verify envelope states.
5. Use Mark, Mark all, Unmark all and the app-only Delete confirmation.
6. Exit and reopen the APK. The intentional demo read/unread state should
   persist while the fiction fixture remains selected.
7. Repeat **Seed / reset sample data** to restore all 12 fictional SMS,
   original three unread states and all six threads.

## Contact sorting and secondary-number exercise

Open **Menu > Contacts > Names**. Browse all 12 names (Aaron, Abigail,
Benjamin, Chaya, David, Ella, Fiona, Hannah, Leah, Noah, Oliver, Zoe).
Open Aaron West, select either phone number with Up/Down. Test Options
and green Call: demo mode shows a safety notice instead of starting a call.
Use **Names > Options > Search** and use keypad multi-tap. Toggle **#** to
search by digits, including Aaron's 102 and Leah's 111 secondary numbers.

## Turn off all demo fixtures

1. **Menu > Settings > Demo data (debug)**.
2. Choose **Clear demo data**.
3. Confirm **Yes**.
4. The app returns to **Settings**; the **DEMO** label disappears.
5. Your genuine Android Contacts and SMS, if permission was granted, are
   displayed on the next visit. If the emulator has none, messaging is empty
   or prompts for permission as before.

Clearing demo fixtures also removes their app-local hidden and read/unread
overrides; it does NOT delete Android contacts, Android messages, or your
pre-existing app-saved drafts/notes/templates.

## Reporting an audit regression

For each issue, record **APK version**, **emulator and Android version**,
**screen path**, **selected row/Options action**, **expected vs actual
behaviour**, and a **screenshot**. Indicate whether **DEMO** was visible.
