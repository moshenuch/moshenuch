GPT c2 from reborn — APK v1.8.0 / HTML reference v6.342

Native Java/Canvas. This HTML version is a reference; the APK remains a partial native implementation.

- Drafts are now structured records retaining recipient, body text, input mode and creation time.
- Existing body-only drafts from older APK releases migrate automatically; their original text is preserved.
- Opening a saved draft restores its recipient and editing mode. Saving again updates that draft rather than creating duplicate copies.
- Editing a draft and then sending the SMS removes only that specific draft, not every draft with the same text.
- Drafts > Options now has Open, Delete draft and Delete all drafts. Destructive actions require confirmation.
- Other v1.7.1 fixes remain, including real SMS reply addressing, per-number contact editing, Contacts marking, honest local SMS deletion semantics and dynamic APK version labels.

UNVERIFIED: visual parity for most nested icons/fonts and detailed SMS states, cellular send/delivery confirmation, remaining Options, native Gallery/Media features, handset UI testing and full HTML behavior parity. No WebView.
