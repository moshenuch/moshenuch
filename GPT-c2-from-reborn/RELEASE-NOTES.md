GPT c2 from reborn — APK v1.25.0 / HTML reference v6.343

Native Android Java/Canvas, NOT WebView.

New in v1.25.0 — contextual phone-number Options
- Inbox, opened SMS, selected conversation message, Conversations, Sent items,
  call history lists, active call, Group members and Speed dials now offer
  "Contact" for a securely matched saved number or "Save a contact" if unknown,
  where no pre-existing Save/Save to contacts action takes precedence.
- "Contact" navigates to the existing native contact details screen.
- "Save a contact" opens the editable native new-contact form prefilled with
  the actual selected number. No contact data changes until Save is pressed.
- Active call Options now include Send message, pre-addressed to the call
  number. This is an editing action, not automatic SMS sending.
- Contact display lookup no longer uses a six/eight-digit suffix that could
  falsely link distinct numbers. The comparison now uses full E.164-format
  normalization where possible, with strict full-number fallback.
- Unimplemented dial-screen Add to contact now clearly reports its limitation
  instead of falsely implying completion.
- A number is always taken from the actual provider-backed message/call
  record or the native dial state, never from the displayed contact label.

Retains v1.24.1 app-local SMS read/unread states, v1.23.0 long-press
speed dial, Contacts Groups and other implemented native features.

Known differences:
- Contextual number menus reproduce the functional v6.343 rule where current
  native pages exist, not the HTML's entire per-screen Options catalogue.
- Native Contacts currently displays one phone number per aggregated contact;
  its underlying device-provider contacts can contain multiple numbers.
- Phone-number normalization depends on Android's device locale and input.
- No automated emulator screenshot parity, physical device call/SMS tests, or
  full dynamic menu/visual verification has been performed.
