package com.tcc.pjb.backend.core.util;

import java.text.Normalizer;
import java.util.Locale;

public final class EnumText {

    private EnumText() {
    }

    
    public static String normalizeToken(String raw) {
        if (raw == null) {
            return "";
        }

        String v = raw.trim();
        if (v.isEmpty()) {
            return "";
        }

        v = v.toUpperCase(Locale.ROOT);
        v = Normalizer.normalize(v, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        v = v.replace('-', '_').replace(' ', '_').replace('/', '_').replace('.', '_');
        v = v.replaceAll("[^A-Z0-9_]", "");
        v = v.replaceAll("_+", "_");
        v = v.replaceAll("^_+", "").replaceAll("_+$", "");
        return v;
    }
}
