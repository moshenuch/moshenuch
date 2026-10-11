GPT c2 from reborn — APK v1.26.0 / HTML reference v6.343

Native Android Java/Canvas application. NOT a WebView.

New in APK v1.26.0 — multi-number contact handling
- Collect all phone-number rows for each Android aggregated contact while
  retaining one entry per contact in the Nokia Names list.
- Recognise a contact's secondary phone numbers in SMS, conversation,
  call-history and other contextual "Contact" Options.
- Contact details show the primary number and up to two additional numbers,
  with an overflow count if more are stored.
- Green Call and contact editing still use the primary phone-number row.
- Clear cached Android contacts and aliases when READ_CONTACTS is revoked,
  and handle empty Contact details and Call actions without crashing.
- Keep all contact writes behind the existing editable Save action.

Retained:
- v1.25.0 contextual Contact / Save a contact and in-call Send message.
- v1.24.1 local SMS read/unread and conversation Mark controls.
- v1.23.0 on-screen long-press speed dials; earlier Gallery/media support.

Known differences and verification:
- Extra contact numbers are displayed and recognised but are not yet
  individually selectable for outgoing calls or per-number editing.
- Locale-aware phone-number canonicalisation may not resolve all formats.
- No physical-handset contact test or pixel-exact HTML/native screenshot
  comparison has been performed. GitHub Actions verifies compilation and APK
  packaging only.
