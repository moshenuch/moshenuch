# Native icon and SMS parity audit — HTML v6.342

Source: `mbaiforinsticnt-dev/c2-reborn/index.html`, blob `8966ff284125bd6fa752a78f6bc4a51fd47a03a4`, version **6.342** (published 2026-10-07). Native baseline: APK 1.1.0; corrective build: **APK 1.2.0**.

## Observed source behavior (code inspection, not emulator confirmation)

| Context | Source behavior | v1.2.0 native action |
|---|---|---|
| Unread incoming SMS | Uses `INBOX_ICON_UNREAD` bitmap, closed yellow envelope | Use source PNG `SMS_UNREAD` |
| Read incoming SMS | Uses `VIEW_ICON_READ`, different open/read bitmap | Use source PNG `SMS_READ` |
| Sent SMS | Uses `VIEW_ICON_SENT`, separate sent icon | Use source PNG `SMS_SENT` rather than treating all non-unread as received/read |
| Conversation rows | `convIconHtml` selects `CONV_ICON_UNREAD`, `CONV_ICON_READ` or `CONV_ICON_SENT`, based on any unread and newest message direction | Use source PNG `THREAD_*` variants |
| Open a conversation | `openConversationRow` navigates without clearing unread status | Native already follows this rule |
| Open an individual message | `markDemoRead` changes incoming `unread` to false; same for Inbox individual read screen | Native retains local read overlay (Android SMS write limitations) |
| Status bar | `drawStatus` shows `.statusMsg` if any thread/Inbox messages are unread | Use exact HTML status PNG when local phone SMS has unread messages |
| Red conversation dot | Source CSS has `.conversationRow .unreadDot{display:none!important}` although JSX still emits it | **Intentional outstanding difference:** native retains dot due to user's earlier explicit request. Verify before removing |
| Message bubble layout | Source uses `.thread`, `.bubble`, selected white and unselected navy, message timestamp, specific truncation/scroll/softkey behavior | **Outstanding:** native still displays simpler list rows, not exact bubble layout |

## v6.342 HTML list-row icon mapping

The source `rows()` implementation maps page/index to `listIcons` entries. The build now uses the same IDs wherever these native screens are implemented:

- Messaging 14: `0570 9001 0724 9002 0603 0602 0723 0415 0582 9003 0584 0598 0572 0604`
- Organiser 8 present in APK: `0557 0715 0661 0703 0528 0558 0569 0580`; HTML also has Dictionary `DICT`
- Media 6: `GT23 GT54 GT03 0596 GT57 GTeq` (replaces handmade recorder/equaliser graphics)
- Settings first 15: `0595 0608 0513 0628 0574 0571 0692 0915 0568 0560 Iphone Iacc Icfg Isec Irst`
- Contacts first 8: `0593 0922 0626 0599 0562 0605 0585 0572`
- Gallery first 4 shown in native app: `0534 0619 0625 0620`
- Log first 13: `0719 0565 0566 0563 0603 0572 0564 0921 0579 0580 0601 0627 0916`
- Applications initial rows from the available firmware pool.

79 bitmap resources were copied from the exact HTML blob, including SMS/thread variants and status glyphs. Native retains its own Android data integrations: source HTML uses seeded demo/localStorage SMS rather than live device SMS.

## Explicit limitations / follow-up

1. Screen counts, ordering and all Options actions must be compared screen-by-screen. Current Android screens often have fewer or differently grouped rows than the HTML.
2. Reconstruct real HTML message bubble geometry, selection, timestamps, scroll and softkeys. Bitmap accuracy alone does not imply screen parity.
3. Find source bitmap references for Gallery theme/graphics/tones/recordings/received files entries. The icon maps reference `GALth`, `GALgr`, `GALto`, `GALre`, `GALrc` but these were not extracted in this pass.
4. Audit each remaining dynamically rendered icon, softkey icon, status icon and modal icon, including themes and accessibility/pressed/disabled states.
5. Real handset/emulator screenshot-by-screenshot parity is **not verified**. The user-provided conversation screenshot was used as a visual cue and code paths were checked; this pass is an implementation step, not a full device-certification.

## APK v1.3.0 — native conversation parity improvement

- Replaced fixed-height conversation list rows with variable-height HTML-inspired SMS bubble cards.
- Sent bubble uses SMS_SENT, incoming unread uses SMS_UNREAD, incoming locally opened uses SMS_READ; bitmap selection is stateful on every draw.
- Dynamic three-line previews with ellipses, message timestamps, navy/default and white/selected surfaces, bubble tails, selected-item scrolling, and a 1/N header count.
- Restored HTML center action distinction: incoming short/nontruncated message Reply; sent or truncated message Open. No automatic read-on-conversation-entry.
- Follow-up: device screenshots need comparison for pixel geometry/fonts and same-day versus historical dates; no full equivalence claim.

## APK v1.4.0 — Gallery icon pass

Source `screens.gallery` contains nine rows: Memory card, Images, Video clips,
Music files, Themes, Graphics, Tones, Recordings, Received files. Prior APKs showed
only the first four. The native Gallery now has all nine entries using the matching
bitmap keys: `MEMCARD 0619 0625 0620 GALth GALgr GALto GALre GALrc`.
Six additional gallery bitmap assets were extracted from the HTML source, not drawn
as substitutes. Opening the additional entries is still placeholder functionality.

## APK v1.5.0 — top-level row order/icon alignment

From actual HTML `screens` arrays and `maps` ID lists:

- Settings: source has 15 items including **Lights** at index 3, **Phone** at 10, Accessories, Configuration, Security and factory reset. Previously native had 10, so the exact bitmap keys mapped to the wrong labels after index 2. Restored exact source ordering with 15 rows, fixed routing indices, moved native About to Options.
- Contacts: source 10, previously native 8. Added Move contacts and Copy contacts using exact PNG IDs `9007` and `9006`.
- Log: source 14, previously native 8. Added omitted log list names; last `Simulate missed call` lacks a distinct list-ID mapping in original `maps.log`, so the native list uses the existing Missed calls bitmap until precisely characterized.
- Organiser: source 9, previously native 8. Added Dictionary using existing `DICT` source icon, renamed Countd. timer exactly.
- Still unverified: nested screens, Options submenus, screen-specific icon variations, operator themes, tactile UX and final dimensions.
