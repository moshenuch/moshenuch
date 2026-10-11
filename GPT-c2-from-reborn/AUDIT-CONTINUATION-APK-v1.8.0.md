# Native audit continuation — v1.8.0 (HTML v6.342 reference)

## Confirmed and repaired
- **A07, previously P1 data integrity:** older draft records stored only a bare body string. The new `DraftMessage` keeps `recipient`, `body`, `inputMode`, and `createdAt` as JSON. Original `drafts` string-array data migrates to `drafts_v2` the first time a user opens the new APK.
- New drafts are distinguished from drafts opened for editing. Editing updates the selected record; sending removes only that draft, not another record containing the same body.
- Drafts Options now implement Open, Delete draft, Delete all drafts, with confirmation for both destructive operations.
- Multiple editor exit pathways (Back, red End, Save message) use the same structured save method. A brand-new composer resets its current-draft pointer.

## Continuing high-priority audit findings
- **SMS receipts:** Android `SmsManager` initiation alone is not a carrier delivery confirmation. PendingIntent callback handling remains absent.
- **SMS icon parity:** source's firmware catalogue gives p0522/p0525 conversation bubbles, p1066 unread incoming, p0760 read incoming, p0759 sent; source also specifies checkbox p0456/p0457 and radio p0460/p0461. Native atlas has message icons but does not yet carry the checkbox/radio family and nested-page mappings.
- **System read/delete roles:** read state and deletes are local overlays, not system message mutations. User has been informed, but default-SMS role integration is not implemented.
- **Media/Gallery:** root icon lists are largely source-mapped; nested screens are not operational.
- **Typography:** native uses default Android typefaces rather than firmware Nokia Sans, making line wrapping and pixel parity unverified.
- **Contact Android provider:** exact Phone Data row edits are now implemented in v1.7.x; device verification using multi-number contacts remains necessary, and the selected contact list still shows the first phone number per aggregated contact.
- **UI verification:** GitHub compilation verifies syntax, not parity. Diff matching seeded screenshots/softkey paths on an Android test device is still required.
- **Static source limitation:** 3.7MB HTML source is too large for the connector's full-file text response, so the earlier documented source icon catalogue was used for behavior-specific comparisons. Do not claim an exhaustive current-file HTML code audit.

## Next implementation order
1. Source-exact checkbox and radio bitmap assets + per-state selection/marking.
2. SMS submitted/sent/failed/delivered state callbacks and careful permission scoping.
3. An executable Options inventory: no silent placeholder operations.
4. Nokia Sans fonts, pixel-scaled screenshot fixtures, nested screen flows.

## Release verification
APK v1.8.0 GitHub Actions compile/publish to be checked after commit. No device instrumentation for draft migration was run.

## APK v1.8.1 — additional SMS Options safety audit
- Implemented Mark all and Unmark all on individual message and conversation-thread lists; Delete acts on marked items after confirmation.
- Corrected a cross-thread marking hazard: opening a different thread now clears the previous thread's selection.
- Guarded Conversation details/Delete conversation when no actual thread is selected, avoiding an action on permission-placeholder rows.
- Marked indicators still use generic check glyphs. Firmware p0456/p0457 bitmap parity is pending source asset extraction and screenshot testing.
- Source-only changes: compile and release pipeline verification still required.

## APK v1.8.2 — cross-folder deletion safety
- Confirmed static bug: `executeConfirmed(deleteSms)` previously preferred all `markedSmsIds` even when on Inbox or MessageRead, which could silently hide unrelated messages marked earlier in another conversation.
- Fix: only consume `markedSmsIds` on `conversation` page and constrain the IDs to current `conversationMessages()`; Inbox/MessageRead uses `activeMessageId` only.
- Local-hide semantics retained: this never deletes provider SMS, and requires a Yes/No confirmation.
- Regression needs real-device demonstration with two conversation threads, marked items in one and Inbox deletion in another.
