# Deep native parity audit — HTML v6.342 / APK v1.6.0
**Audit date:** 11 October 2026  
**Scope:** Nokia C2 Reborn's documented icon catalogue (including firmware evidence passes 141, 143–146), the latest native Android source `C2PhoneView.java`, `AndroidBackend.java`, `RebornIconAtlas.java`, prior repository parity audit and the user's 10 October conversation screenshot.  
**Status:** Static source + documentary audit; **not** a verified full visual/function parity certification.

## Method and evidence limits
- Inspected native screens, keypad dispatcher, options handlers, message state, icons, data persistence, hardware integration, build status.
- Rechecked the source's `ICON-CATALOGUE.md` and `START-HERE.md` evidence. The original `index.html` is roughly 3.7 MB of inline HTML/assets; the connected GitHub file reader returned an empty content body for this file, while smaller documents and native sources read correctly. Therefore claims about source behavior are grounded in the catalogue and pre-existing line-by-line HTML v6.342 audit, rather than an independent full parse of today's index.html.
- The original's documented v6.314 route sweep covered **204 page entries**, but that is *not* a current v6.342 parity test and is not comparable one-to-one with native screen counts.
- No physical phone run, emulator navigation pass, source-served screenshots across every state, or instrumented screenshot diff was carried out in this pass. Do not label any screenshot-perfect result VERIFIED.

## Counts measured directly in APK v1.6.0 code
- `itemsFor()`: **53 distinct list-page cases**, some of which are deliberate info/simulation pages.
- `iconForRow()`: **10 explicit bitmap-list groups** (messaging, organiser, media, settings, contacts, gallery, log, applications, inbox, sent). Conversation icons use a separate state-dependent drawing code path. Many nested pages use text-only rows, but some may legitimately have no icons; individual source comparison is required.
- `RebornIconAtlas`: **87 exact/derived bundled bitmap keys**, including status and SMS variants.
- `C2PhoneView` uses `android.graphics.Typeface.DEFAULT` or `DEFAULT_BOLD` **27 times**; no Nokia typeface loading was found in native Java or project font assets.
- Native APK v1.6.0 CI **succeeded**, and release asset `GPT-c2-from-reborn-HTML-v6.342-APK-v1.6.0.apk` exists. The build's **HTML v6.342** tag identifies the reference, **not full feature parity**.

## Firm source/icon rules (documented original)
| Context | Documented icon / behaviour |
|---|---|
| Messaging > Conversations menu row | p0955: two gold speech bubbles |
| Messaging > Outbox row | p0591: blue upward-arrow tray |
| Conversation list, most recent received | p0522 gold received bubble |
| Conversation list, most recent sent | p0525 grey sent bubble |
| Unread conversation | received bubble + separate red unread marker, with unread count text |
| Incoming unread / focused message | p1066 closed envelope with green down-arrow |
| Incoming read message | p0760 open envelope |
| Sent message | p0759 envelope + green check |
| Sent message details / info | p0786 envelope + blue information badge |
| Status bar while unread SMS exists | distinct white envelope, immediately after battery in original's documented status-icon order |
| Multi-select lists | p0456 empty checkbox, p0457 checked checkbox (not plain text ✓) |
| Single-select pickers | p0460 unselected radio, p0461 selected radio |

**Important exception:** The previous HTML v6.342 analysis notes source CSS hides the conversation-list `unreadDot`, even while rendering the element. The native APK displays a red dot because the owner explicitly requested one. Keep this as a tracked intentional user-requested difference until directly reconciled.

## Findings — confirmed native code behaviour

