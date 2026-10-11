# Deep audit continuation — SMS Options and detail screens
HTML reference **v6.342**; native APK **v1.10.0**.
Date: 11 October 2026.

## Defects resolved by native source changes
| Issue | Before | Corrected implementation |
|---|---|---|
| Forward content | Received and sent SMS Forward produced literal `Forwarded message` regardless of message body | `forwardSmsCopy(DeviceSms)` preserves full actual text and requests a new recipient |
| SMS details | Details hardcoded `Status: Read` | Details resolved from Android message type, `isSmsUnread`, original number/date/body length; outgoing delivery labelled unconfirmed |
| Sent message center | Always offered `Reply` | Incoming offers Reply, outgoing offers Forward, and both respond accordingly |
| SMS envelope on detail | Detail page had no source Nokia envelope | Detail uses `SMS_UNREAD`, `SMS_READ`, `SMS_SENT` bitmap state and actual timestamp |
| Conversation details | Options either displayed an uninformative toast or just a generic name | Actual thread summary counts incoming, outgoing and app-local unread SMS, showing participant and latest timestamp |
| Conversation view | Chose a general list, not the requested message's thread | Enters selected message's specific conversation, resets previous marked selection |
| Unsupported options | Displayed option label as a toast that could be mistaken for success | Explicit `Not implemented:` label where behavior remains unsupported |

## Deliberate limitations
- Opening incoming SMS reads it **in the app only**; cannot promise Android provider read state without system SMS role.
- Sent SMS stored in Android does not establish carrier-delivery success. Metadata explicitly marks outgoing delivery **unconfirmed**.
- SMS Move / Copy as template / Use detail remain unsupported, and some nested Options are still not implemented.
- Button glyphs/screens are not yet independently compared pixel-by-pixel against handset.
- The original source's icon catalogue is evidence of bitmap identification, not sufficient for a whole-device parity claim.

## Validation checklist for later instrumented device testing
1. Make two threads with sent/received messages and one unread incoming. On Inbox > Forward, confirm exact text appears and recipient starts empty.
2. On Sent items > message > Forward, confirm outgoing text appears, no accidental Reply-to-self.
3. Compare Message details direction/time/read state across four message types and local read transition.
4. Open Message details > Conversation view and confirm it selects only the corresponding thread.
5. From a conversation bubble, test Forward/Message details/Conversation details and page Back navigation.
6. Inspect exact icon draw positions and options typography on test Android device.

Build status: check GitHub Actions for `v1.10.0` compilation and release upload.
