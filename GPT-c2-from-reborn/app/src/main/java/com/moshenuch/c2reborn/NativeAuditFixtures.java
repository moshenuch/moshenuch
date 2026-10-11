package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Pure-Java, deliberately fictional test records for the DEBUG build only.
 *
 * These records are NEVER added to Android ContactsContract or Telephony.
 * Stable IDs are reserved well outside normal provider ranges so app-local
 * read/hidden overrides can be reset without touching actual phone records.
 */
final class NativeAuditFixtures {
    static final long CONTACT_BASE = 9_300_000_000_000L;
    static final long MESSAGE_BASE = 9_400_000_000_000L;
    static final long THREAD_BASE = 9_500_000_000_000L;
    static final int INBOX = 1;
    static final int SENT = 2;

    static final class ContactRow {
        final long id;
        final String name;
        final String[] numbers;

        ContactRow(int number, String name, String... numbers) {
            this.id = CONTACT_BASE + number;
            this.name = name;
            this.numbers = numbers;
        }
    }

    static final class MessageRow {
        final long id;
        final long threadId;
        final String address;
        final String body;
        final long date;
        final int type;
        final boolean read;

        MessageRow(int n, int thread, String address, String body, int minutesAgo,
                   int type, boolean read, long timestamp) {
            id = MESSAGE_BASE + n;
            threadId = THREAD_BASE + thread;
            this.address = address;
            this.body = body;
            this.date = timestamp - minutesAgo * 60_000L;
            this.type = type;
            this.read = read;
        }
    }

    private NativeAuditFixtures() {}

    static ArrayList<ContactRow> contacts() {
        return new ArrayList<>(Arrays.asList(
            new ContactRow(1, "Aaron West", "+44 7700 900101", "+44 7700 900102"),
            new ContactRow(2, "Abigail Cole", "+44 7700 900103"),
            new ContactRow(3, "Benjamin Reed", "+44 7700 900104"),
            new ContactRow(4, "Chaya Lewis", "+44 7700 900105"),
            new ContactRow(5, "David Bell", "+44 7700 900106"),
            new ContactRow(6, "Ella Ford", "+44 7700 900107"),
            new ContactRow(7, "Fiona Green", "+44 7700 900108"),
            new ContactRow(8, "Hannah Stone", "+44 7700 900109"),
            new ContactRow(9, "Leah Hart", "+44 7700 900110", "+44 7700 900111"),
            new ContactRow(10, "Noah Wood", "+44 7700 900112"),
            new ContactRow(11, "Oliver Price", "+44 7700 900113"),
            new ContactRow(12, "Zoe Clarke", "+44 7700 900114")
        ));
    }

    /** Newest-first messages; 6 threads, inbox/sent, read/unread, saved/unknown. */
    static ArrayList<MessageRow> messages(long now) {
        return new ArrayList<>(Arrays.asList(
            new MessageRow(1, 1, "+447700900102",
                "DEMO: Please call +44 7700 900122, email audit@example.org or visit "
                + "https://example.org/audit/12345678. Appointment 2026-10-11; code 123456.",
                1, INBOX, false, now),
            new MessageRow(2, 2, "+447700900110",
                "DEMO: New message from Leah. This should show a closed envelope "
                + "and a red conversation dot until read.", 3, INBOX, false, now),
            new MessageRow(3, 1, "+447700900102",
                "DEMO: Thanks Aaron, I will reply later.", 8, SENT, true, now),
            new MessageRow(4, 3, "+447700900799",
                "DEMO: Unknown sender. This sender should offer Save a contact.",
                12, INBOX, false, now),
            new MessageRow(5, 2, "+447700900110",
                "DEMO: Earlier read message. Look for the open envelope.",
                19, INBOX, true, now),
            new MessageRow(6, 4, "NOKIA",
                "DEMO service alert: code 123456; more at www.example.org/help.",
                25, INBOX, true, now),
            new MessageRow(7, 1, "+447700900102",
                "DEMO: Earlier message to Aaron's SECONDARY number. "
                + "Options > Contact should select that number.", 36, INBOX, true, now),
            new MessageRow(8, 3, "+447700900799",
                "DEMO: Reply copied for unknown sender.", 44, SENT, true, now),
            new MessageRow(9, 2, "+447700900110",
                "DEMO: Sent item in Leah's conversation.", 51, SENT, true, now),
            new MessageRow(10, 5, "+447700900104",
                "DEMO: Benjamin sends a read SMS. Contact search should find Benjamin.",
                63, INBOX, true, now),
            new MessageRow(11, 5, "+447700900104",
                "DEMO: A previous outgoing reply to Benjamin.", 71, SENT, true, now),
            new MessageRow(12, 6, "+447700900113",
                "DEMO: A longer message for scrolling and Options. "
                + "Numbers 020 7946 1234 and +44 7700 900133; email "
                + "qa@example.org. The text is intentionally long for display testing.",
                90, INBOX, true, now)
        ));
    }

    static boolean isFixtureContactId(long id) {
        return id > CONTACT_BASE && id <= CONTACT_BASE + 1000L;
    }

    static boolean isFixtureMessageId(long id) {
        return id > MESSAGE_BASE && id <= MESSAGE_BASE + 1000L;
    }

    static boolean isFixtureThreadId(long id) {
        return id > THREAD_BASE && id <= THREAD_BASE + 1000L;
    }
}
