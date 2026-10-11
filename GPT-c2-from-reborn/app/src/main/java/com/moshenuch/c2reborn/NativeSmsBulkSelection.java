package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 * Selects which *received* SMS to mark read/unread within one open thread.
 * The Android provider is never mutated. This is a pure-Java utility shared
 * by the native Nokia conversation Options and the CI regression tests.
 */
final class NativeSmsBulkSelection {
    private NativeSmsBulkSelection() { }

    /**
     * @param threadIncomingIds the active conversation's received-message IDs,
     *                          newest first. Sent/outgoing SMS must be omitted.
     * @param markedIds all messages explicitly marked in the conversation;
     *                  may include sent entries.
     * @param highlightedId selected message ID, used ONLY when nothing is marked.
     * @return selected inbound messages, with stable conversation display order.
     *
     * When any messages are marked, an empty return is intentional if those
     * marks refer only to sent messages. Never silently apply the action to
     * the highlighted inbox row in that situation.
     */
    static ArrayList<Long> targetReceivedIds(Collection<Long> threadIncomingIds,
                                              Collection<Long> markedIds,
                                              long highlightedId) {
        ArrayList<Long> selected = new ArrayList<>();
        if (threadIncomingIds == null || threadIncomingIds.isEmpty()) return selected;
        boolean bulkMode = markedIds != null && !markedIds.isEmpty();
        HashSet<Long> marked = bulkMode ? new HashSet<>(markedIds) : new HashSet<Long>();
        HashSet<Long> seen = new HashSet<>();
        for (Long id : threadIncomingIds) {
            if (id == null || !seen.add(id)) continue;
            if (bulkMode ? marked.contains(id) : id == highlightedId)
                selected.add(id);
        }
        return selected;
    }
}
