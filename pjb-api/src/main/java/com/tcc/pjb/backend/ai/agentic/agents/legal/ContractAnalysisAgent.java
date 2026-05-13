package com.tcc.pjb.backend.ai.agentic.agents.legal;

import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;
import com.tcc.pjb.backend.shared.text.TextTokenUtils;
import java.util.Locale;

@Component
public class ContractAnalysisAgent implements Agent {

    private static final List<RiskLexiconItem> LEXICON = List.of(
            RiskLexiconItem.of("multa", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("penalidade", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("clausula penal", RiskSeverity.HIGH),
            RiskLexiconItem.of("cláusula penal", RiskSeverity.HIGH),
            RiskLexiconItem.of("rescisao unilateral", RiskSeverity.HIGH),
            RiskLexiconItem.of("rescisão unilateral", RiskSeverity.HIGH),
            RiskLexiconItem.of("exclusao", RiskSeverity.HIGH),
            RiskLexiconItem.of("exclusão", RiskSeverity.HIGH),
            RiskLexiconItem.of("confidencialidade", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("nao concorrencia", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("não concorrência", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("foro", RiskSeverity.LOW),
            RiskLexiconItem.of("arbitragem", RiskSeverity.MEDIUM),
            RiskLexiconItem.of("reajuste", RiskSeverity.LOW),
            RiskLexiconItem.of("indice", RiskSeverity.LOW),
            RiskLexiconItem.of("prazo", RiskSeverity.LOW)
    );

    @Override
    public String name() {
        return "ContractAnalysisAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        String text = resolveText(request);
        List<RiskClauseFinding> findings = analyze(text);

        double confidence = findings.isEmpty() ? 0.35 : 0.78;
        boolean human = findings.stream().anyMatch(f -> f.severity == RiskSeverity.HIGH);

        List<String> actions = findings.isEmpty()
                ? List.of("fornecer_texto_integral_do_contrato")
                : List.of("validar_contexto_das_clausulas", "confirmar_minuta_e_versao");

        AgentResult out = new AgentResult();
        out.setAgent(name());
        out.setConfidence(confidence);
        out.setHumanReviewRequired(human);
        out.setData(Map.of(
                "findings", findings.stream().map(RiskClauseFinding::toMap).toList(),
                "actions", actions
        ));
        return out;
    }

    private static String resolveText(AgenticRunRequest request) {
        if (request == null) return "";
        Map<String, Object> in = request.getInput();
        if (in != null) {
            Object t = in.get("contractText");
            if (t != null && !String.valueOf(t).isBlank()) return String.valueOf(t);
            t = in.get("documentText");
            if (t != null && !String.valueOf(t).isBlank()) return String.valueOf(t);
        }
        return request.getTask();
    }

    private static List<RiskClauseFinding> analyze(String text) {
        if (text == null || text.isBlank()) return List.of();

        String lc = text.toLowerCase(Locale.ROOT);
        Set<String> tokens = TextTokenUtils.tokens(lc);

        LinkedHashMap<String, RiskClauseFinding> out = new LinkedHashMap<>();

        for (RiskLexiconItem it : LEXICON) {
            if (lc.contains(it.term)) {
                RiskClauseFinding f = extractFinding(text, lc, tokens, it);
                String k = it.term + "|" + f.severity;
                out.putIfAbsent(k, f);
            }
        }

        ArrayList<RiskClauseFinding> list = new ArrayList<>(out.values());
        list.sort(Comparator.<RiskClauseFinding, RiskSeverity>comparing(a -> a.severity).reversed()
                .thenComparing(a -> a.term));

        if (list.size() > 12) {
            return List.copyOf(list.subList(0, 12));
        }
        return List.copyOf(list);
    }

    private static RiskClauseFinding extractFinding(String original, String lc, java.util.Collection<String> tokens, RiskLexiconItem it) {
        int idx = lc.indexOf(it.term);
        int window = 220;
        int start = Math.max(0, idx - window);
        int end = Math.min(original.length(), idx + it.term.length() + window);

        String snippet = original.substring(start, end).trim();
        RiskSeverity sev = adjustSeverity(it.severity, lc, idx);

        LinkedHashSet<String> anchors = new LinkedHashSet<>();
        for (String t : tokens) {
            if (t.length() <= 3) continue;
            if (snippet.toLowerCase(Locale.ROOT).contains(t) && anchors.size() < 8) {
                anchors.add(t);
            }
        }

        return new RiskClauseFinding(it.term, sev, snippet, anchors.stream().toList());
    }

    private static RiskSeverity adjustSeverity(RiskSeverity base, String lc, int idx) {
        if (base == RiskSeverity.HIGH) return base;
        int from = Math.max(0, idx - 180);
        int to = Math.min(lc.length(), idx + 180);
        String near = lc.substring(from, to);
        if (near.contains("sem possibilidade") || near.contains("irrevog") || near.contains("irretrat")) {
            return RiskSeverity.HIGH;
        }
        if (near.contains("multa") && near.contains("percent")) {
            return RiskSeverity.MEDIUM;
        }
        return base;
    }

    private enum RiskSeverity {
        LOW, MEDIUM, HIGH
    }

    private record RiskLexiconItem(String term, RiskSeverity severity) {
        static RiskLexiconItem of(String term, RiskSeverity severity) {
            return new RiskLexiconItem(term.toLowerCase(Locale.ROOT), severity);
        }
    }

    private record RiskClauseFinding(String term, RiskSeverity severity, String snippet, List<String> anchors) {
        Map<String, Object> toMap() {
            return Map.of(
                    "term", term,
                    "severity", severity.name(),
                    "snippet", snippet,
                    "anchors", anchors
            );
        }
    }
}
