# Native Nokia C2-01 audit — Contact number selection
Date: 11 October 2026
APK target: v1.27.0
Upstream HTML version referenced: v6.343.

## Confirmed defect in previous APK v1.26.0
Android contact loader saved multiple numbers and displayed the first three
on the custom contact detail view. However CALL and OK always dialled
the primary number. The Options action handlers also hardcoded ct.number for
Call, Send message, Use number, Copy number, Speed dial, Add to group and
black/white-list actions. Opening an existing contact via an SMS to a secondary
number always showed the primary number.

## Changes made
- currentContactNumbers(): ordered deduplicated primary + secondary numbers
  from the Android contact's aggregated ID. Strict full-number normalisation
  prevents duplicate strings with equivalent international formatting.
- Contact details has selectable, highlighted rows; shows up to four visible
  numbers in 240x320 Nokia display with Up/Down wrapping selection.
- Green Call/OK and Contact Options now resolve selectedContactNumber().
- Open from SMS/call "Contact" selects the matching number in that contact,
  not just the first entry.
- Text display is ellipsized to LCD width for long numeric strings; full
  underlying phone number is still used for calls and sending.
- Editing and deleting continue to operate on the whole contact or primary
  number; no accidental mutation of secondary provider rows.
- No external phone action is initiated merely by changing row selection.

## Code-level regression checks completed before build
- Explicit row selection only handled on contactDetail page.
- All contactDetail option actions that act on a number use selected number.
- Pressing physical/simulated Nokia CALL and centre OK use the same selection.
- Contact navigation from a matching SMS/call selects the corresponding alias.
- Existing contact storage, multi-number loader, external SMS database and
  old debug signing config remain unchanged.

## Outstanding real-device test matrix
1. Contact with three different numbers: Up/Down highlight each in turn;
   each Green Call initiates only its highlighted number.
2. Contact with five numbers: scroll and wrap to hidden rows, check correct
   final/first number and label "Number N of 5".
3. SMS received on secondary contact number -> Options > Contact;
   secondary number should be preselected.
4. Options > Send message, Copy number, Use number and Speed dial should
   use highlighted secondary number, not primary.
5. Options > Edit after selection: only primary edit should be opened;
   secondary Android data row must not be replaced.
6. Revoke READ_CONTACTS while in contact details and use CALL/Options:
   graceful notice, no crash.
7. Test long numbers, duplicates, different international formatting, local
   country/region ambiguity and null contact providers.
8. Compare icons, Nokia fonts, contact details geometry, Options order and
   real-handset behaviour against HTML v6.343.
9. Confirm update installation behaviour on an old debug-signed package:
   one-time uninstall is currently required.

## Parity caveat
This patch adds functional selection, not a claim that HTML's exact animation,
pixel geometry, editable number types or all contextual Options are identical.
GitHub Actions success verifies Java compilation and APK packaging, not phone
telephony interaction.
