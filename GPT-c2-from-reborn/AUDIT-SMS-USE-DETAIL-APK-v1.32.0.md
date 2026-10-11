# Native Nokia C2-01 — "Use detail" SMS audit
Date: 11 October 2026. APK version v1.32.0; pinned upstream HTML v6.343.

## Verified gap in v1.31.0
The Inbox and opened-message Options both displayed "Use detail", but their
handler fell through to the "Not implemented" notice. The native Conversation
Options lacked the action entirely.

## Implementation
- Add read-only NativeSmsDetails extractor for phone numbers, e-mail
  addresses, and HTTP(S) / www. links. Retain sender phone number if usable.
- Ignore short verification codes, ISO-style dates, and phone-looking digits
  overlapping identified URLs or email addresses. Deduplicate repeated
  entries. Limit each SMS to 25 entries and reject unreasonably long links.
- Add a native list screen "Use detail". The highlighted row maps to its
  own detail, rather than another previously opened SMS or message.
- Phone Options: Details, Call, Send message, Save a contact, Copy.
- E-mail Options: Details, Compose e-mail, Copy.
- Web Options: Details, Open link, Copy.
- Centre OK shows details instead of calling, sending, or opening links.
- Explicit selection opens Android email/browser handler, or prepared dial /
  SMS / Contact editor. No automatic external action merely from extraction.
- The original Android SMS database stays unchanged by these operations.
- Android READ_SMS revocation removes the temporary extracted detail list.
- Sent and received message details and conversation rows are supported.
- Enforce native versionCode 13200 and versionName 1.32.0.

## Parser CI tests
The GitHub Action runs standalone Java regression tests before Gradle:
1. Sender international number and same formatted number in text deduplicate.
2. Phone-like digits inside a web URL are not extracted separately.
3. Email/URL are found and punctuation is trimmed.
4. A six-digit OTP and date 2026-10-11 are not interpreted as phone numbers.
5. Two distinct phone numbers are still extracted.
6. Empty/null SMS source behaves safely.

## Remaining tests for actual Android device
1. Inbox > Options > Use detail on SMS containing phone, email, and URL:
   each appears in the native picker and shows its correct type.
2. Conversation > Options > Use detail on a selected row extracts that SMS,
   not an unrelated or last-opened message.
3. SMS composed to an extracted number is not sent before pressing Send.
4. Save a contact opens a prefilled editor but does not silently write
   Android contacts.
5. An explicit mailto: action launches the installed email composer; web
   action opens a browser; missing handlers fail gracefully.
6. Copy detail puts the exact full value on Android Clipboard.
7. No usable phone/email/URL gives "No usable details in SMS".
8. Revoke READ_SMS while on detail screen; no previously read SMS remains.
9. Compare actual Nokia 240x320 screen, text icons, Options ordering/scroll,
   and softkey behaviour against upstream HTML v6.343.

Static source inspection and CI compilation do NOT constitute a device
verification or claim of pixel-perfect or complete HTML feature parity.
