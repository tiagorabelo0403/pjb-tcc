package com.tcc.pjb.backend.platform.security.rbac;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public final class CapabilityStrings {

    private static final Pattern VERSION_SUFFIX = Pattern.compile("(?i)[_:.-]V[123]$");
    private static final Pattern INVALID = Pattern.compile("[^A-Z0-9_:.\\-/]");

    private CapabilityStrings() {}

    public static String canonical(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        
        s = s.replace(' ', '_');
        s = s.toUpperCase(Locale.ROOT);

        
        s = VERSION_SUFFIX.matcher(s).replaceAll("");

        
        s = INVALID.matcher(s).replaceAll("_");
        s = collapseUnderscores(s);
        return s.isBlank() ? null : s;
    }

    public static boolean containsToken(String canonicalCapability, String tokenUpper) {
        Objects.requireNonNull(tokenUpper, "tokenUpper");
        if (canonicalCapability == null) return false;
        return canonicalCapability.contains(tokenUpper);
    }

    private static String collapseUnderscores(String s) {
        String out = s;
        while (out.contains("__")) {
            out = out.replace("__", "_");
        }
        if (out.startsWith("_")) out = out.substring(1);
        if (out.endsWith("_")) out = out.substring(0, out.length() - 1);
        return out;
    }
}
