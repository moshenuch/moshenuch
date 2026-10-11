# v1.24.0 — App-local SMS read/unread parity pass
Source: HTML v6.343, upstream commit 6fec42d4146cd01bc59c561ceac14e340fc467ed.

Previous native v1.23.0 only stored sms_local_read, so Android provider
records already marked read could not be marked unread in the emulator.
Inbox, Message read, Conversation, and Conversations options were missing
read/unread actions introduced by the newer upstream reference.

Implemented:
- Persist sms_local_unread alongside sms_local_read. Explicit unread overrides
  provider READ; explicit read removes unread override; both retain across runs.
- Context-sensitive Mark as read/unread for inbox and message view (received
  only), highlighted received SMS in a conversation, and entire selected or
  marked threads. Outgoing SMS are never treated as incoming unread.
- Icons, per-thread red dot, status envelope and counters use isSmsUnread().
- No SMS provider writes, and no claim of modifying the phone's SMS state.

Unperformed real-device regression cases:
1. Mark unread phone SMS read; check Inbox icon, thread red dot and status.
2. Mark read phone SMS unread; check both persistence and reopening behavior.
3. Mark conversations including mixed incoming/outgoing messages, ensure
   only received SMS change and unrelated threads remain unaffected.
4. Reopen the APK; Android Messages unread state must not be changed.
5. Empty Inbox, no READ_SMS permission, hidden SMS and sent-only thread.
6. Compare HTML v6.343 screen captures and all option menu geometry.

This is a bounded code-level improvement, not full screenshot parity.
