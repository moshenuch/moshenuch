GPT c2 from reborn — APK v1.5.0 / HTML reference v6.342

NATIVE Android Java, no WebView. Source HTML Git blob 8966ff284125bd6fa752a78f6bc4a51fd47a03a4.
Now aligns primary menu row ordering/count more closely to HTML v6.342:
- Settings: all 15 original entries with source-exact icons and corrected navigation indices;
  native About APK is accessible through Settings > Options > About APK (not a bogus HTML row).
- Contacts: all 10 original entries, including newly extracted exact Move/Copy contacts icons.
- Log: all 14 source menu entries; some entries are information-only pending implementation.
- Organiser: all 9 source entries, including Dictionary with original icon.
- Gallery: all 9 original entries and exact folder icons from v1.4.0.
- SMS: Nokia-style conversation bubble layout/scroll and state-aware envelope icons from v1.3.0.

LIMITATIONS: Many nested feature screens and Options actions remain placeholders, not a
fully equivalent HTML port. A device screenshot parity check is still required.
Real SMS read/delete mutations are locally simulated due to Android provider restrictions.
