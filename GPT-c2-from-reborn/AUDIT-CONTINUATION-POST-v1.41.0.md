## Critical correction — real Android contact Edit Save (v1.42.0)

The failed Edit Save test was on a **real Android contact "Ab" created manually through the C2 app, with demo data OFF**, not a seeded fixture. The previous demo-only explanation was incorrect. This is a genuine provider/edit failure and top Contacts priority. Code now handles name-only changes with a direct StructuredName row update rather than the two-operation phone+name batch when the selected phone number is unchanged; failures display Contact save failed instead of incorrectly asserting a permission failure. The existing save-success path refreshes and returns to details. **Requires emulator retest**; this code-level hypothesis is not yet verified, and editing name-plus-number still follows the batch update path.

## Phone top priority (v1.42.0 user audit)

Real outgoing calls leak to stock Google Dialer, including the Google call screen and post-hangup dialer main screen. ACTION_CALL in AndroidBackend causes this. A genuine in-app real-call flow requires default phone role + InCallService with Telecom.Call lifecycle and a C2 activity, including call state/end/timer and return to C2. No claims of full in-app calling until role/manifest/service, on-device test and system behaviour are verified; emergency calls remain under Android system control. My shortcuts No cancellation, manual adding and sorting are verified.

## v1.42.0 on-screen regression findings

User-confirmed passes: demo-contact deletion gives explanatory notice, contact details avatar/blue handset row/Number 1 of 1 footer and Options/Call/Back match HTML; Home light softkey bar, new-contact automatic capitalisation, Names name-only rows.

Failures: (1) Edit contact Save stays on editor without confirmation and name does not update. Source reveals demoMode short-circuits saves as read-only, so tests must distinguish seeded demo from real writable Android contacts. Fix should support safe app-local fixture editing if demo edits are required without altering phone contacts; live provider errors need accurate failure notices. (2) Editing name typing 'xy' displays lowercase although Abc; verify multi-tap first-letter cycle and replace semantics. (3) Opening SMS thread leaves unread dot; source now marks entire active thread read on entry and persists local read-state, requiring retest. (4) Compose from contact details appears to show doubled Write message input; current Canvas contains only one composer panel, while sendMessage can show a 'Write message' notice on empty body. Reproduce physical touch/key path before modifying editor. (5) Tapping Home's centre softkey label did not open Menu, while physical OK did. Screen-centre touch hit region now maps to OK, requiring retest. (6) Grey swirl wallpaper still missing; current packaged bitmap must be compared with HTML artwork.

No claims of overall v1.42.0 completion. Preserve the above verified graphics.

## Names Options reference correction — 17 entries

User established exact Nokia HTML Names Options order: Search; Call >; Send message >; View conversations; Add new >; Edit >; Delete contact; Add to group; Selecting options >; Details; Add to contact; Edit contact; Copy contact; Move contact; Merge contact; Add to blacklist; Add to whitelist. Popup title Options only, no counter, solid dark list, white selection bar, softkeys Options / Details / Back.

Native source list is now exactly these 17 strings in this order. Existing Search, Call, SMS, Add new, Edit, Delete, Add to group, Details, View conversations, blacklist/whitelist are routed; Selecting options > displays Mark/Mark all/Unmark all/Delete selected. **Limitations:** Call >, Send message >, Add new >, Edit > still dispatch directly rather than opening reference-style submenus. Add to contact, Copy contact, Move contact, Merge contact presently show explicit not-implemented notices rather than silently mutating data. Selection submenu and 17-item list require emulator verification. Do not claim full functional parity yet.

## 2026-10-11 Options popup screenshot parity

Names Options screenshot shows black 28px title strip with only **Options**, six rows visible (Search, Call >, Send message >, Add new >, Edit >, Delete contact) and the underlying softbar Options / Details / Back. Names option strings already match these six in order within the twelve total. Suppressed numeric options counter in native overlay (now applies to all Options popups). Call-log list pages now show Details centre softkey, retaining existing open-details action.

**Evidence limitation:** The second attached screenshot appears to be the GitHub APK release page, not a Log Options popup. The user specified Log options text Mark >, Mark all, Unmark all, Delete selected, Add to blacklist, Add to whitelist, and matching softkeys; the current native call-log option list is *different*. Do not substitute those options without validating the specific Log screen/functionality, because existing Log Actions have separate semantics. Obtain the intended Log reference frame to confirm this before changing call-log menu actions.

