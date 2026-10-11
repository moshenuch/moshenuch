# Contact Groups implementation audit — APK v1.22.0 / HTML v6.342
Date: 11 October 2026.

## Before change
- Contacts root Group entry opened a static details screen stating
  "No groups defined", irrespective of real or app-local state.
- Contact detail Options > Add to group only displayed the command text.
- No group object model, group membership persistence, creation, rename,
  confirmed removal, or group-member call/message route existed.

## Implementation
- `NativeContactGroups` keeps up to 30 app-local groups, each up to
  100 unique phone-number memberships. JSON is stored in SharedPreferences
  `contact_groups_v1`, with stable UUID IDs and case-insensitive unique names.
- Members are recorded by normalized phone-number equivalence rather than
  mutable visible Android Contacts array positions. Labels resolve the
  current contact name dynamically and fall back to saved number.
- Native page routes: `contactGroups`, `groupMembers`,
  `groupContactPicker`, `groupContactAssign`.
- The existing Nokia text editor handles create/rename; both operations
  reject invalid/duplicate names. Creating group is possible via Options or
  the empty-state Create group entry.
- Group Options provide Open, Create group, Rename group, Delete group,
  Group details, and real app-memory counters.
- Group member Options provide Details, Call, Send message (to the
  SELECTED member, not the entire group), Add member, Remove member,
  Rename group, Delete group and Group details.
- Contact details > Add to group opens the saved-groups picker.
- Delete group and Remove member both snapshot the target ID and phone
  number and require existing Nokia-style Yes/No confirmation.
- All group writes mutate only app data. Neither Android Contacts groups nor
  source Android contacts are modified by these operations.

## Target device QA (NOT yet performed)
1. Add two groups with different names, reject a case-insensitive duplicate.
   Rename a group, exit/reenter and restart APK; verify persistence.
2. Add two contacts to group through the group picker and the contact
   detail route; test duplicate membership and empty-group behaviour.
3. Rename one Android phone contact and reload. The group's member label
   should update without changing stored membership by number.
4. Open group member Details; Call should dial selected contact only;
   Send message should populate recipient, NEVER auto-send.
5. Remove member > No, Yes; verify real Contacts record is untouched.
6. Delete group > No, Yes from both groups list and group members screen;
   confirm other groups and actual Android Contacts remain intact.
7. Deny READ_CONTACTS permission or delete an Android contact; existing
   group numbers must remain viewable with a numeric fallback.
8. Compare Nokia menu/options sequencing, icons/fonts and pixel positions
   against HTML v6.342: NOT verified yet.

## Open issues
Native groups are an app-local approximation, not Android system Contacts
groups, SIM group synchronisation, or complete Nokia group-options parity.
GitHub Actions compilation does not establish physical device functionality.
