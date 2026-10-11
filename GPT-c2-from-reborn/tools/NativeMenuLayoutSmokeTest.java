package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.Arrays;

public final class NativeMenuLayoutSmokeTest {
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static ArrayList<Integer> order(int... entries) {
        ArrayList<Integer> out = new ArrayList<>();
        for (int i : entries) out.add(i);
        return out;
    }

    public static void main(String[] args) {
        ArrayList<Integer> original = NativeMenuLayout.defaultOrder();
        check(original.size() == 9, "Nokia menu has nine entries");
        check(NativeMenuLayout.destination(original, 0) == 0, "Contacts default destination");
        check(NativeMenuLayout.destination(original, 4) == 4, "Messaging default destination");

        check(NativeMenuLayout.GRID_WITH_LABELS.equals(
                NativeMenuLayout.validatedMode("unsupported")), "Unknown menu mode fallback");
        check(NativeMenuLayout.modeIndex(NativeMenuLayout.LIST) == 0, "List radio position");
        check(NativeMenuLayout.modeIndex(NativeMenuLayout.GRID) == 1, "Grid radio position");
        check(NativeMenuLayout.modeIndex(NativeMenuLayout.GRID_WITH_LABELS) == 2,
                "Labeled grid radio position");
        check(NativeMenuLayout.modeIndex(NativeMenuLayout.TAB) == 3, "Tab radio position");

        check(NativeMenuLayout.navigate(0, "LEFT", NativeMenuLayout.GRID) == 2,
                "Grid left wraps within row");
        check(NativeMenuLayout.navigate(2, "RIGHT", NativeMenuLayout.GRID) == 0,
                "Grid right wraps within row");
        check(NativeMenuLayout.navigate(1, "UP", NativeMenuLayout.GRID) == 7,
                "Grid up wraps rows");
        check(NativeMenuLayout.navigate(7, "DOWN", NativeMenuLayout.GRID) == 1,
                "Grid down wraps rows");
        check(NativeMenuLayout.navigate(8, "DOWN", NativeMenuLayout.LIST) == 0,
                "List down wraps sequentially");
        check(NativeMenuLayout.navigate(0, "LEFT", NativeMenuLayout.TAB) == 8,
                "Tab left wraps sequentially");
        check(NativeMenuLayout.navigate(2, "RIGHT", NativeMenuLayout.TAB) == 3,
                "Tab right moves to next");

        ArrayList<Integer> moved = NativeMenuLayout.defaultOrder();
        check(NativeMenuLayout.move(moved, 0, 4), "Move Contacts icon");
        check(moved.equals(order(1,2,3,4,0,5,6,7,8)), "Move must shift, not overwrite");
        check(NativeMenuLayout.destination(moved, 4) == 0,
                "Moved icon must still open Contacts");
        check(NativeMenuLayout.destination(moved, 3) == 4,
                "Shifted icon must still open Messaging");
        check(!NativeMenuLayout.move(moved, -1, 0), "Invalid source is rejected");
        check(!NativeMenuLayout.move(moved, 1, 9), "Invalid destination rejected");
        check(!NativeMenuLayout.move(moved, 4, 4), "Identical destination is no-op");

        check(NativeMenuLayout.validatedOrder(moved).equals(moved),
                "A valid saved order must be restored");
        ArrayList<Integer> corrupt = order(1,1,2,3,4,5,6,7,8);
        check(NativeMenuLayout.validatedOrder(corrupt).equals(original),
                "Duplicate saved destinations reset to default");
        check(NativeMenuLayout.validatedOrder(order(0,1)).equals(original),
                "Truncated saved order resets to default");
        check(NativeMenuLayout.validatedOrder(null).equals(original),
                "Missing saved order resets to default");
        check(NativeMenuLayout.validatedOrder(order(1,2,3,4,5,6,7,8,9)).equals(original),
                "Out-of-range saved destination resets to default");
        check(NativeMenuLayout.destination(moved, -1) == -1,
                "Invalid selected visual slot must not route");
        System.out.println("NativeMenuLayout: all smoke tests passed");
    }
}
