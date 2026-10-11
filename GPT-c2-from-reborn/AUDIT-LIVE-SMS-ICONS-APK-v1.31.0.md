# Native Nokia C2-01 — Live SMS state audit (APK v1.31.0)
Date: 11 October 2026
HTML reference pinned: v6.343

## Verified gap in v1.30.0
MainActivity refreshed phone data on initial foreground entry, permissions
changes and occasional app-initiated sends. It did not observe external
Android SMS database updates while the Nokia app remained foregrounded.
Consequently, after incoming SMS or changes in another messaging app, the
in-app unread envelope, red conversation dots and row/message icons could
remain stale until the next app resume.

Also refreshSmsFromPhone returned without purging its previously loaded
system SMS data if READ_SMS was revoked. Cached messages could remain visible
despite the revoked permission.

## Changes made in v1.31.0
- MainActivity dynamically registers a ContentObserver against
  Telephony.Sms.CONTENT_URI with notifyForDescendants=true while resumed.
- Android's main-thread Handler coalesces multiple onChange notifications
  into a single refreshSmsFromPhone call after 300ms.
- PhoneView.onSmsProviderChanged() refreshes SMS data and invalidates the
  native Canvas so unread indicators reflect the current provider snapshot.
- Observers and callbacks unregister on Pause/Destroy to prevent background
  activity, duplicate observers, leaks and unwanted background queries.
- Permission grant triggers provider observer registration, and Android
  runtime/provider failure is handled without crashing.
- READ_SMS revocation clears provider-derived cached SMS, Inbox, Threads,
  active message/thread and marked message/thread selections. App-local
  saved templates and drafts remain retained.

## Privacy and safety scope
- This code only reads the provider; does not mutate SMS read flags or SMS
  records, register the app as default SMS handler or send background SMS.
- No background service, notification or extra runtime permissions added.
- Existing app-local Mark as unread/read overrides still intentionally
  supersede Android provider state.
- Foreground-only observer aligns with existing Activity lifecycle.

## Checks against source
1. SMS provider observer registered only after READ_SMS permission check.
2. Handler de-bounces notifications and removes callbacks on Pause.
3. Any permission denial in refreshSmsFromPhone purges loaded SMS caches.
4. Same isSmsUnread function continues to drive conversation dots, envelope
   icons and simulated status envelope after refresh.
5. Android app is still native Java/Canvas (no WebView).

## Tests remaining on a real handset
1. With the app open to Conversations, receive an actual SMS; red dot and
   status envelope should appear without restarting the app.
2. Open Inbox; unread envelope should show before opening the SMS.
3. Open that SMS; it should turn read in app, while Android's provider READ
   flag is not modified by Nokia's app-local handling.
4. Mark another SMS unread in native Options and leave it in that state;
   external provider flag changes should not override an explicit app-local
   unread setting.
5. Open native app then use another messaging app to mark a phone SMS read;
   return to Nokia and verify changed provider state in the absence of an
   app-local override.
6. Revoke READ_SMS in system settings; resume Nokia and check no cached SMS
   remains in Inbox, opened message, status icon or Conversations.
7. Grant permission again; ensure lists return without duplicate observer.
8. Stress many provider notifications rapidly; no UI freezes or ANR.
9. Confirm no changes to phone SMS database and no background read after
   onPause. Compare exact Nokia icon sprites against HTML v6.343.

Automated GitHub CI can verify Java compilation and APK packaging, but
physical SMS observation and visual/behavioral parity are unverified.
