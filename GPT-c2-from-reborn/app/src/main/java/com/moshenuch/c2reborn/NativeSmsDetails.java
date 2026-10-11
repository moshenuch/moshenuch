package com.moshenuch.c2reborn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Native, read-only Nokia SMS "Use detail" parser. No provider writes,
 * URI launches, calls or outbound messages occur while parsing an SMS.
 */
final class NativeSmsDetails {
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern WEB = Pattern.compile(
            "(?i)(?:https?://|www\\.)[^\\s<>\\\"']+");
    private static final Pattern PHONE = Pattern.compile(
            "(?<![A-Za-z0-9@])(?:\\+|00)?\\d[\\d() .-]{5,}\\d(?![A-Za-z0-9])");

    static final class Entry {
        static final String PHONE = "Phone";
        static final String EMAIL = "E-mail";
        static final String WEB = "Web";
        final String kind;
        final String value;

        Entry(String kind, String value) {
            this.kind = kind;
            this.value = value;
        }

        String label() {
            return kind + ": " + value;
        }
    }

    private NativeSmsDetails() {}

    static ArrayList<Entry> extract(String sender, String body) {
        ArrayList<Entry> details = new ArrayList<>();
        HashSet<String> seen = new HashSet<>();
        String text = body == null ? "" : body;
        ArrayList<int[]> blocked = new ArrayList<>();

        // A sender number is useful even if its digits are not in the body.
        add(details, seen, Entry.PHONE, sender);

        Matcher emails = EMAIL.matcher(text);
        while (emails.find() && details.size() < 25) {
            blocked.add(new int[]{emails.start(), emails.end()});
            add(details, seen, Entry.EMAIL, emails.group());
        }

        Matcher urls = WEB.matcher(text);
        while (urls.find() && details.size() < 25) {
            if (overlaps(urls.start(), urls.end(), blocked)) continue;
            blocked.add(new int[]{urls.start(), urls.end()});
            add(details, seen, Entry.WEB, trimPunctuation(urls.group()));
        }

        Matcher phones = PHONE.matcher(text);
        while (phones.find() && details.size() < 25) {
            if (overlaps(phones.start(), phones.end(), blocked)) continue;
            add(details, seen, Entry.PHONE, trimPunctuation(phones.group().trim()));
        }
        return details;
    }

    private static boolean overlaps(int start, int end, ArrayList<int[]> spans) {
        for (int[] span : spans)
            if (start < span[1] && end > span[0]) return true;
        return false;
    }

    private static String trimPunctuation(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (!value.isEmpty() && ".,;!?:)]}".indexOf(value.charAt(value.length() - 1)) >= 0)
            value = value.substring(0, value.length() - 1);
        return value;
    }

    private static String digits(String raw) {
        if (raw == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < raw.length(); i++)
            if (raw.charAt(i) >= '0' && raw.charAt(i) <= '9')
                b.append(raw.charAt(i));
        return b.toString();
    }

    private static void add(ArrayList<Entry> out, HashSet<String> seen,
                            String kind, String raw) {
        if (raw == null || raw.trim().isEmpty() || out.size() >= 25) return;
        String value = trimPunctuation(raw);
        String key;
        if (Entry.PHONE.equals(kind)) {
            String phoneDigits = digits(value);
            if (phoneDigits.length() < 7 || phoneDigits.length() > 15) return;
            key = "p:" + (phoneDigits.startsWith("00") ? phoneDigits.substring(2) : phoneDigits);
        } else {
            if (Entry.WEB.equals(kind) && value.length() > 250) return;
            key = kind + ":" + value.toLowerCase(Locale.ROOT);
        }
        if (seen.add(key)) out.add(new Entry(kind, value));
    }
}
