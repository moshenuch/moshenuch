package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Nokia S40-style main-menu selection and positioning, independent of
 * Android rendering so that navigation and rearranging can be regression-tested.
 */
final class NativeMenuLayout {
    static final String LIST = "List";
    static final String GRID = "Grid";
    static final String GRID_WITH_LABELS = "Grid with labels";
    static final String TAB = "Tab";
    static final String[] MODES = {LIST, GRID, GRID_WITH_LABELS, TAB};
    static final int COUNT = 9;

    private NativeMenuLayout() {}

    static String validatedMode(String proposed) {
        for (String mode : MODES)
            if (mode.equals(proposed)) return mode;
        return GRID_WITH_LABELS;
    }

    static int modeIndex(String proposed) {
        String valid = validatedMode(proposed);
        for (int i = 0; i < MODES.length; i++)
            if (valid.equals(MODES[i])) return i;
        return 2;
    }

    static boolean isGrid(String mode) {
        return GRID.equals(mode) || GRID_WITH_LABELS.equals(mode);
    }

    static ArrayList<Integer> defaultOrder() {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0; i < COUNT; i++) result.add(i);
        return result;
    }

    static ArrayList<Integer> validatedOrder(ArrayList<Integer> proposed) {
        if (proposed == null || proposed.size() != COUNT) return defaultOrder();
        HashSet<Integer> unique = new HashSet<>();
        for (Integer n : proposed)
            if (n == null || n < 0 || n >= COUNT || !unique.add(n))
                return defaultOrder();
        return new ArrayList<>(proposed);
    }

    /**
     * Selected position is in the VISUAL ordering, not a stable destination ID.
     * List and Tab modes browse sequentially; grids move between three columns.
     */
    static int navigate(int selected, String direction, String mode) {
        if (selected < 0 || selected >= COUNT) selected = 0;
        if (!isGrid(mode)) {
            if ("UP".equals(direction) || "LEFT".equals(direction))
                return (selected + COUNT - 1) % COUNT;
            if ("DOWN".equals(direction) || "RIGHT".equals(direction))
                return (selected + 1) % COUNT;
            return selected;
        }
        switch (direction) {
            case "UP": return (selected + COUNT - 3) % COUNT;
            case "DOWN": return (selected + 3) % COUNT;
            case "LEFT": return selected % 3 == 0 ? selected + 2 : selected - 1;
            case "RIGHT": return selected % 3 == 2 ? selected - 2 : selected + 1;
            default: return selected;
        }
    }

    /** Moves an entry, shifting the elements between source and destination. */
    static boolean move(ArrayList<Integer> order, int from, int to) {
        if (order == null || order.size() != COUNT ||
                from < 0 || from >= COUNT || to < 0 || to >= COUNT || from == to)
            return false;
        Integer selectedItem = order.remove(from);
        order.add(to, selectedItem);
        return true;
    }

    static int destination(ArrayList<Integer> order, int selected) {
        if (order == null || order.size() != COUNT || selected < 0 || selected >= COUNT)
            return -1;
        return order.get(selected);
    }
}
