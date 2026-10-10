# Deep icon and SMS behavior audit — C2 Reborn HTML v6.342

**Scope:** source-code audit of HTML v6.342, not a claim of pixel-perfect device parity.  
**Source:** `mbaiforinsticnt-dev/c2-reborn/index.html` at git blob `8966ff284125bd6fa752a78f6bc4a51fd47a03a4` (3,744,446 bytes).  
**Native:** `GPT-c2-from-reborn`, Java/Canvas Android, APK v1.2.0.  
**User visual reference:** 10 October 2026 screenshot of Alex Morgan conversation.

## 1. Icon taxonomy

Do NOT treat the following as interchangeable:
- 9 main-menu icons, from HTML `assets[]`.
- Page-specific list rows, mapped in `rows()` via `maps[page]?.[i]` then `listIcons[id]`.
- SMS conversation folder icons, selected by `convIconHtml()`.
- Message bubble and Inbox icons, selected by `msgIconHtml()` and Inbox renderer.
- Status-bar glyphs (new message, signal, battery, Bluetooth).
- Marked checkboxes, selected/unselected radios, modal/picker icons.
- Dynamic visual states: active/inactive; focus/selection; read/unread; incoming/outgoing; disabled; marked; theme-specific.

The HTML's `rows()` has **47 named icon mapping groups** and **246 mapped icon slots** (this is slot-count, not distinct image-count). It separately names **70 pages** in `noIcon`, whose list rows intentionally omit icons. Rendering a generic image on these pages would be *less* faithful, not more. The embedded `listIcons` object contains at least **246 identifiable data-URL PNG assignments** (some additional icons are assigned dynamically outside its initial literal). See the original `ICON-CATALOGUE.md` for firmware extraction evidence and caveats.

## 2. Exact SMS state machine and icon selection

| Surface/state | Observed HTML logic | Expected native behavior |
|---|---|---|
| Inbox incoming unread | `x.unread ? INBOX_ICON_UNREAD : VIEW_ICON_READ` | Source closed-envelope bitmap |
| Inbox incoming read | `VIEW_ICON_READ` | Source read/open bitmap |
| Conversation bubble incoming unread | `msgIconHtml('unread')` | Same unread source sprite |
| Conversation bubble incoming read | `msgIconHtml('read')` | Read source sprite |
| Conversation bubble outgoing | `msgIconHtml('sent')` | Sent-specific sprite regardless of unread read flags |
| Conversation list with unread message(s) | `convIconHtml('unread')` | Conversation-level unread sprite; subtitle shows unread count |
| Conversation list read, latest incoming | `convIconHtml('read')` | Read conversation sprite |
| Conversation list read, latest outgoing | `convIconHtml('sent')` | Sent conversation sprite |
| Status | `drawStatus()`: statusMsg visible if any thread or Inbox item unread | New-mail indicator while unread remains |
| Enter conversation | `openConversationRow()`: navigation does not clear unread | Opening thread alone must not mark all read |
| Open individual SMS | `markDemoRead(m)` in `msgopen` | Only that incoming message becomes read, persisted |
| Open individual Inbox SMS | `messageread` flips Inbox `unread` false | Same transition for Inbox items |
| Options: Mark as read | Thread-level option clears each `unread` | Must operate only on appropriate received messages |
| Options: Mark as unread | Thread-level option marks first received message unread | Never mark a sent message incoming/unread |
| Persist failure | `markDemoRead` restores previous values | No silent state loss; show failure |

**Crucial distinction:** individual-bubble sprites and conversation-folder sprites are different PNGs. A single drawn-envelope function cannot match both.

The HTML marks the opening bubble as selected (light background); other bubbles are dark navy. CSS caps message preview to three lines. Incoming tails point left and outgoing tails right; a timestamp appears in the upper-right corner; a separate `galCheck` indicates marked messages. The selected counter (e.g. 1/4) is part of the title area. `keepConvSelVisible()` manages scrolling and also considers the softkey overlay.

`.conversationRow .unreadDot{display:none!important}` appears in the HTML despite source template emitting an unread dot. Earlier user instruction specifically requested the dot in the native APK; it is therefore preserved as an intentional difference pending confirmation.

## 3. Confirmed HTML row maps (screen order matters)

