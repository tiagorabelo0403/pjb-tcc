package com.tcc.pjb.backend.service.jurisprudencia.graph;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class CitationExtractor {

    private static final Pattern STF_CASE = Pattern.compile(
            "\\b(RE|ARE|ADI|ADPF|HC|RHC|MS|MI)\\s*(?:n\\.?|N\\.?|No\\.?|nº|Nº)?\\s*([0-9]{1,3}(?:\\.[0-9]{3})*(?:/[A-Z]{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern STJ_CASE = Pattern.compile(
            "\\b(REsp|AREsp|AgInt|AgRg|EREsp|EDcl)\\s*(?:no?|n\\.?|nº)?\\s*([0-9]{1,3}(?:\\.[0-9]{3})*(?:/[A-Z]{2})?)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TEMA = Pattern.compile(
            "\\bTema\\s*(?:STF|STJ)?\\s*([0-9]{1,3}(?:\\.[0-9]{3})*)\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern SUMULA = Pattern.compile(
            "\\bS[úu]mula\\s*(?:Vinculante\\s*)?(\\d{1,4})(?:\\s*do\\s*(STF|STJ|TST))?\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern OJ = Pattern.compile(
            "\\bOJ\\s*(\\d{1,4})\\b",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern ARTIGO = Pattern.compile(
            "\\bart\\.?\\s*(\\d+[A-Za-z]?)\\s*(?:,\\s*§\\s*(\\d+º?))?\\s*(?:do|da)\\s*(CPC|CPP|CLT|CF|CDC|CC)\\b",
            Pattern.CASE_INSENSITIVE);

    public List<CitationRef> extract(String... texts) {
        if (texts == null || texts.length == 0) return List.of();

        String joined = String.join("\n", Arrays.stream(texts).filter(Objects::nonNull).toList());
        if (joined.isBlank()) return List.of();

        LinkedHashMap<String, CitationRef> out = new LinkedHashMap<>();

        scanCases(out, joined, STF_CASE, "STF");
        scanCases(out, joined, STJ_CASE, "STJ");

        scanTema(out, joined);
        scanSumula(out, joined);
        scanOj(out, joined);
        scanArtigos(out, joined);

        return List.copyOf(out.values());
    }

    private void scanCases(Map<String, CitationRef> out, String text, Pattern p, String corte) {
        Matcher m = p.matcher(text);
        while (m.find()) {
            String classe = norm(m.group(1));
            String numero = norm(m.group(2));
            if (classe == null || numero == null) continue;
            String ref = corte + ":" + classe.toUpperCase() + ":" + numero;
            put(out, CitationRelationType.CITES, CitationTargetType.PRECEDENTE, ref, m.group(0));
        }
    }

    private void scanTema(Map<String, CitationRef> out, String text) {
        Matcher m = TEMA.matcher(text);
        while (m.find()) {
            String num = normalizeDigits(m.group(1));
            if (num == null) continue;
            put(out, CitationRelationType.CITES, CitationTargetType.TEMA_REPERCUSSAO, "TEMA:" + num, m.group(0));
        }
    }

    private void scanSumula(Map<String, CitationRef> out, String text) {
        Matcher m = SUMULA.matcher(text);
        while (m.find()) {
            String num = norm(m.group(1));
            String tribunal = norm(m.group(2));
            if (num == null) continue;
            String ref = "SUMULA:" + num + (tribunal != null ? ":" + tribunal.toUpperCase() : "");
            put(out, CitationRelationType.CITES, CitationTargetType.SUMULA, ref, m.group(0));
        }
    }

    private void scanOj(Map<String, CitationRef> out, String text) {
        Matcher m = OJ.matcher(text);
        while (m.find()) {
            String num = norm(m.group(1));
            if (num == null) continue;
            put(out, CitationRelationType.CITES, CitationTargetType.OJ, "OJ:" + num, m.group(0));
        }
    }

    private void scanArtigos(Map<String, CitationRef> out, String text) {
        Matcher m = ARTIGO.matcher(text);
        while (m.find()) {
            String art = norm(m.group(1));
            String par = norm(m.group(2));
            String cod = norm(m.group(3));
            if (art == null || cod == null) continue;
            String ref = "ART:" + art.toUpperCase() + (par != null ? ":PAR:" + par.toUpperCase() : "") + ":" + cod.toUpperCase();
            put(out, CitationRelationType.CITES, CitationTargetType.ARTIGO_LEI, ref, m.group(0));
        }
    }

    private void put(Map<String, CitationRef> out,
                     CitationRelationType relation,
                     CitationTargetType type,
                     String targetRef,
                     String raw) {
        if (targetRef == null || targetRef.isBlank()) return;
        String key = relation.name() + "|" + type.name() + "|" + targetRef;
        out.putIfAbsent(key, new CitationRef(relation, type, targetRef, raw));
    }

    private static String normalizeDigits(String s) {
        String normalized = norm(s);
        return normalized == null ? null : normalized.replace(".", "");
    }

    private static String norm(String s) {
        if (s == null) return null;
        String x = s.trim().replaceAll("\\s+", " ");
        return x.isBlank() ? null : x;
    }
}