| ID | Priority | Area | Evidence and consequence | Suggested correction |
|---|---|---|---|---|
| A01 | **P0 functionality** | Inbox Reply | `chooseOption` sets `recipient = inboxName(inboxIndex)`; `inboxName` resolves contact's **display name**, while `sendMessage` passes the raw recipient string to `SmsManager.sendTextMessage`. Contact-backed Reply can fail/address incorrectly. | Resolve actual SMS sender number/URI, not display name; add integration regression test. |
| A02 | **P1 functionality** | Sent items | `itemsFor("sent")` lists sent messages, but `openSelected()` has **no "sent" case**; pressing OK on a sent item has no corresponding message-opening action. | Bind row to SMS ID; open correct message detail and options. |
| A03 | **P1 safety/UX** | SMS deletion | `executeConfirmed("deleteSms"/"deleteThreads")` only writes SMS IDs to persistent `hiddenSmsIds`; the real Android inbox still contains them. Screen says **Deleted** without indicating app-local hiding. | Clearly label local hide, or implement legitimate default-SMS-app deletion only after explicit user authorization and Android role/permission handling. |
| A04 | **P1 functionality** | Contact Mark | `chooseOption` handles Contacts Names > Mark with `showNotice("Mark")` only. No selected-state change, checkbox or bulk action follows. | Implement contact selection set and firmware checkbox states, then bulk operations. |
| A05 | **P1 functionality** | Call-log Clear | Both `Clear lists` and `Clear list` show success notices, but no provider change or local cleared-record filter. A fresh view still shows the same records. | Don't claim clearing without effect; gated delete or clear-as-local-view operation plus confirmation. |
| A06 | **P1 reliability** | Sending SMS | Android backend returns `true` immediately after submitting `SmsManager.sendTextMessage` without sent/delivery result PendingIntents. Native UI immediately says `Message sent`, even though delivery might subsequently fail. | Distinguish queued/sent/delivered/failed states, populate Outbox/Reports from real callbacks. |
| A07 | **P1 data integrity** | Drafts | Draft persistence stores only `messageBody` as a string. Selecting a saved draft explicitly resets `recipient = ""`. Messages with recipient lose that field. | Persist structured drafts (recipient, body, editing mode, timestamp). |
| A08 | **P1 functionality** | Gallery/Media | All Gallery rows open synthetic `itemDetail` with `Folder is empty`; Camera is explicitly called a `Native camera screen placeholder`; video, radio and voice recorder mostly open static text. Icons are present, but most corresponding features aren't. | Implement source-accurate navigation and source-approved native media integrations; keep explicit nonfunctional labels until implemented. |
| A09 | **P1 functionality** | Contacts root | Select `Synchronise all`, Groups, Speed dials, Del. all contacts, Move/Copy contacts generally leads to generic `itemDetail`. Most do not perform the named function. | Implement functionality or clear Unsupported notices, never imply completed mutations. |
| A10 | **P1 functionality** | Options | `openOptionsForPage()` advertises many options; `chooseOption()` often falls back to `showNotice(choice)` (e.g. Gallery sort/view, Contacts Search, multiple SMS editing controls). Visible entries are not necessarily executable. | Build per-page option execution matrix; implement or explicitly disable all nonfunctional entries. |
| A11 | **P1 source parity** | Marking icon | Conversation/message marking currently paints a Unicode `✓` over rows. Original firmware catalogue documents separate checkbox icons p0456/p0457 for multi-mark lists. | Add marked/unmarked/disabled bitmaps to atlas and map by selection state. |
| A12 | **P1 visual parity** | Typography | Native uses Android default fonts repeatedly; original Reborn has Nokia Sans/font fallbacks. Text width, line breaks, pixel geometry and button labels cannot be pixel-identical. | Bundle properly licensed project-approved Nokia font resources and assign per-screen font styles; compare screenshots. |
| A13 | **P2 visual parity** | Options and nested screens | `iconForRow()` covers only 10 list families; many nested source screens are rendered generically. No evidence that per-screen icon and focus/disabled/state rules were ported. | Catalogue icons on each reachable submenu screen; check selected, disabled, marked, focus and off states. |
| A14 | **P2 visual parity** | Conversation bubbles | Native draws approximate selected white / unselected navy bubbles and Nokia image states, but it uses generic fonts and manual `Canvas` wrapping and timestamps. Pixel geometry against screenshot remains untested. | Capture identical seeded conversations from HTML/native and diff at 240×320. |
| A15 | **P2 version consistency** | About screen | App is `1.6.0`; `itemsFor("aboutNative")` still says `APK version: 1.5.0`, and selecting that screen displays `Native APK 1.1.0`. Home text is updated to 1.6.0. | Derive all version labels from BuildConfig.VERSION_NAME and one HTML reference constant. |
| A16 | **P2 native parity** | Local SMS read overlay | Incoming `unread` is based on `Telephony.Sms.READ` plus local `localReadSmsIds` (persistent). Entering a conversation does not auto-read; opening a message marks locally. Correct within APK, but system inbox unread status can remain unchanged. | Document intentional Android role limitation; test reopening and notification badge state after local read. |
| A17 | **P1 data integrity** | Contact edits | Android backend's `updateContact` updates all `Phone` data rows for a given `CONTACT_ID` / phone MIMETYPE, not one selected phone row; contacts with multiple numbers can have all numbers overwritten. | Target individual Data row IDs or use proper raw-contact/aggregate-contact operations with regression coverage. |
| A18 | **P2 cosmetic/functional** | Synthetic content | App seeds example contacts and static saved messages / delivery reports. Source may also seed demo data, but no live-phone parity test validates these fixtures. | Distinguish demo vs live device records and ensure example rows cannot be confused with real delivered messages. |
| A19 | **P2 key/UI** | Options popup | Options screen is a simple full-width text list. The HTML/firmware contains icon radio/checkbox selection controls and context-dependent Option submenus. | Compare popup geometry, scroll anchor, counters, selection/disabled bitmap and action mapping. |
| A20 | **P2 completeness** | Apps / Settings | A number of exposed pages, including Settings Phone and Security, remain generic details or "Not implemented" rather than actual forms with reversible state. | Build screen-by-screen route inventory, prioritizing high-use flows. |

