package com.caimanproject.contracts.util;

public final class LogMask {

    private LogMask() {}

    /**
     * Masks an email address for safe logging: "mateus@gmail.com" → "ma***@gmail.com".
     * Shows the first 2 characters of the local part, then "***", then the full domain.
     */
    public static String email(final String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }

        final int atIdx = email.indexOf('@');
        final String local = email.substring(0, atIdx);
        final String domain = email.substring(atIdx);
        final String maskedLocal = local.length() <= 2 ? "*".repeat(local.length()) : local.substring(0, 2) + "***";
        return maskedLocal + domain;
    }
}
