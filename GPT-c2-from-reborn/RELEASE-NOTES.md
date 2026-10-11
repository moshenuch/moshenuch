GPT c2 from reborn — APK v1.31.0 / HTML reference v6.343

Native Android Java/Canvas; NOT a WebView wrapper.

v1.31.0 — live unread-SMS icon and conversation refresh
- Observe the Android SMS content provider while the C2 app is visible.
  Changes made by a received SMS or another SMS app now trigger a debounced
  refresh of Inbox, Conversations and the open conversation's message rows.
- Nokia SMS open/closed-envelope icons, unread-conversation red dots and the
  simulated Nokia status-bar envelope use that refreshed message snapshot.
- Repeated SMS provider updates are coalesced into one refresh after 300 ms.
  Observation stops when the app is backgrounded, and starts on Resume.
- SMS observer is also registered after SMS permissions are granted.
- If READ_SMS permission is revoked, discard cached system SMS, inbox entries,
  conversation rows and marked SMS selection immediately. Saved app-owned
  templates and drafts remain untouched. No system SMS data is changed.
- If the Android provider refuses observation, app stays usable and refreshes
  its SMS data when foregrounded, as it did before.

Retained previous functionality:
- v1.30.0 Saved items and copied SMS templates, including Yes/No deletion.
- v1.29.0 native searchable Contacts by multi-tap or phone number.
- v1.28.0 exact Nokia contact icons, v1.27.0 multi-number contact actions.
- v1.24.x local SMS read/unread overrides, envelope and red-dot rules.

Limitations:
- This is *in-app* live updating only. It is not a new Android system
  notification or background SMS monitoring service.
- A recent SMS must appear in the Android provider database before a
  provider-change notification can update the Nokia display.
- Existing app-local Mark as unread overrides intentionally take precedence
  over Android's system read flag until changed inside this app.
- CI compilation and APK publishing do not prove actual physical-device SMS
  broadcasts, database observation or icon-level screenshot parity.
- APK debug signing is still ephemeral until signing PR #2 is configured;
  uninstall old APK first if Android reports a signing conflict.
