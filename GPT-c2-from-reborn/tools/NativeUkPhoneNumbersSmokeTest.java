package com.moshenuch.c2reborn;

/** Pure Java smoke/regression tests; run before Gradle Android compilation. */
public final class NativeUkPhoneNumbersSmokeTest {
    private static void check(boolean ok, String detail) {
        if (!ok) throw new AssertionError(detail);
    }

    public static void main(String[] args) {
        String trunk = "07700900123";
        // All four dialling forms of one UK subscriber number are equal.
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("07700900123")),
                "trunk 0 form is canonical");
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("+447700900123")),
                "+44 form must equal trunk 0");
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("00447700900123")),
                "0044 form must equal trunk 0");
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("447700900123")),
                "bare 44 form must equal trunk 0");
        // Formatting characters never change the comparison.
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("+44 7700 900 123")),
                "spaced +44 form must equal trunk 0");
        check(trunk.equals(UkPhoneNumbers.ukNormalisedDigits("07700 900123")),
                "spaced trunk form must equal trunk 0");
        // Different subscriber numbers stay different.
        check(!trunk.equals(UkPhoneNumbers.ukNormalisedDigits("07700900124")),
                "different trunk number must not match");
        check(!trunk.equals(UkPhoneNumbers.ukNormalisedDigits("+447700900124")),
                "different +44 number must not match");
        // Non-UK-length numbers are compared as dialled.
        check("+12225550101".replace("+", "").equals(
                UkPhoneNumbers.ukNormalisedDigits("+1 222 555 0101")),
                "non-UK number keeps its digits untouched");
        check("0044770090012".equals(UkPhoneNumbers.ukNormalisedDigits("0044770090012")),
                "truncated 0044 number is not normalised");
        check("".equals(UkPhoneNumbers.ukNormalisedDigits(null)),
                "null is safe");
        check("2".equals(UkPhoneNumbers.ukNormalisedDigits("C2ALERTS")),
                "alphanumeric sender keeps only its digits");
        System.out.println("NativeUkPhoneNumbers: all smoke tests passed");
    }
}
