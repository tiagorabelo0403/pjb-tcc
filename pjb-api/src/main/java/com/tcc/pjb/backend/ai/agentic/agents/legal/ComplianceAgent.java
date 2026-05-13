package com.tcc.pjb.backend.ai.agentic.agents.legal;

import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;
import java.util.Locale;

@Component
public class ComplianceAgent implements Agent {

    @Override
    public String name() {
        return "ComplianceAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        String corpus = buildCorpus(request);
        ComplianceAssessment assessment = assess(corpus);

        AgentResult out = new AgentResult();
        out.setAgent(name());
        out.setConfidence(assessment.confidence);
        out.setHumanReviewRequired(assessment.requiresHuman);
        out.setData(Map.of(
                "flags", assessment.flags,
                "actions", assessment.actions
        ));
        return out;
    }

    private static String buildCorpus(AgenticRunRequest request) {
        if (request == null) return "";
        StringBuilder sb = new StringBuilder(512);
        if (request.getTask() != null) sb.append(request.getTask());
        Map<String, Object> in = request.getInput();
        if (in != null) {
            Object doc = in.get("documentText");
            if (doc != null) {
                sb.append('\n').append(doc);
            }
            Object facts = in.get("facts");
            if (facts != null) {
                sb.append('\n').append(facts);
            }
        }
        return sb.toString();
    }

    private static ComplianceAssessment assess(String corpus) {
        if (corpus == null || corpus.isBlank()) {
            return new ComplianceAssessment(0.40, true, List.of("entrada_insuficiente"), List.of("fornecer_fatos_e_documentos"));
        }

        String lc = corpus.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> flags = new LinkedHashSet<>();
        LinkedHashSet<String> actions = new LinkedHashSet<>();

        if (containsAny(lc, "cpf", "rg", "cnh", "biometr", "endereco", "endereço", "telefone", "email")) {
            flags.add("lgpd_sensiveis");
            actions.add("mascarar_identificadores");
        }

        if (containsAny(lc, "menor", "criança", "crianca", "adolescente", "adoção", "adocao")) {
            flags.add("infancia_juventude");
            actions.add("aplicar_sigilo_reforcado");
        }

        if (containsAny(lc, "violencia doméstica", "violencia domestica", "medida protetiva", "maria da penha")) {
            flags.add("violencia_domestica");
            actions.add("redigir_com_cautela_e_sigilo");
        }

        if (containsAny(lc, "laudo", "prontuário", "prontuario", "diagnóstico", "diagnostico", "tratamento", "cirurgia", "uti")) {
            flags.add("saude_documental");
            actions.add("avaliar_sigilo_medico");
        }

        if (containsAny(lc, "interceptação", "interceptacao", "colaboração premiada", "colaboracao premiada")) {
            flags.add("penal_sensivel");
            actions.add("validar_autorizacoes_e_segredo");
        }

        boolean human = flags.contains("penal_sensivel") || flags.contains("infancia_juventude");
        double conf = flags.isEmpty() ? 0.70 : 0.80;

        if (flags.isEmpty()) {
            actions.add("prosseguir_com_analise");
        }

        return new ComplianceAssessment(conf, human, flags.stream().toList(), actions.stream().toList());
    }

    private static boolean containsAny(String lc, String... needles) {
        if (lc == null || lc.isBlank()) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (lc.contains(n)) return true;
        }
        return false;
    }

    private record ComplianceAssessment(double confidence, boolean requiresHuman, List<String> flags, List<String> actions) {
    }
}
