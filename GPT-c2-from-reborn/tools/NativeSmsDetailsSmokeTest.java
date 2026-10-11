package com.moshenuch.c2reborn;

import java.util.ArrayList;

/** Pure Java smoke/regression tests; run before Gradle Android compilation. */
public final class NativeSmsDetailsSmokeTest {
    private static void check(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }

    private static int count(ArrayList<NativeSmsDetails.Entry> entries, String kind) {
        int n = 0;
        for (NativeSmsDetails.Entry entry : entries)
            if (kind.equals(entry.kind)) n++;
        return n;
    }

    public static void main(String[] args) {
        ArrayList<NativeSmsDetails.Entry> rich = NativeSmsDetails.extract(
                "+447700900123",
                "Call +44 7700 900123, email hello@example.com. "
                    + "Visit https://example.org/path/12345678.");
        check(count(rich, NativeSmsDetails.Entry.PHONE) == 1,
                "duplicate sender numbers or numeric URL must not add a phone");
        check(count(rich, NativeSmsDetails.Entry.EMAIL) == 1,
                "must recognize email");
        check(count(rich, NativeSmsDetails.Entry.WEB) == 1,
                "must recognize HTTPS web URL");
        check(rich.size() == 3, "unexpected extracted detail count: " + rich.size());

        ArrayList<NativeSmsDetails.Entry> blanks = NativeSmsDetails.extract(
                "Private sender", "Your verification code is 123456. Meeting 2026-10-11.");
        check(blanks.isEmpty(), "short codes and ISO dates are not phone numbers");

        ArrayList<NativeSmsDetails.Entry> numbers = NativeSmsDetails.extract(
                "", "Call 020 7946 1234 or +44 (0) 7700 900123");
        check(count(numbers, NativeSmsDetails.Entry.PHONE) == 2,
                "must parse two distinct phone numbers");

        ArrayList<NativeSmsDetails.Entry> web = NativeSmsDetails.extract(
                null, "Visit www.example.co.uk/help! and write to info@example.co.uk");
        check(count(web, NativeSmsDetails.Entry.WEB) == 1,
                "must detect www. link");
        check(count(web, NativeSmsDetails.Entry.EMAIL) == 1,
                "must detect email while excluding its domain from link scan");
        for (NativeSmsDetails.Entry entry : web)
            check(!entry.value.endsWith("!"), "trailing punctuation must be stripped");

        check(NativeSmsDetails.extract("", null).isEmpty(), "null body must be safe");
        System.out.println("NativeSmsDetails: all smoke tests passed");
    }
}
