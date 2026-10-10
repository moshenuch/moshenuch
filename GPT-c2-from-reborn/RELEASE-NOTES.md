GPT c2 from reborn — APK v1.3.0 / HTML reference v6.342

Native Android Java, NOT a WebView. HTML source blob 8966ff284125bd6fa752a78f6bc4a51fd47a03a4.
New in APK v1.3.0: SMS conversation screen now uses dynamic Nokia-style message bubbles,
blue nonselected / white selected backgrounds, original sent/read/unread bitmaps,
per-message timestamps, three-line previews with ellipses, triangular tails,
selected-item auto scrolling and 1/N conversation count.
The centre softkey uses Reply for short incoming messages and Open for sent/truncated
messages; opening an individual incoming SMS changes only the local read overlay.
Previous v1.2.0 source-exact SMS status icons and menu bitmaps are retained.

LIMITATIONS: This is still a partial native port. Visual comparison on a physical
handset/emulator is not yet verified; options across all screens are not fully implemented.
An Android non-default SMS app cannot write read/delete state back to the device SMS provider.
Debug APK signatures may differ between CI builds.
