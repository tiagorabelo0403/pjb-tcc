package com.tcc.pjb.backend.platform.logging;

import java.security.SecureRandom;
import java.util.Locale;

public final class TraceIds {

    private TraceIds() {}

    private static final SecureRandom RNG = new SecureRandom();
    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static String newTraceId() {
        return randomHex(16); 
    }

    public static String newSpanId() {
        return randomHex(8);  
    }

    public static String normalize(String raw) {
        if (raw == null) return newTraceId();
        String s = raw.trim();
        if (s.isEmpty()) return newTraceId();
        s = s.replaceAll("[^a-zA-Z0-9_.:-]", "_");
        if (s.length() > 64) s = s.substring(0, 64);
        return s;
    }

    private static String randomHex(int bytes) {
        byte[] b = new byte[bytes];
        RNG.nextBytes(b);
        char[] out = new char[bytes * 2];
        for (int i = 0; i < b.length; i++) {
            int v = b[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out).toLowerCase(Locale.ROOT);
    }
}
