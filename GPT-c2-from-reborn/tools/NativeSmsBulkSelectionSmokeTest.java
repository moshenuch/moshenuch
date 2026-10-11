package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

/** Pure-Java regression of Nokia in-conversation Mark / Mark all actions. */
public final class NativeSmsBulkSelectionSmokeTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static ArrayList<Long> ids(Long... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    private static ArrayList<Long> receivedInThread(
            ArrayList<NativeAuditFixtures.MessageRow> fixtures, long threadId) {
        ArrayList<Long> result = new ArrayList<>();
        for (NativeAuditFixtures.MessageRow message : fixtures)
            if (message.threadId == threadId &&
                    message.type == NativeAuditFixtures.INBOX)
                result.add(message.id);
        return result;
    }

    private static void same(ArrayList<Long> actual, ArrayList<Long> expected,
                             String description) {
        check(actual.equals(expected),
                description + "; expected=" + expected + ", actual=" + actual);
    }

    public static void main(String[] args) {
        ArrayList<NativeAuditFixtures.MessageRow> messages =
                NativeAuditFixtures.messages(1_780_000_000_000L);
        long tAaron = NativeAuditFixtures.THREAD_BASE + 1;
        long tLeah = NativeAuditFixtures.THREAD_BASE + 2;
        long latestAaron = NativeAuditFixtures.MESSAGE_BASE + 1;
        long sentAaron = NativeAuditFixtures.MESSAGE_BASE + 3;
        long olderAaron = NativeAuditFixtures.MESSAGE_BASE + 7;
        long latestLeah = NativeAuditFixtures.MESSAGE_BASE + 2;
        ArrayList<Long> aaronInbox = receivedInThread(messages, tAaron);
        ArrayList<Long> leahInbox = receivedInThread(messages, tLeah);
        same(aaronInbox, ids(latestAaron, olderAaron),
                "Aaron has two received messages, excluding sent reply");
        check(leahInbox.size() == 2, "Leah fixture has two incoming SMS");

        // No marked entries: only the highlighted incoming SMS is a target.
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, Collections.<Long>emptySet(), latestAaron),
                ids(latestAaron), "Unmarked single received target");
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, Collections.<Long>emptySet(), sentAaron),
                ids(), "Highlighted sent message must never be marked read/unread");

        // Mark + Mark all: targets the selected received messages, even when
        // the highlight is on a SENT row. Sent messages are skipped.
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(olderAaron)), sentAaron),
                ids(olderAaron),
                "Marked inbound SMS must beat highlighted sent message");
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(latestAaron, sentAaron, olderAaron)),
                sentAaron), ids(latestAaron, olderAaron),
                "Mark all must operate on two received SMS and ignore sent");
        System.out.println("PASS: Mark, Mark all and sent-row exclusion");

        // If the marked set contains ONLY sent or another thread, do not
        // silently fall back to the highlighted row: this was a subtle bug.
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(sentAaron)), latestAaron),
                ids(), "Sent-only marks must not fall back to highlighted inbound");
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(latestLeah)), latestAaron),
                ids(), "Stale marks from another conversation must not apply");
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(latestAaron, olderAaron)), latestAaron),
                ids(latestAaron, olderAaron), "Read/unread applies to all marked inbound rows");
        same(NativeSmsBulkSelection.targetReceivedIds(
                leahInbox, new HashSet<>(ids(latestAaron, latestLeah)), latestLeah),
                ids(latestLeah), "Cross-thread marks filtered by active conversation");
        System.out.println("PASS: selection isolation, no accidental fallbacks");

        same(NativeSmsBulkSelection.targetReceivedIds(
                Arrays.asList(latestAaron, latestAaron, olderAaron),
                new HashSet<>(ids(latestAaron)), sentAaron),
                ids(latestAaron), "Duplicate source IDs deduplicated");
        same(NativeSmsBulkSelection.targetReceivedIds(
                null, new HashSet<>(ids(latestAaron)), latestAaron),
                ids(), "Null conversation safe");
        same(NativeSmsBulkSelection.targetReceivedIds(
                ids(), new HashSet<>(ids(latestAaron)), latestAaron),
                ids(), "Empty conversation safe");
        same(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, null, olderAaron),
                ids(olderAaron), "Null marked set behaves as no marks");
        System.out.println("PASS: null, empty and duplicate safety");

        // The in-app read-state override should leave unrelated threads
        // untouched. This computes resulting unread state from the seeded
        // fixture and the TARGET IDs selected by production's helper.
        HashSet<Long> locallyRead = new HashSet<>(NativeSmsBulkSelection.targetReceivedIds(
                aaronInbox, new HashSet<>(ids(latestAaron, olderAaron, sentAaron)),
                sentAaron));
        int remainingUnread = 0;
        for (NativeAuditFixtures.MessageRow m : messages) {
            if (m.type == NativeAuditFixtures.INBOX && !m.read &&
                    !locallyRead.contains(m.id)) remainingUnread++;
        }
        check(remainingUnread == 2,
                "Mark Aaron's two received SMS read: two other demo unread SMS remain");
        System.out.println("PASS: mixed read/unread fixture regression, other threads stay unread");
        System.out.println("NativeSmsBulkSelection: all smoke tests passed");
    }
}