## v1.41.0 multi-number Contacts and SMS audit follow-up

- **Multi-number edit:** user observed editor always targeted number 1. Native provider now carries raw-contact and phone-data IDs alongside each phone number. Editing from Contact details uses the selected number, labels the field Selected number and targets that specific Android Data row. Requires emulator validation with contacts linked across different raw-contact accounts; existing demo data remains read-only.
- **Edit title:** the earlier y=48 header-clearance fix applies to New/Edit contact alike; retest on v1.42.0.
- **Save:** code now returns to contactDetail for edits, selects the edited number and displays Contact saved. Failures still stay in the editor and display an error; verify using real writable Android contacts.
- **SMS Use detail:** removed explanatory/developer placeholder text; detail page displays extracted value.
- **SMS counter:** replaced total/160 with remaining/segment count, e.g. six over 160 -> -6/2 needs verification against implementation: current calculation for 166 characters is 140/2 under the concatenated-segment rule, not -6/2. Exact Nokia reference convention is still an open parity detail.
- **Demo label:** status DEMO glyph now gated by debuggable flag, as are demo tools. Home debug footer previously removed. Ensure release APK build/type and actual demo fixture exposure do not leak into production.
- **Verified:** current Names list styling (bold selected name, subordinate number and scrollbar), and empty-number contact-save validation.

# C2 Reborn native APK continuation after v1.41.0

**Baseline:** APK v1.41.0; HTML reference v6.343 (read-only).  
**Source commit:** 08adce5e9372471ef9fec82d82359733a3896928.  
**CI:** GitHub Actions run 38131421649 succeeded; this proves build/tests, not emulator-screen parity.

## Reconfirmed v1.41.0 Menu/Names/Log audit; follow-up native changes

User reiterated the 3×3 menu icon artwork pass, Nokia condensed label/font mismatch, grey swirl vs black vignette, and dark rounded selection tile; Names must show name-only and Details softkey with Business/Family/Friends group rows; Names Options must omit '1-12'; deletion must show result and refresh; All calls '(empty)' is accepted. Previously committed fixes for name-only, Details, count suppression and delete refresh/notice remain in source.

**New source changes:** Names now appends actual app-local contact groups to its list and opens group members for a selected group rather than treating its row as a contact. New installations seed Business, Family and Friends; preferences with an existing group list (even intentionally empty) are respected. Existing group membership implementation remains app-local, not Android Contacts provider group membership. Group icon artwork, Home/Voicemail entries and precise screenshot row order remain open. Requires emulator verification before declaring fixed.

## v1.41.0 side-by-side graphics audit (HTML v6.343)

User-verified gaps: Home should show grey swirl wallpaper, Demo network, date like Sun 11-10-2026, small clock top-right, My style 1, light Nokia-font Go to/Menu/Names softkey bar. APK had near-black wallpaper, large centred time, abbreviated date, General, white text on black and intrusive build/debug footer. Status and list/menu fonts and signal/battery art do not match condensed Nokia font/reference. Go to uses large white selection rather than dark glossy highlight. Call reference displays Calling. on blue panel with contact NAME and progress bar, softkeys Options / Menu / Loudsp. (the separate report asking for centre End conflicts with this visual reference and should be reconciled explicitly). Names requires Business/Family/Friends plus Home/Voicemail rows, contact detail silhouette and blue handset glossy bar. Lock keypad Unlock -> Now press * is user-verified.

**Source changes:** Home now displays compact top-right clock, Demo network, numeric day-month-year date, no visible build/debug footer, with a light softkey bar. Existing wallpaper bitmap is used without darkening overlay; **its exact swirl artwork is not yet verified**. Existing saved profile value is preserved to avoid silently rewriting user preferences; default/profile parity still pending. Remaining art, condensed font diagnostics, 3×3 selection, Go to glossy highlight, contact avatar, group rows and reference call panel remain open. Require screenshot retest; do not claim pixel parity.

## User v1.41.0 Phone/Log/SMS emulator audit (seeded data)

