package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Build-side layout regression based on the reported Pixel 4 / Android 11
 * demo SMS Conversations overlap. No Android runtime is required.
 */
public final class NativeConversationRowLayoutSmokeTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        check(NativeConversationRowLayout.nonOverlapping(),
                "Conversations icon, text, mark checkbox and unread dot never intersect");
        check(NativeConversationRowLayout.ICON_LEFT >= 0f,
                "Envelope icon remains on-screen");
        check(NativeConversationRowLayout.LABEL_X >= NativeConversationRowLayout.ICON_RIGHT + 6f,
                "Full leading character must begin at least 6px right of envelope icon");
        check(NativeConversationRowLayout.labelMaxWidth(false) == 181f,
                "Unmarked name has correct LCD width with dot reserve");
        check(NativeConversationRowLayout.labelMaxWidth(true) == 155f,
                "Marked name has shorter width reserved for checkbox");
        check(NativeConversationRowLayout.LABEL_X +
                NativeConversationRowLayout.labelMaxWidth(false) <
                NativeConversationRowLayout.UNREAD_DOT_CENTRE_X -
                NativeConversationRowLayout.UNREAD_DOT_RADIUS,
                "Unmarked label may not draw over unread dot");
        check(NativeConversationRowLayout.LABEL_X +
                NativeConversationRowLayout.labelMaxWidth(true) <
                NativeConversationRowLayout.MARK_CHECKBOX_LEFT,
                "Marked label may not draw under checkbox");
        check(NativeConversationRowLayout.UNREAD_DOT_CENTRE_X +
                NativeConversationRowLayout.UNREAD_DOT_RADIUS <
                NativeConversationRowLayout.LCD_WIDTH,
                "Red unread dot must stay within right screen edge");
        System.out.println("PASS: icon/name spacing, checkbox, unread dot and 240px bounds");

        ArrayList<NativeAuditFixtures.ContactRow> contacts =
                NativeAuditFixtures.contacts();
        HashSet<String> names = new HashSet<>();
        for (NativeAuditFixtures.ContactRow row : contacts) {
            check(row.name != null && !row.name.isEmpty(),
                    "Fixture contact name available for row tests");
            names.add(row.name);
        }
        check(names.contains("Leah Hart"), "Demo Leah Hart row present");
        check(names.contains("Aaron West"), "Demo secondary-number contact present");
        ArrayList<NativeAuditFixtures.MessageRow> messages =
                NativeAuditFixtures.messages(1_780_000_000_000L);
        boolean unknownNumber = false, alphanumeric = false;
        for (NativeAuditFixtures.MessageRow sms : messages) {
            if ("+447700900799".equals(sms.address)) unknownNumber = true;
            if ("NOKIA".equals(sms.address)) alphanumeric = true;
        }
        check(unknownNumber, "Demo unknown +447700900799 row present");
        check(alphanumeric, "Demo NOKIA sender row present");
        System.out.println("PASS: regression fixtures cover Leah, leading + and leading N");
        System.out.println("NativeConversationRowLayout: all smoke tests passed");
    }
}
