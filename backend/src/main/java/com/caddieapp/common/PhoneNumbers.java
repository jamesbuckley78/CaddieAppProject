package com.caddieapp.common;

/** Phone numbers are the login name, so "087 123 4567" and "0871234567" must match. */
public final class PhoneNumbers {

    /** What a phone number may look like when typed in (checked with @Pattern on requests). */
    public static final String PATTERN = "^\\+?[0-9 ()\\-.]{7,25}$";

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