**Defects:** call screen shows raw number rather than matching contact name; centre End did not end call despite repeated presses; stock Android white Calling screen appears above C2; Log recorded 00:11 for call observed 00:52+; contact's saved 555-5 becomes 5555 in Log.

**Visual/interaction mismatches:** speaker softkey stays Loudsp. after enabling rather than changing to Handset; Conversations unread red dot appears on icon AND at right edge; conversation messages are chat bubbles rather than HTML/Nokia list rows.

**Verified:** Log menu arrow icons; Thread Options has 15 entries; Conversations Options has 11 entries. Contact edit/delete, multi-number, Use detail remain in testing.

**Source fixes started:** call screen resolves display name via contactNameFor(activeCall); centre OK calls endCall instead of toggleSpeaker; RSK toggle shows Handset when speaker enabled. **Important limitation:** AndroidBackend.endCall() can fail or be unavailable, while local endCall() currently clears the visual call state regardless; on-device termination is not yet proven. Likewise ACTION_CALL launching platform UI is an Android integration limitation—not solved by changing Canvas. A fully custom in-call experience may require default-dialer/InCallService integration and user-granted role, not merely suppressing the system app. Timer is elapsed time since intent dispatch rather than actual connection duration; compare Android call-log duration before changing source. Number formatting should be preserved in presentation while comparisons use normalized forms. Unread-dot and conversation-list rendering require screenshot/reference confirmation before choosing the correct single indicator and layout.

**Release gate:** Verify physical call termination (not only on-screen timer), actual connection state, permission handling, log duration against provider, and contact formatting. Do not mark the five uncorrected issues as resolved.

## Further v1.41.0 Contacts editor audit

- **Confirmed defect:** OK after entering the contact Name failed to advance to Number; subsequent number-key presses were interpreted as alphabetic multi-tap. Production key handler called saveEdit() on OK for any field. Corrected source so first OK on Name resets multi-tap state, focuses Number and selects 123 mode; OK on Number and Save softkey invoke validation/save. Up/Down field changes also switch alphabetic/numeric modes.
- **Punctuation report:** typing `bob q.` produced saved display name `bob q`. The Contacts save path uses Java `String.trim()` which removes surrounding whitespace, **not periods**. Thus the source currently has no explicit punctuation stripping at save; test whether the dot was truly committed before Save, and inspect Android Contacts provider after round-trip. Preserve user-entered punctuation in names; do not claim resolved without emulator confirmation.
- **Verified:** Empty number is rejected with `Enter name and number`; retain this validation.
- **Still requires emulator retest:** OK focus change, number digits, punctuation persistence in detail view/provider and Name display.

## User v1.41.0 Contacts editor and search audit (on emulator)

**Verified:** contact Save workflow and success notice; case-insensitive alphabetical sort; 12-item Names Options menu. Do not regress these.

**Observed defects:** New contact title clipped by form panel overlapping Nokia status/title area; name entry starts lowercase rather than Abc; search characters show inconsistent capitalisation; consecutive search space presses do not visibly advance.

**Source correction committed in 1.42.0 development:** contact editor panels moved from y=28/92 to y=48/112 to clear status/title baseline y=37; new/edit contact begins in Abc, with initial letter after empty field or space capitalised but later letters lowercase (ABC, abc, 123 still selectable); repeated 0 in alphabetic contact search now inserts independent spaces instead of replacing the previous space in a multi-tap cycle. **Needs emulator verification**, especially capitalisation across repeated key presses, spacing display and search matching.

**Further Contacts, SMS and Phone audit findings pending.**

## User v1.41.0 screen audit — Contacts and visual parity

- Menu 3×3 icons: existing icon set matches reference reasonably well (user verified). Gaps: plain sans labels rather than Nokia condensed type; black vignette rather than grey swirl wallpaper; light selection outline rather than dark rounded tile.
- Names: reference shows name-only contact rows plus Business, Family, Friends group rows with group icons and center softkey **Details**; APK had name+number and **Select**. Source corrected name-only rows and Details center softkey; **group rows in Names are still pending** (existing Groups screen is separate).
- Names Options overlay: source corrected to hide the right-hand '1-12' item count on that page.
- Contact deletion: user observed confirmed deletion left contact visible with no toast. Source now refreshes after either provider result, checks that deleted contact ID is absent, clamps selection, and displays explicit success/failure notice. **Requires device retest**; external sync/restoration can make a contact reappear, and current backend uses provider raw-contact deletion.
- Log > All calls '(empty)' accepted by user.
- Unrelated pending tests: Mark subset, templates/Saved, Organise.

