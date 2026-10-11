# C2 Reborn native APK continuation after v1.41.0

**Baseline:** APK v1.41.0; HTML reference v6.343 (read-only).  
**Source commit:** 08adce5e9372471ef9fec82d82359733a3896928.  
**CI:** GitHub Actions run 38131421649 succeeded; this proves build/tests, not emulator-screen parity.

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
