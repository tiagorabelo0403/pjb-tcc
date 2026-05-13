package com.tcc.pjb.backend.ai.agentic.agents.legal;

import java.util.*;
import org.springframework.stereotype.Component;
import com.tcc.pjb.backend.ai.agentic.agents.common.Agent;
import com.tcc.pjb.backend.ai.agentic.core.AgentResult;
import com.tcc.pjb.backend.ai.agentic.core.AgenticRunRequest;
import java.util.Locale;

@Component
public class TaxAgent implements Agent {

    @Override
    public String name() {
        return "TaxAgent";
    }

    @Override
    public AgentResult execute(AgenticRunRequest request) {
        String corpus = buildCorpus(request);
        TaxAssessment assessment = assess(corpus);

        AgentResult out = new AgentResult();
        out.setAgent(name());
        out.setConfidence(assessment.confidence);
        out.setHumanReviewRequired(assessment.requiresHuman);
        out.setData(Map.of(
                "classification", assessment.classification,
                "signals", assessment.signals,
                "checklist", assessment.checklist,
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
            Object facts = in.get("facts");
            if (facts != null) sb.append('\n').append(facts);
            Object doc = in.get("documentText");
            if (doc != null) sb.append('\n').append(doc);
        }
        return sb.toString();
    }

    private static TaxAssessment assess(String corpus) {
        if (corpus == null || corpus.isBlank()) {
            return new TaxAssessment(0.35, true, "insuficiente", List.of(), List.of(), List.of("fornecer_fatos_tributarios"));
        }

        String lc = corpus.toLowerCase(Locale.ROOT);
        LinkedHashSet<String> signals = new LinkedHashSet<>();

        if (containsAny(lc, "execução fiscal", "execucao fiscal", "cda", "penhora", "garantia", "embargos")) {
            signals.add("execucao_fiscal");
        }
        if (containsAny(lc, "icms", "substituição tributária", "substituicao tributaria")) {
            signals.add("icms");
        }
        if (containsAny(lc, "iss", "serviço", "servico")) {
            signals.add("iss");
        }
        if (containsAny(lc, "irpf", "irpj", "lucro real", "lucro presumido")) {
            signals.add("imposto_de_renda");
        }
        if (containsAny(lc, "pis", "cofins", "cumulativo", "não cumulativo", "nao cumulativo")) {
            signals.add("pis_cofins");
        }

        String classification = classify(signals);

        List<String> checklist = buildChecklist(signals);
        List<String> actions = new ArrayList<>();
        if (signals.contains("execucao_fiscal")) {
            actions.add("reconstruir_linha_do_tempo_credito_tributario");
            actions.add("validar_cda_e_juros_multa");
        }
        actions.add("confirmar_fatos_geradores_e_competencias");

        boolean requiresHuman = signals.contains("execucao_fiscal");
        double confidence = signals.isEmpty() ? 0.45 : 0.78;

        return new TaxAssessment(confidence, requiresHuman, classification, signals.stream().toList(), checklist, actions);
    }

    private static String classify(Set<String> signals) {
        if (signals.contains("execucao_fiscal")) return "contencioso_execucao_fiscal";
        if (signals.contains("icms") || signals.contains("iss") || signals.contains("pis_cofins")) return "tributos_indiretos";
        if (signals.contains("imposto_de_renda")) return "imposto_de_renda";
        return "tributario_geral";
    }

    private static List<String> buildChecklist(Set<String> signals) {
        ArrayList<String> out = new ArrayList<>();
        out.add("identificar_fato_gerador_e_base_de_calculo");
        out.add("identificar_lancamento_auto_infracao_e_defesa_administrativa");
        out.add("verificar_prazo_decadencia_prescricao");
        if (signals.contains("execucao_fiscal")) {
            out.add("validar_elementos_cda");
            out.add("verificar_garantia_penhora_deposito");
            out.add("mapear_excecao_pre_executividade_ou_embargos");
        }
        if (signals.contains("icms")) {
            out.add("mapear_operacao_e_regime_st");
        }
        if (signals.contains("iss")) {
            out.add("confirmar_municipio_incidente_e_lista_servicos");
        }
        if (signals.contains("pis_cofins")) {
            out.add("avaliar_creditos_nao_cumulativos");
        }
        if (signals.contains("imposto_de_renda")) {
            out.add("avaliar_regime_lucro_e_apuracao");
        }
        return List.copyOf(out);
    }

    private static boolean containsAny(String lc, String... needles) {
        if (lc == null || lc.isBlank()) return false;
        for (String n : needles) {
            if (n == null || n.isBlank()) continue;
            if (lc.contains(n)) return true;
        }
        return false;
    }

    private record TaxAssessment(double confidence, boolean requiresHuman, String classification, List<String> signals, List<String> checklist, List<String> actions) {
    }
}