## User emulator audit: APK v1.40.0 (historical, not v1.41.0 confirmation)

**Verified on screen:** shortcut assignment/rejection notices readable; Settings > Phone stub has no overlap; My shortcuts Restore defaults confirmation offers Yes and resets Left to Go to.

**Still failing in v1.40.0:** Calculator title upper half clipped. **Later source fix in v1.41.0** moved content below header, but on-device retest is still pending.

**UI parity mismatch:** My shortcuts root displays Options / Select / Back but Nokia reference expects Select / Back only. In current source, the left Options binding is used for Restore defaults; do not simply hide it without specifying an alternative accessible reset path. Defer this non-core correction until Contacts/SMS/Phone priorities are covered, unless parity testing discovers a safe fix.

**Pending emulator tests:** conversation Mark subset; Messaging templates and Saved; Menu/Go to Organise. Do not mark these verified without user results.

## Current mandatory scope: Contacts, SMS and Phone — function AND graphics

Prioritize Contacts, Messaging/SMS and Phone over all unrelated features. **Visual parity is an acceptance criterion, not deferred polish.** Match the read-only HTML v6.343 and Nokia C2-01 reference in both behavior and rendering.

For **each** of the three core feature areas, verify:
- Real Android provider integration and safe create/read/update/delete operations (where supported), permissions and failures, realistic data and persistence.
- Correct screen transitions, softkeys, Options actions, selection/mark state, prompts and edge cases.
- Exact/closest-possible Nokia icon artwork and every state variation (especially SMS unread/closed envelope, read/open envelope, sent and status bar icons, and contact/phone call-type icons).
- Fonts, text weight, glyphs, truncation, baselines, line height, colours, backgrounds, header/footer/softkey positioning, margins and list row dimensions.
- 240x320 Nokia composition with screenshot-side comparisons to the HTML rendered at matching viewport, including selected/empty/long-name/multinumber/unread/mark/error states.

Do not treat passing Java smoke tests or successful APK compilation as evidence of graphic parity. Require screenshot-based emulator comparisons and documented deviations. Avoid unrelated app/Settings/Calculator polish until the core three are dependable and visually faithful.

## Confirmed implementation baseline
- Native Java/Canvas, not WebView.
- v1.41.0: measured, two-line bounded Nokia notices; Calculator and Loan title-safe panel start.
- v1.39.0: user-verified uncut conversation names with mark indicators and unread dots.
- v1.38.0: user-verified Home softkeys, four navigation directions, persistence after force-stop, and Menu-reachability protection.
- Unassigned navigation picker intentionally starts at None.
- v1.37.0: bulk SMS read/unread selection restricts actions to marked received messages in active thread; static/fixture tests pass.

## Priority emulator acceptance matrix (not yet verified)
1. On Pixel 4 Android 11 check full text of *Keep Menu on at least one home key*, no clipping at 240px or bleed-through, including long Assignment notices.
2. Check full Calculator and Loan calculator titles above panel.
3. My shortcuts > Restore defaults: No retains assignments; Yes restores all defaults; ensure persistence over force-stop and relaunch.
4. In Conversations, mark only a subset of incoming SMS, then execute Mark as read / unread; verify only chosen incoming rows change, unread status indicators are correct and sent/out-of-thread rows remain unchanged.
5. Test Messaging templates and Saved: view/create/select/delete/back with confirm prompts and compare exact HTML v6.343 actions.
6. Test Menu > Options > Organise and Go to > Options > Organise, check order, hidden items and persistence on cold start.
7. Check icon fidelity and menu positions against HTML v6.343.

## Release criteria for v1.42.0
- Correct demonstrated parity gaps in production Java, not just audit documents.
- Add a regression test for each fix, confirm source-to-screen wiring, run GitHub Actions and install on emulator.
- Publish versioned APK with distinct HTML reference in filename.
- Do not claim emulator confirmation from CI-only tests.
- Address stable APK signing separately; debug signing may change between builds and force uninstall.

This checklist documents the continuation point; it is not a v1.42.0 release.
