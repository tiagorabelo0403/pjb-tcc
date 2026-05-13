package com.tcc.pjb.backend.ai.agentic.core;

import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.ai.scope.MateriaDecision;
import com.tcc.pjb.backend.ai.scope.MateriaInferenceEngine;
import com.tcc.pjb.backend.service.SigiloService;

@Service
public class AgenticRequestRouter {

    private final MateriaInferenceEngine materiaInferenceEngine;
    private final SigiloService sigiloService;

    public AgenticRequestRouter(MateriaInferenceEngine materiaInferenceEngine, SigiloService sigiloService) {
        this.materiaInferenceEngine = materiaInferenceEngine;
        this.sigiloService = sigiloService;
    }

    public AgenticRoutingDecision route(AgenticRunRequest request) {
        String task = safeString(request != null ? request.getTask() : null);
        Map<String, Object> input = request != null && request.getInput() != null ? request.getInput() : Map.of();

        AgenticDomain domain = resolveDomain(request != null ? request.getDomain() : null, task, input);

        String assunto = safeString(input.get("assunto"));
        String materiaHint = safeString(input.get("materia"));
        String rito = safeString(input.get("rito"));
        String jurisdicao = safeString(input.get("jurisdicao"));
        String docText = safeString(input.get("documentText"));
        String facts = safeString(input.get("facts"));

        MateriaDecision materia = materiaInferenceEngine.infer(task, assunto, materiaHint, rito, jurisdicao, facts, docText);

        String sigiloCorpus = buildSigiloCorpus(task, assunto, materiaHint, rito, jurisdicao, facts, docText);
        SigiloService.SigiloDecision sigilo = sigiloService.avaliarCorpus(sigiloCorpus);

        String query = resolveQuery(task, input);

        return new AgenticRoutingDecision(domain, materia, sigilo, query);
    }

    private static AgenticDomain resolveDomain(AgenticDomain explicit, String task, Map<String, Object> input) {
        if (explicit != null) {
            return explicit;
        }

        String t = task.toLowerCase(Locale.ROOT);
        if (looksFinance(input) || containsAny(t, "balanco", "balanço", "dre", "ebitda", "valuation", "wacc", "cashflow", "fluxo de caixa", "multiplo", "múltiplo", "endividamento", "liquidez", "margem")) {
            return AgenticDomain.FINANCE;
        }

        return AgenticDomain.LEGAL;
    }

    private static boolean looksFinance(Map<String, Object> input) {
        if (input == null || input.isEmpty()) return false;
        return input.containsKey("balanceSheet")
                || input.containsKey("dre")
                || input.containsKey("cashFlow")
                || input.containsKey("valuation")
                || input.containsKey("ticker")
                || input.containsKey("cvm")
                || input.containsKey("bcb");
    }

    private static String resolveQuery(String task, Map<String, Object> input) {
        String q = safeString(input.get("query"));
        if (!q.isBlank()) return q;
        q = safeString(input.get("pergunta"));
        if (!q.isBlank()) return q;
        q = safeString(input.get("tema"));
        if (!q.isBlank()) return q;
        return task;
    }

    private static String buildSigiloCorpus(String task,
                                           String assunto,
                                           String materia,
                                           String rito,
                                           String jurisdicao,
                                           String facts,
                                           String docText) {
        StringBuilder sb = new StringBuilder(512);
        append(sb, task);
        append(sb, assunto);
        append(sb, materia);
        append(sb, rito);
        append(sb, jurisdicao);
        append(sb, facts);
        append(sb, docText);
        return sb.toString();
    }

    private static void append(StringBuilder sb, String s) {
        if (s == null || s.isBlank()) return;
        if (!sb.isEmpty()) sb.append('\n');
        sb.append(s);
    }

    private static String safeString(Object o) {
        if (o == null) return "";
        String s = String.valueOf(o);
        if (s == null) return "";
        return s.trim();
    }

    private static boolean containsAny(String lc, String... needles) {
        if (lc == null || lc.isBlank() || needles == null) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (lc.contains(n.toLowerCase(Locale.ROOT))) return true;
        }
        return false;
    }
}
