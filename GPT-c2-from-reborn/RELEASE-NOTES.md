GPT c2 from reborn — APK v1.11.0 / HTML reference v6.342

Native Android Java/Canvas (not WebView); still a partial HTML-to-native port.

Call Log audit repairs:
- Pressing OK on a call now displays real direction, caller name, phone number,
  recorded call date/time and duration, rather than only 'Ready to call'.
- Call-list Options > View opens that actual selected record.
- Call-list Options > Call dials the selected record only when present.
- Options > Send message opens SMS Compose with the selected record's number.
- Options > Save to contacts opens the actual native contact editor, prefilled
  with the record's number and any available contact name; saving still requires
  user action and Android Contacts permission.
- Options > Delete and Clear list are executable, Yes/No confirmed and explicitly
  APP-LOCAL hide operations: they never modify Android's call log provider.
- Log > Clear lists hides only in the app, with confirmation. Log > Restore hidden
  calls reverses app-local hides and restores visible records from Android.
- Call timers now displays a count and duration from actual visible call records,
  not a static fake report.

Prior improvements retained: v1.10 real SMS Forward/metadata, original Nokia
selection icons, structured drafts, contacts and message selection safeguards.

Not fully source-parity: Nokia typography/screenshots, numerous nested Options
and Media/Gallery actions remain pending. Android device integration needs testing
with demo or consenting data before declaring it verified.
