GPT c2 from reborn — APK v1.22.0 / HTML reference v6.342

Native Android Java/Canvas application. NOT a WebView; partial Nokia C2-01 port.

Contacts > Groups native actions:
- Replaced the static "No groups defined" placeholder with an app-local
  groups screen. Users can create up to 30 named groups and rename groups.
- Open a group to browse its members, with names dynamically resolved from
  the currently visible Android or app-local Contacts list.
- From group Options > Add member choose a contact. The same membership
  action is available from Contact details > Options > Add to group.
- Selected group members have executable Details, Call and Send message.
  Send message opens the native SMS composer prefilled with that member's
  number; it does NOT automatically send a text to the whole group.
- Remove member and Delete group use the Nokia-style Yes/No confirmation.
  Both operations affect ONLY the APK's local group membership data and
  never delete or alter system Android contacts or actual Contacts groups.
- Group details show actual saved name and member count; memory status
  shows real app-local group and membership counts.
- Group IDs are stable UUIDs and memberships are stored as phone numbers,
  not mutable Contacts list indexes. Persisted JSON survives APK relaunch
  and Android Contacts name/reordering changes.
- Duplicate membership and duplicate group names are rejected.
- Unsupported SIM/cloud group synchronisation is NOT claimed.

Retains v1.21.0 Contacts speed dials, call-log/options, real media and
Gallery functions, source Nokia fonts/icons and SMS features.

KNOWN LIMITS:
- Groups are app-local; Android's system Contacts group provider is NOT
  modified. Deleting a real contact leaves that number in app-local groups
  until the member is explicitly removed (shown as a number).
- No group-wide SMS/mass calling, custom ringtones, group icons,
  SIM contact-group sync or deep original HTML screenshot parity.
- Real-device Contacts/phone permission testing remains outstanding.
