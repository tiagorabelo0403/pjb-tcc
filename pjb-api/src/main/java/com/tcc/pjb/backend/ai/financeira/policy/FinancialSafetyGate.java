package com.tcc.pjb.backend.ai.financeira.policy;

import java.util.regex.Pattern;

public final class FinancialSafetyGate {

    private FinancialSafetyGate() {}

    private static final Pattern GARANTIR = Pattern.compile("(?i)\\b(garantir|garantido|garantida|garante)\\b");
    private static final Pattern CERTEZA = Pattern.compile("(?i)\\bcerteza\\b");

    public static String apply(String text) {
        if (text == null || text.isBlank()) return text;
        String out = text;
        out = GARANTIR.matcher(out).replaceAll("estimar");
        out = CERTEZA.matcher(out).replaceAll("estimativa");
        out = out.replaceAll("\\s+\n", "\n").trim();
        return out;
    }
}
