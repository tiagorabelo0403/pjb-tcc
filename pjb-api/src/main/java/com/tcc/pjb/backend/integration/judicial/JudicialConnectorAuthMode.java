package com.tcc.pjb.backend.integration.judicial;

public enum JudicialConnectorAuthMode {
    NONE,
    BEARER,
    API_KEY,
    BASIC,
    OAUTH2_CLIENT_CREDENTIALS,
    REQUEST_BEARER,
    REQUEST_BASIC,
    REQUEST_API_KEY,
    REQUEST_CUSTOM,
    REQUEST_HEADERS,
    MISSING;

    public boolean satisfies(boolean authRequired) {
        return !authRequired || this != MISSING;
    }

    public static JudicialConnectorAuthMode from(Object value) {
        if (value instanceof JudicialConnectorAuthMode mode) {
            return mode;
        }
        if (value == null) {
            return NONE;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return NONE;
        }
        try {
            return JudicialConnectorAuthMode.valueOf(text.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return NONE;
        }
    }
}
