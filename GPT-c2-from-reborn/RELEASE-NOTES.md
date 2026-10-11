GPT c2 from reborn — APK v1.32.0 / HTML reference v6.343

Native Android Java/Canvas application — NOT an HTML WebView.

v1.32.0: Working SMS "Use detail" Options
- The Nokia "Use detail" action is now executable from highlighted Inbox SMS,
  opened received/sent SMS, and highlighted messages in a conversation.
- A native, read-only parser extracts usable phone numbers, email addresses,
  and HTTP/HTTPS or www. web links from the source SMS body. The sender's
  telephone number can also appear as a detected detail.
- A "Use detail" screen displays detected entries and context-specific Options.
  Phone: Details, Call, Send message, Save a contact, Copy.
  E-mail: Details, Compose e-mail in installed Android app, Copy.
  Web: Details, Open link using Android browser, Copy.
- SMS compose opens with recipient pre-filled but sends nothing until the
  user explicitly sends. Save a contact opens the editable Contact editor
  without committing a change. External browser/mail actions require explicit
  user selection, never run merely from parsing.
- Copies use Android clipboard. The phone's original SMS database is never
  edited, moved, marked read or deleted by "Use detail".
- Parser deduplicates numbers, ignores six-digit verification codes, ignores
  ISO-style date text, and excludes phone-looking digits inside URLs or emails.
- Once Android READ_SMS access is revoked, any temporary extracted details
  are discarded alongside cached phone SMS, without erasing app-owned drafts.
- CI now runs standalone Java parser regression tests before building the APK.

Earlier native functionality retained:
- v1.31.0 live, foreground-only Nokia unread SMS icon refresh.
- v1.30.0 persistent SMS templates and Saved items.
- v1.29.0 multi-tap Contacts Search; v1.28.0 exact contact bitmap sprites;
  v1.27.0 multiple selectable contact phone numbers.

Verification and limitations:
- HTML v6.343 is the pinned reference; this implements the actions rather
  than claiming exact Options menu ordering, typography or screen pixel parity.
- Phone-number extraction is best-effort and may miss uncommon formats.
- Browser and email actions require an installed Android handler; navigation
  outside the Nokia app is explicitly requested by a user action.
- CI builds and tests parsing; actual Android telephony, SMS data, external
  app actions and device screenshot behaviour still require handset tests.
- The separate stable-signing PR #2 remains unconfigured, so installing over
  an earlier APK may require uninstalling the old package first.