| Screen | Source icon IDs (row order) |
|---|---|
| Messaging | 0570, 9001, 0724, 9002, 0603, 0602, 0723, 0415, 0582, 9003, 0584, 0598, 0572, 0604 |
| Organiser | 0557, 0715, 0661, 0703, 0528, 0558, 0569, 0580, DICT |
| Media | GT23, GT54, GT03, 0596, GT57, GTeq |
| Settings | 0595, 0608, 0513, 0628, 0574, 0571, 0692, 0915, 0568, 0560, Iphone, Iacc, Icfg, Isec, Irst |
| Contacts | 0593, 0922, 0626, 0599, 0562, 0605, 0585, 0572, 9007, 9006 |
| Gallery | 0534, 0619, 0625, 0620, GALth, GALgr, GALto, GALre, GALrc |
| Log | 0719, 0565, 0566, 0563, 0603, 0572, 0564, 0921, 0579, 0580, 0601, 0627, 0916 |
| Application list | 0617, 0616, 0534 |
| Go to / shortcuts | GT00 through GT59 (selected destination controls index) |
| Sync and backup | 0912, 0908, 0909, 0916 |
| Phone switch | 0912, 0913, 0914 |
| Music library | MEDnote, MEDnote, MEDplay, MEDart, MEDalb, MEDgen, MEDvid |
| Recipient picker | 0972, 1056, callreg, 0593, 0562, 0922 |
| All-calls-type screens | LGmiss, LGrecv, LGdial based on call event type |
| Bluetooth | 0806 family listed, but **no icons rendered** because page is on `noIcon` exclusion list |

**Important:** the native APK may have fewer/differently ordered list rows than HTML; copying icons by index is necessary but not sufficient. For example, HTML Organiser includes `DICT` as a ninth row, whereas the current native Organiser lists eight. HTML Gallery has nine items in its map while the native page currently lists four.

## 4. Current implementation, v1.2.0

- Added 79 source-derived bitmap assets in `RebornIconAtlas.java`, keyed by original IDs/state names.
- Replaced generated SMS read/unread/sent envelope graphics with exact HTML sprites.
- Added conversation-specific unread/read/sent sprites.
- Replaced the native new-message status symbol with the HTML bitmap.
- Corrected list-row icon IDs in Messaging, Organiser, Media, Settings, Contacts, Gallery, Log and Applications.
- Version retained two coordinates: HTML reference 6.342 and APK 1.2.0. Native contacts, SMS and call logs still use device permission/provider access, unlike HTML demo/localStorage data.
- Build validated successfully with GitHub Actions, but **visual parity on real Android screen was not tested**.

## 5. Defect backlog — cannot be declared complete

**High priority**
1. Rebuild the native SMS conversation display with correct selected/unselected bubbles, tails, widths, three-line truncation, timestamps, selected count and scroll clipping. v1.2.0 still uses a simpler list.
2. Add thread-level Mark as read and Mark as unread Options matching the HTML, with native Android SMS permission restrictions clearly handled.
3. Reconcile the native folder icons and previews with actual SMS provider sorting; review read-state persistence after app restart/reload.
4. Match Inbox, Sent items, Drafts, Saved items and message-selection views, including their icons and enable/disable states.

**Medium priority**
5. Align native page row counts and ordering before assuming the indexed icon map is correct; especially Organiser Dictionary, full Gallery, Contacts extras and Go to.
6. Port source icon assignments for unimplemented nested pages and remove artificial generic fallback icons when the HTML intentionally shows none.
7. Check source sprite intrinsic dimensions against rendered native bounds, themes, focus state and pixel-art filtering. The screenshot alone is not adequate evidence for all themes.
8. Match status signal, battery, Bluetooth, ringing/call and other UI glyphs; the native app still draws some substitutes.
9. Audit dynamic checkboxes and radio symbols, marking indicators, file-type icons, media controls and Options lists individually.

**Verification**
10. For each screen: record path from home, screenshot side-by-side, icon sprite reference, source CSS dimensions, selected/unselected states and transitions caused by Up/Down/OK/Back/Options; do not mark passed until evidence supports it.
11. Check SMS sequences: unread ➜ enter thread ➜ open received message ➜ return ➜ mark unread ➜ send reply ➜ delete with confirmation ➜ restart; ensure status and folder icons transition consistently.
12. Confirm which intentional differences the user wants preserved (red conversation dot; phone live-data integration; source-only fake data).

This is an evidence-based implementation inventory, **not** a claim that all icons and behaviors are fixed.
