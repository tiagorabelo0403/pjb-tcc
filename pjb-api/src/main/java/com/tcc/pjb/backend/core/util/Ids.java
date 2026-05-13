package com.tcc.pjb.backend.core.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class Ids {

    private static final SecureRandom RNG = new SecureRandom();
    private static final int DEFAULT_TOKEN_BYTES = 16;

    private Ids() {
    }

    public static String opaqueId() {
        return nextOpaqueToken(DEFAULT_TOKEN_BYTES);
    }

    public static String opaqueId(String prefix) {
        String token = nextOpaqueToken(DEFAULT_TOKEN_BYTES);
        String normalizedPrefix = normalizePrefix(prefix);
        return normalizedPrefix.isEmpty() ? token : normalizedPrefix + "-" + token;
    }

    private static String nextOpaqueToken(int bytes) {
        byte[] buffer = new byte[bytes];
        RNG.nextBytes(buffer);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer);
    }

    private static String normalizePrefix(String prefix) {
        if (prefix == null) {
            return "";
        }
        String value = prefix.trim();
        if (value.isEmpty()) {
            return "";
        }
        return value.endsWith("-") ? value.substring(0, value.length() - 1) : value;
    }
}
