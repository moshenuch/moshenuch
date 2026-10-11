# Nokia C2-01 native icon parity — Contacts v1.28.0
Reference: https://mbaiforinsticnt-dev.github.io/c2-reborn/index.html
HTML: v6.343, GitHub blob 2aabdff3423d1fdf76d1e1c5b7608348f916ce60

## Direct upstream findings
Upstream HTML uses:
- `listIcons.CONTbust` for a contact's default image, CSS 42 x 42 pixels
  at the upper right of the Contact details header.
- `listIcons.CONTphone` at CSS 22 x 22 beside the selected phone number.
- A contact name followed by a selected row with a pale background.

Native v1.27.0 instead rendered `c.drawText("●", ...)` as the portrait,
and drew telephone numbers with no phone icons. This failed source artwork
parity even though Up/Down selection had been implemented.

## Implementation
- New `NokiaContactSprites.java` stores the exact upstream embedded base64
  PNG bytes for the two specific sprites. No substitute icon library.
- Decodes and caches the Android Bitmap from these source bytes.
- Renders bust at x=187..229, y=43..85; the phone sprite at 22x22 in every
  visible number row, with stable selected/unselected text.
- Native details keeps a four-row sliding selection window, wraps Up/Down,
  and preserves all v1.27 actions on the selected number.
- Clips list contents to the screen's content area and ellipsizes long values.
- The icon asset base64 contains complete original PNG pixels; PNG source
  dimensions are bust 84x84 and phone 56x56.

## Regression checks required
1. Install APK and view Contacts > Names > saved contact: the original bust
   image must appear rather than a text circle.
2. Check all visible phone rows use the source Nokia telephone icon.
3. Navigate 2, 4 and 6 stored phone numbers with Up/Down and verify the
   selected pale-grey row remains visible while scrolling.
4. Long full names and international-format phone numbers must truncate
   without colliding with bust or exceeding the LCD right border.
5. Green key, Copy number, Speed dial and Send message must use highlighted
   number unchanged from v1.27.0.
6. Verify the Options overlay does not cut off rows or softkey labels.
7. Compare screenshot geometry, font metrics and selection colors against
   the live HTML on real Android hardware.
8. Verify RAM/bitmap cache remains bounded over repeated navigation.

## Limits
This change verifies only two relevant source PNGs and the native
renderer's usage. Full icon atlas, all nested menus, phone integration and
pixel-perfect layout equivalence remain open. CI success means Java
compilation and packaging succeeded, not that the app was tested on device.
