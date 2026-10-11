GPT c2 from reborn — APK v1.30.0 / HTML reference v6.343

Native Android Java/Canvas application — NOT an HTML WebView.

v1.30.0: Working SMS "Copy as template" and Saved items
- Received messages, outgoing message detail screens and individual SMS
  conversation rows can copy an actual message body as a text template.
- The Messaging > Saved items folder now lists only templates explicitly
  copied by the user. App-local templates persist over APK restarts.
- Selecting a saved template opens the native SMS composer with the text
  prefilled but the recipient empty. Sending requires explicit user action.
- Saved items Options support Use template, Details, Delete template,
  Delete all templates, New message and Memory status.
- Delete template / Delete all templates require Nokia-style Yes/No
  confirmation. These operations only change app-private saved text,
  never delete, move or change system SMS records.
- Saved items memory/folder details display the real number of templates.
- Maximum 100 copies to avoid unbounded app-local storage.
- Inbox opening and Inbox Options now reject missing SMS permissions or
  absent selections instead of showing an obsolete previously-opened SMS.

Previous native features retained:
- v1.29.0 working Contacts Search (Nokia multi-tap and number search).
- v1.28.0 exact source Contact PNGs, v1.27.0 multi-number selection.
- v1.24.x unread message and per-conversation app-local read states.
- Native SMS, Groups, speed dial, Gallery/Camera, Organiser and audio.

Limitations:
- Saved items here is a collection of copied SMS text templates, not a
  complete emulation of S40's system-message storage hierarchy.
- The "Move" SMS action still does not move Android provider records.
- The new folder's visual layout and full Options catalogue have not been
  compared pixel-for-pixel against source HTML v6.343.
- CI compiling and publishing an APK is not a physical-device test.
- Signatures still depend on GitHub's ephemeral debug key until the
  separate stable-signing PR #2 is activated; a fresh uninstall may be
  necessary before installing the latest APK.
