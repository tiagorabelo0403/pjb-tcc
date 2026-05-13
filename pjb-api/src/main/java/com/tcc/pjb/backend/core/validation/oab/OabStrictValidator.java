package com.tcc.pjb.backend.core.validation.oab;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import java.util.Locale;

@Component
public class OabStrictValidator {

    private static final Set<String> UFS = Set.of(
            "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI",
            "RJ","RN","RS","RO","RR","SC","SP","SE","TO"
    );

    private static final Pattern P1 = Pattern.compile("^(?<uf>[A-Z]{2})-?(?<num>\\d{1,9})(?:-?(?<suf>[A-Z]{1,5}))?$");
    private static final Pattern P2 = Pattern.compile("^(?<num>\\d{1,9})-?(?<uf>[A-Z]{2})(?:-?(?<suf>[A-Z]{1,5}))?$");

    public OabInfo parseAndValidate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("OAB inválida: valor vazio.");
        }

        String cleaned = normalizeInput(raw);

        Matcher m = P1.matcher(cleaned);
        if (!m.matches()) {
            m = P2.matcher(cleaned);
        }
        if (!m.matches()) {
            throw new IllegalArgumentException("OAB inválida: formato não reconhecido.");
        }

        String uf = m.group("uf");
        String num = m.group("num");
        String suf = safeUpper(m.group("suf"));

        if (uf == null || uf.length() != 2 || !UFS.contains(uf)) {
            throw new IllegalArgumentException("OAB inválida: UF inexistente/ inválida: " + (uf == null ? "" : uf));
        }

        if (num == null || num.isBlank()) {
            throw new IllegalArgumentException("OAB inválida: número ausente.");
        }

        String numero = stripLeadingZeros(num);
        if (numero.isBlank()) {
            throw new IllegalArgumentException("OAB inválida: número inválido.");
        }

        
        if (suf != null && !suf.isBlank()) {
            if (!suf.matches("[A-Z]{1,5}")) {
                throw new IllegalArgumentException("OAB inválida: sufixo inválido.");
            }
        } else {
            suf = null;
        }

        String normalized = uf + "-" + numero + (suf != null ? "-" + suf : "");
        String display = "OAB/" + uf + " " + numero + (suf != null ? "-" + suf : "");
        return new OabInfo(display, normalized, uf, numero, suf);
    }

    private static String normalizeInput(String raw) {
        String v = raw.trim().toUpperCase(Locale.ROOT);
        v = v.replace(".", "");

        
        v = v.replaceAll("^OAB[/\\-\\s]*", "");

        
        v = v.replace('/', '-');
        v = v.replaceAll("\\s+", "");
        v = v.replaceAll("--+", "-");

        
        v = v.replaceAll("[^A-Z0-9-]", "");

        
        v = v.replaceAll("-+", "-");
        v = v.replaceAll("^-", "");
        v = v.replaceAll("-$", "");

        return v;
    }

    private static String stripLeadingZeros(String num) {
        String n = num;
        int i = 0;
        while (i < n.length() && n.charAt(i) == '0') i++;
        String out = n.substring(i);
        return out.isEmpty() ? "0" : out;
    }

    private static String safeUpper(String s) {
        if (s == null) return null;
        String v = s.trim().toUpperCase(Locale.ROOT);
        return v.isBlank() ? null : v;
    }
}
