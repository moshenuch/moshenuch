GPT c2 from reborn — APK v1.24.1 / HTML reference v6.343

Native Android Java/Canvas application; NOT a WebView.

New in v1.24.0:
- Add working Mark as read / Mark as unread to received SMS in Inbox,
  opened messages, and within individual conversations.
- Allow marking a selected conversation (or already marked conversations)
  as read or unread. Only incoming SMS in those conversations are affected.
- Store read/unread as durable app-local overrides, supporting Mark as unread
  even if Android's SMS provider already reports a message as read.
- Align incoming envelope bitmaps, red thread dot and status envelope with the
  new app-local state. Opening a received SMS marks it read in app.
- Sent SMS cannot be marked unread; system SMS records are never modified.

Retains all v1.23.0 speed-dial, Contact Groups, messaging, media and Gallery work.
Version 6.343 identifies the published upstream HTML, not full parity.

LIMITATIONS: Device screenshot parity, default SMS integration and physical
hardware key behaviour are not verified. GitHub CI checks compilation and
APK packaging, not real device user interaction.

Release metadata repair in v1.24.1:
- The GitHub build reads a pinned HTML-REFERENCE.txt instead of an outdated
  hardcoded v6.342, so release asset names identify the correct upstream.
- Both read and unread are always available in Conversations Options, including
  when selected threads contain mixed read states. All actions remain app-local.
