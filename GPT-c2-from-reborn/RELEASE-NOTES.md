GPT c2 from reborn — APK v1.10.0 / HTML reference v6.342

Native Android Java/Canvas, not a WebView. This remains a partial native port.

Audited and implemented in SMS:
- Forward from Inbox and message-detail Options opens composer containing the actual
  full selected SMS text, never generic placeholder text; recipient is left blank.
- Edit on a received message opens an editable copy, rather than modifying the
  Android provider record. "New message" now creates an empty composer.
- Message details now shows correct direction, sender/recipient number, actual
  timestamp, length and app-local unread/read or outgoing status. It no longer
  claims every message has "Status: Read".
- The Message Read screen displays the Nokia envelope matching sent/read/unread
  state, the message timestamp, and a correct center action: Reply on incoming,
  Forward on outgoing. Message options vary by direction.
- Conversation > Options provides working Forward, Message details and Conversation
  details. Conversation-list details report actual counts (received/outgoing/
  unread) and most recent activity rather than fixed placeholder information.
- Conversation view from message Options opens the right specific thread,
  instead of taking the user to the general conversation list.
- Unsupported SMS options now report "Not implemented" rather than claiming
  a feature ran.

Retained: Nokia source firmware checkbox/radio sprites (v1.9.0), structured
drafts, read/unread/sent envelope icons, mark actions and Android integration.

Pending: true carrier delivery confirmation, system SMS read/delete role,
source-exact nested options and typography, all device screenshot verification.
HTML reference 6.342 is not a full-parity certification.
