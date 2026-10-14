package com.moshenuch.c2reborn;

/**
 * Pure UK phone-number helpers, kept Android-free so the CI smoke test can
 * compile and run them directly. Moti's handset is UK: for duplicate checks
 * a leading +44 / 0044 / 44 international form and the trunk 0 form are the
 * same subscriber number.
 */
public final class UkPhoneNumbers {
    private UkPhoneNumbers() {}

    public static String digitsOnly(String value) {
        if (value == null) return "";
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isDigit(ch)) b.append(ch);
        }
        return b.toString();
    }

    /** Canonical duplicate-check key: UK numbers normalise to trunk-0 form. */
    public static String ukNormalisedDigits(String number) {
        String digits = digitsOnly(number);
        if (digits.length() == 14 && digits.startsWith("0044"))
            return "0" + digits.substring(4);
        if (digits.length() == 12 && digits.startsWith("44"))
            return "0" + digits.substring(2);
        return digits;
    }
}
