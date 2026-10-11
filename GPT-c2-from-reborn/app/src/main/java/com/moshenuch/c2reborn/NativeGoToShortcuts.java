package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

/**
 * App-local Nokia "Go to" shortcut layout. Each ID is a stable destination:
 * display position and whether an entry is shown can change independently.
 */
final class NativeGoToShortcuts {
    static final String[] NAMES = {
            "Lock keypad", "Profiles", "Alarm clock", "Camera", "Video recorder",
            "Calculator", "Nokia Browser", "Media player", "Conversations"
    };
    static final int COUNT = NAMES.length;

    private final ArrayList<Integer> order = new ArrayList<>();
    private final boolean[] enabled = new boolean[COUNT];

    NativeGoToShortcuts() {
        restoreDefaults();
    }

    void restoreDefaults() {
        order.clear();
        for (int id = 0; id < COUNT; id++) order.add(id);
        Arrays.fill(enabled, true);
    }

    static boolean isValidOrder(ArrayList<Integer> candidate) {
        if (candidate == null || candidate.size() != COUNT) return false;
        HashSet<Integer> unique = new HashSet<>();
        for (Integer id : candidate)
            if (id == null || id < 0 || id >= COUNT || !unique.add(id))
                return false;
        return true;
    }

    /**
     * Bad or empty saved sets are rejected rather than leaving Go to with
     * zero working shortcuts, inaccessible Options or arbitrary destinations.
     */
    boolean restore(ArrayList<Integer> savedOrder, ArrayList<Integer> selectedIds) {
        if (!isValidOrder(savedOrder) || selectedIds == null ||
                selectedIds.isEmpty() || selectedIds.size() > COUNT) {
            restoreDefaults();
            return false;
        }
        boolean[] selected = new boolean[COUNT];
        for (Integer id : selectedIds) {
            if (id == null || id < 0 || id >= COUNT || selected[id]) {
                restoreDefaults();
                return false;
            }
            selected[id] = true;
        }
        order.clear();
        order.addAll(savedOrder);
        System.arraycopy(selected, 0, enabled, 0, COUNT);
        return true;
    }

    /**
     * These JSON integer arrays are the exact same strings stored in Android
     * SharedPreferences by C2PhoneView. The pure-Java CI test round-trips the
     * same format, including app restart simulated by a new instance.
     */
    String serializedOrder() { return intArray(order); }
    String serializedEnabled() { return intArray(enabledIds()); }

    boolean restoreSerialized(String savedOrder, String savedEnabled) {
        return restore(parseIntArray(savedOrder), parseIntArray(savedEnabled));
    }

    private static String intArray(ArrayList<Integer> values) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(values.get(i));
        }
        return sb.append(']').toString();
    }

    /** Strict parsing of the old and current JSON-array SharedPreferences. */
    private static ArrayList<Integer> parseIntArray(String value) {
        if (value == null) return null;
        String json = value.trim();
        if (json.length() < 2 || json.charAt(0) != '[' ||
                json.charAt(json.length() - 1) != ']') return null;
        String middle = json.substring(1, json.length() - 1).trim();
        ArrayList<Integer> decoded = new ArrayList<>();
        if (middle.isEmpty()) return decoded;
        String[] tokens = middle.split(",", -1);
        if (tokens.length > COUNT) return null;
        for (String token : tokens) {
            String trimmed = token.trim();
            if (!trimmed.matches("-?[0-9]+")) return null;
            try { decoded.add(Integer.parseInt(trimmed)); }
            catch (NumberFormatException e) { return null; }
        }
        return decoded;
    }

    ArrayList<Integer> order() {
        return new ArrayList<>(order);
    }

    ArrayList<Integer> enabledIds() {
        ArrayList<Integer> ids = new ArrayList<>();
        for (int id : order) if (enabled[id]) ids.add(id);
        return ids;
    }

    int visualId(int position) {
        return position >= 0 && position < COUNT ? order.get(position) : -1;
    }

    int visibleId(int position) {
        ArrayList<Integer> ids = enabledIds();
        return position >= 0 && position < ids.size() ? ids.get(position) : -1;
    }

    boolean isEnabled(int id) {
        return id >= 0 && id < COUNT && enabled[id];
    }

    int enabledCount() {
        int n = 0;
        for (boolean on : enabled) if (on) n++;
        return n;
    }

    /**
     * Do not allow the last shortcut to be disabled, since the user otherwise
     * cannot browse the list using normal Nokia navigation.
     */
    boolean setEnabled(int id, boolean show) {
        if (id < 0 || id >= COUNT) return false;
        if (enabled[id] == show) return false;
        if (!show && enabledCount() == 1) return false;
        enabled[id] = show;
        return true;
    }

    boolean toggleAt(int visualIndex) {
        int id = visualId(visualIndex);
        return id >= 0 && setEnabled(id, !isEnabled(id));
    }

    boolean move(int fromIndex, int delta) {
        int target = fromIndex + delta;
        if (fromIndex < 0 || fromIndex >= COUNT || target < 0 || target >= COUNT)
            return false;
        Integer moving = order.remove(fromIndex);
        order.add(target, moving);
        return true;
    }
}
