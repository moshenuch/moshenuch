package com.moshenuch.c2reborn;

import java.util.ArrayList;

/**
 * Pure Java, width-aware word wrapping for the native Nokia LCD notice.
 * The Canvas provides its actual Paint measurement; this class never
 * guesses character widths or uses font-dependent fixed-width substrings.
 */
final class NativeNoticeText {
    interface WidthMeasurer {
        float width(String text);
    }

    private NativeNoticeText() { }

    static ArrayList<String> wrap(String notice, float width, int maxLines,
                                   WidthMeasurer measurer) {
        ArrayList<String> result = new ArrayList<>();
        if (notice == null || notice.trim().isEmpty() ||
                width <= 0f || maxLines <= 0 || measurer == null)
            return result;
        String[] words = notice.trim().split("\\s+");
        String line = "";
        for (String word : words) {
            if (word.isEmpty()) continue;
            String proposed = line.isEmpty() ? word : line + " " + word;
            if (measurer.width(proposed) <= width) {
                line = proposed;
                continue;
            }
            if (!line.isEmpty()) {
                result.add(line);
                line = "";
            }
            if (measurer.width(word) <= width) {
                line = word;
            } else {
                // A long unbroken token must not escape the notice box.
                result.add(fit(word, width, measurer, true));
            }
        }
        if (!line.isEmpty()) result.add(line);
        if (result.size() > maxLines) {
            while (result.size() > maxLines) result.remove(result.size() - 1);
            int last = maxLines - 1;
            result.set(last, fit(result.get(last), width, measurer, true));
        }
        return result;
    }

    private static String fit(String value, float width, WidthMeasurer measurer,
                              boolean addEllipsis) {
        String suffix = addEllipsis ? "…" : "";
        if (measurer.width(suffix) > width) return "";
        String candidate = value;
        while (!candidate.isEmpty() &&
                measurer.width(candidate + suffix) > width)
            candidate = candidate.substring(0, candidate.length() - 1);
        return candidate + suffix;
    }
}