### Source references by native line
Use base path:
`https://github.com/moshenuch/moshenuch/blob/main/GPT-c2-from-reborn/app/src/main/java/com/moshenuch/c2reborn/`

- `C2PhoneView.java#L569-L671` — list row render, conversation, icon states.
- `C2PhoneView.java#L831-L877` — message softkey condition and row icon mapping.
- `C2PhoneView.java#L1493-L1634` — list page data.
- `C2PhoneView.java#L1634-L2052` — route actions; includes placeholders.
- `C2PhoneView.java#L2194-L2266` — SMS send/draft persistence.
- `C2PhoneView.java#L2854-L3279` — option menus, handlers and confirmations.
- `C2PhoneView.java#L3292-L3318` — local SMS read logic.
- `AndroidBackend.java#L145-L205` — contact edits/deletes.
- `AndroidBackend.java#L233-L260` — SMS sending.

Original authority: `mbaiforinsticnt-dev/c2-reborn/ICON-CATALOGUE.md`, passes 141, 143, 144–146; `START-HERE.md` for historical verification scopes and limitations.

## What already works in source
- Native Java/Canvas app; not a WebView.
- Explicit Nokia icon map for major top-level list families; 87 atlas keys including message direction/read state.
- Conversation rows select unread/received/sent icons by device data; unread status envelope is conditional.
- Incoming SMS remains unread when entering thread; local opening changes app-visible state.
- Separate SMS conversation bubble renderer with variable-height previews, outgoing/incoming tails and selection.
- Mark, Mark all and confirm-delete flows are wired for SMS/conversation selection, though delete is local hide, not provider delete.
- Actual Android SMS/calls/contacts read integration, conditional on permissions, with important limitations described above.
- Native v1.6.0 CI completed successfully; icon status alignment pass committed.

## Recommended repair order / validation
1. **Safety + operational accuracy:** fix Inbox Reply recipient; fix Android contact update target; change SMS/Delete/Call-log clear success semantics; add SMS delivery result handling.
2. **Message execution:** Sent item opening, structured drafts, options execution/marking with source checkbox icons; test outgoing/received/read/unread transitions and status icon.
3. **Visual fidelity:** custom Nokia fonts, source exact per-state icon resource/margins; compare selected/unselected and navigation screenshot fixtures.
4. **Full route coverage:** inventory every HTML screen/Options and map to native route, actions and state, including currently placeholder media/gallery/apps/settings.
5. **Verification:** launch native APK on actual test Android phone (with *dummy*, non-sensitive SMS/contacts) and the served HTML on same-scale screenshot fixtures; automate state/action assertions; only then mark an individual screen verified.

## Audit conclusion
This project has a substantially improved native icon foundation but is **not yet functionally or visually identical** to HTML v6.342. The remaining work is not only cosmetic. At least one SMS reply path and one contact data-edit path require priority fixes, and many displayed controls are placeholders. Treat this as a traceable **deep static audit**, not a completed device-level Nokia emulator certification.
