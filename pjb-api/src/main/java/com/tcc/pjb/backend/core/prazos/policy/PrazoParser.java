package com.tcc.pjb.backend.core.prazos.policy;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.prazos.PrazoRegime;











@Service
public class PrazoParser {

    
    private static final Pattern P1 = Pattern.compile(
            "(?i)(?:prazo\\s*(?:de)?\\s*)?(\\d{1,3})\\s*(?:dia|dias)\\s*(uteis|úteis|corridos|corrido)?");

    
    private static final Pattern P2 = Pattern.compile(
            "(?i)(?:prazo\\s*(?:de)?\\s*)?([a-zçãáéíóú]+)\\s*(?:dia|dias)\\s*(uteis|úteis|corridos|corrido)?");

    private static final Map<String, Integer> PT_NUM = Map.ofEntries(
            Map.entry("um", 1), Map.entry("uma", 1),
            Map.entry("dois", 2), Map.entry("duas", 2),
            Map.entry("tres", 3), Map.entry("três", 3),
            Map.entry("quatro", 4),
            Map.entry("cinco", 5),
            Map.entry("seis", 6),
            Map.entry("sete", 7),
            Map.entry("oito", 8),
            Map.entry("nove", 9),
            Map.entry("dez", 10),
            Map.entry("onze", 11),
            Map.entry("doze", 12),
            Map.entry("treze", 13),
            Map.entry("quatorze", 14), Map.entry("catorze", 14),
            Map.entry("quinze", 15),
            Map.entry("dezesseis", 16), Map.entry("dezessete", 17), Map.entry("dezoito", 18), Map.entry("dezenove", 19),
            Map.entry("vinte", 20),
            Map.entry("trinta", 30)
    );

    public PrazoParseResult parse(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String t = texto.trim();

        Matcher m = P1.matcher(t);
        if (m.find()) {
            Integer dias = safeInt(m.group(1));
            PrazoRegime regime = parseRegime(m.group(2));
            String snippet = safeSnippet(t, m.start(), m.end());
            if (dias != null && dias > 0) {
                return new PrazoParseResult(dias, regime, snippet);
            }
        }

        
        Matcher m2 = P2.matcher(t);
        if (m2.find()) {
            String token = normalize(m2.group(1));
            Integer dias = PT_NUM.get(token);
            PrazoRegime regime = parseRegime(m2.group(2));
            String snippet = safeSnippet(t, m2.start(), m2.end());
            if (dias != null && dias > 0) {
                return new PrazoParseResult(dias, regime, snippet);
            }
        }

        return null;
    }

    private static Integer safeInt(String s) {
        try {
            if (s == null) return null;
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static PrazoRegime parseRegime(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        if (v.contains("corr")) return PrazoRegime.CORRIDOS;
        if (v.contains("ute") || v.contains("úte")) return PrazoRegime.UTEIS;
        return null;
    }

    private static String safeSnippet(String full, int start, int end) {
        int s = Math.max(0, start);
        int e = Math.min(full.length(), end);
        String sn = full.substring(s, e).trim();
        return sn.length() <= 80 ? sn : sn.substring(0, 77) + "…";
    }

    private static String normalize(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
