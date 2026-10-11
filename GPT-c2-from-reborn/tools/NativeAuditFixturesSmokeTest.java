package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.HashSet;

/** Test fictional emulator records without loading any Android framework code. */
public final class NativeAuditFixturesSmokeTest {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }

    public static void main(String[] args) {
        ArrayList<NativeAuditFixtures.ContactRow> contacts = NativeAuditFixtures.contacts();
        ArrayList<NativeAuditFixtures.MessageRow> messages =
                NativeAuditFixtures.messages(System.currentTimeMillis());
        check(contacts.size() == 12, "12 contacts for scrolling and ordering");
        check(messages.size() == 12, "12 SMS samples");
        HashSet<Long> contactIds = new HashSet<>();
        int multipleNumbers = 0;
        for (int i = 0; i < contacts.size(); i++) {
            NativeAuditFixtures.ContactRow row = contacts.get(i);
            check(row.numbers.length >= 1 && row.name.length() > 0,
                    "Complete contact fields");
            check(contactIds.add(row.id), "Unique contact IDs");
            check(NativeAuditFixtures.isFixtureContactId(row.id), "Fixture IDs segregated");
            if (i > 0)
                check(contacts.get(i - 1).name.compareToIgnoreCase(row.name) <= 0,
                        "Alphabetically ordered");
            if (row.numbers.length > 1) multipleNumbers++;
        }
        check(multipleNumbers >= 2, "Secondary phone-number cases");
        HashSet<Long> smsIds = new HashSet<>();
        HashSet<Long> threadIds = new HashSet<>();
        int unread = 0, sent = 0, readInbox = 0;
        for (NativeAuditFixtures.MessageRow sms : messages) {
            check(smsIds.add(sms.id), "Unique SMS IDs");
            check(NativeAuditFixtures.isFixtureMessageId(sms.id), "Fixture SMS ID range");
            check(NativeAuditFixtures.isFixtureThreadId(sms.threadId), "Fixture thread ID range");
            check(sms.body.startsWith("DEMO"), "SMS is visibly labelled DEMO");
            threadIds.add(sms.threadId);
            if (sms.type == NativeAuditFixtures.INBOX && !sms.read) unread++;
            if (sms.type == NativeAuditFixtures.INBOX && sms.read) readInbox++;
            if (sms.type == NativeAuditFixtures.SENT) sent++;
        }
        check(threadIds.size() == 6, "Six conversations for navigation");
        check(unread == 3, "Three unread messages for red dots and closed envelopes");
        check(readInbox >= 3, "Several read messages for open envelopes");
        check(sent >= 3, "Mixed outgoing message examples");
        check("+447700900102".equals(messages.get(0).address),
                "Newest SMS is on Aaron's secondary number");
        check(messages.get(0).date > messages.get(1).date,
                "SMS rows are newest first");
        ArrayList<NativeSmsDetails.Entry> extracted =
                NativeSmsDetails.extract(messages.get(0).address, messages.get(0).body);
        int phones = 0, emails = 0, websites = 0;
        for (NativeSmsDetails.Entry entry : extracted) {
            if (NativeSmsDetails.Entry.PHONE.equals(entry.kind)) phones++;
            if (NativeSmsDetails.Entry.EMAIL.equals(entry.kind)) emails++;
            if (NativeSmsDetails.Entry.WEB.equals(entry.kind)) websites++;
            check(!entry.value.contains("2026-10-11"),
                    "Date must not become a selectable phone number");
            check(!"123456".equals(entry.value),
                    "Verification code must not become a phone number");
        }
        check(phones >= 2, "Use detail can extract sender and body phone");
        check(emails >= 1, "Use detail can extract email");
        check(websites >= 1, "Use detail can extract web address");
        check(!NativeAuditFixtures.isFixtureMessageId(123L), "Real system SMS ID not mistaken for demo");
        check(!NativeAuditFixtures.isFixtureContactId(101L), "Real provider contact ID excluded");
        System.out.println("NativeAuditFixtures: all demo-data smoke tests passed");
    }
}
