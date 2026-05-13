package com.tcc.pjb.backend.ai.juridica.policy;

import java.util.Locale;
import java.util.regex.Pattern;

public final class LegalSafetyGate {

    private LegalSafetyGate() {}

    private static final Pattern GARANTIA = Pattern.compile("(?i)\\b(garantido|garantida|garante|garantir)\\b");
    private static final Pattern CERTEZA = Pattern.compile("(?i)\\bcerteza\\b");
    private static final Pattern SEMPRE = Pattern.compile("(?i)\\bsempre\\b");
    private static final Pattern NUNCA = Pattern.compile("(?i)\\bnunca\\b");

    public static String apply(String text) {
        if (text == null || text.isBlank()) return text;
        String out = text;

        out = GARANTIA.matcher(out).replaceAll("indica");
        out = CERTEZA.matcher(out).replaceAll("probabilidade");
        out = SEMPRE.matcher(out).replaceAll("em regra");
        out = NUNCA.matcher(out).replaceAll("em geral não");

        
        out = out.replaceAll("\\s+\n", "\n").trim();
        if (out.length() > 8 && out.equals(out.toUpperCase(Locale.ROOT))) {
            out = out.substring(0, 1).toUpperCase(Locale.ROOT) + out.substring(1).toLowerCase(Locale.ROOT);
        }
        return out;
    }
}
