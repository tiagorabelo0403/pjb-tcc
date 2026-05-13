package com.tcc.pjb.backend.platform.security.ratelimit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import com.tcc.pjb.backend.platform.security.rbac.CapabilityStrings;

public final class RateLimitKeys {

    private RateLimitKeys() {}

    public static String buildKey(String prefix,
                                  CapabilityRateLimitDomain domain,
                                  String capabilityRaw,
                                  String versionRaw,
                                  String subjectRaw) {
        String p = (prefix == null || prefix.isBlank()) ? "pjb:caprl" : prefix.trim();
        String d = (domain != null) ? domain.canonical() : "unknown";
        String cap = CapabilityStrings.canonical(capabilityRaw);
        if (cap == null) cap = "UNKNOWN";
        String v = (versionRaw == null || versionRaw.isBlank()) ? "v?" : versionRaw.trim().toLowerCase(Locale.ROOT);
        String subjectHash = sha256HexTrunc(subjectRaw, 24);
        
        return p + ":" + d + ":" + cap + ":" + v + ":" + subjectHash;
    }

    public static String sha256HexTrunc(String raw, int hexLen) {
        String s = (raw == null) ? "anonymous" : raw.trim();
        if (s.isBlank()) s = "anonymous";
        byte[] dig = sha256(s.getBytes(StandardCharsets.UTF_8));
        String hex = toHex(dig);
        int n = Math.min(Math.max(8, hexLen), hex.length());
        return hex.substring(0, n).toLowerCase(Locale.ROOT);
    }

    private static byte[] sha256(byte[] in) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(in);
        } catch (NoSuchAlgorithmException e) {
            
            throw new IllegalStateException(e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
