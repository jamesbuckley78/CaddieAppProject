package com.caddieapp.common;

/** Phone numbers are the login name, so "087 123 4567" and "0871234567" must match. */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /** Keeps digits and a leading '+'; drops spaces, dashes, brackets and dots. */
    public static String normalise(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("[^0-9]", "");
        return trimmed.startsWith("+") ? "+" + digits : digits;
    }
}
