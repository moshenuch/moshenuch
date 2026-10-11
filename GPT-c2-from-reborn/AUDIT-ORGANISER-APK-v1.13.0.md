# Organiser audit — APK v1.13.0 / HTML reference v6.342

## Findings from native source
- Notes Delete and Delete all notes removed local data immediately, without confirmation.
- Notes Send note and Use detail were not executable: only a notice was shown.
- Notes/To-do/Calendar Memory status options were placeholders.
- To-do Delete mutated saved items without a confirmation or success notice.
- Calendar Delete was unimplemented even though advertised.
- Calendar saved note editing always appended, duplicating notes rather than updating the selected one.
- Date-based calendar navigation and week view have no native scheduling metadata.

## Remediation
- Added scoped `executeConfirmed` actions for single/all Notes and Calendar,
  and single To-do deletion, using persisted app-data records. Confirmation
  Yes/No continues to use existing Nokia-style softkeys.
- Notes > Send note copies the actual note body into SMS Compose with a blank
  recipient (user must explicitly send); Use detail opens text and character count.
- Added `showOrganiserMemory` computed from actual local array data, never fake
  device memory totals.
- Calendar Options now allows Edit of existing note; `saveEdit` updates at
  `editIndex` when applicable and creates a new entry only when requested.
- Calendar Go to today shows the phone's local date and transparently says
  these undated notes are not scheduled calendar events.
- Remaining nonfunctional options report `Not implemented` rather than a
  possibly misleading success message.

## Outstanding QA
- Test Notes: send existing note to draft (without sending), confirm Delete/No,
  Delete/Yes, Delete-all/No and Delete-all/Yes across app restart.
- Test Calendar: create two different notes, edit one, ensure the other remains
  unchanged, and verify no duplicate entry appears. Confirm deletion persists.
- Test To-do: edit existing entry, cancel/confirm Delete and check persistent state.
- Check compatibility with previously saved string-array JSON and empty lists.
- Compare option ordering, fonts and screenshots against actual Nokia/HTML.
  No live-device or pixel-diff verification was performed in this pass.

## Limits
GitHub Actions compilation only establishes that Java/Android build succeeds.
The native Calendar remains an undated notes list, not a date-indexed organiser.
