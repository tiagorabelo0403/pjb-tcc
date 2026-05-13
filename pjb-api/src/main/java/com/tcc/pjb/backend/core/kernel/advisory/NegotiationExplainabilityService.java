package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;

@Service
public class NegotiationExplainabilityService {

    public NegotiationExplainabilityReport compose(Processo processo,
                                                   PropostaAcordo proposta,
                                                   List<ChatMensagem> recentChat,
                                                   SettlementAdvisoryReport settlementAdvisory,
                                                   NegotiationMemoryReport negotiationMemory,
                                                   InstitutionalGovernanceContextReport governance) {
        Objects.requireNonNull(processo, "processo");
        List<NegotiationExplainabilityReport.NegotiationNode> nodes = new ArrayList<>();
        Set<String> openQuestions = new LinkedHashSet<>();
        double confidence = 0.66d;

        nodes.add(node(
                "NEGOTIATION_BASELINE",
                "Linha-base negocial",
                "AcordoService/ProcessDigitalTwinService",
                proposta != null ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                List.of(valueOrPlaceholder(processo.getNumeroUnificado()), processo.getFaseAtual() != null ? processo.getFaseAtual().name() : "fase:pending"),
                proposta != null && proposta.getValorAcordo() != null ? List.of("valor:" + proposta.getValorAcordo()) : List.of("valor:pending"),
                List.of()
        ));

        nodes.add(node(
                "NEGOTIATION_CHAT_HISTORY",
                "Histórico conversacional",
                "ChatMensagemRepository",
                recentChat != null && !recentChat.isEmpty() ? "MEDIUM" : "LOW",
                extractChatInputs(recentChat),
                extractChatOutputs(recentChat),
                extractChatRisks(recentChat)
        ));

        nodes.add(node(
                "NEGOTIATION_SETTLEMENT_ADVISORY",
                "Janela e executabilidade",
                "SettlementAdvisoryService",
                settlementAdvisory != null && settlementAdvisory.executable() ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                settlementAdvisory != null ? settlementAdvisory.nextMoves() : List.of("next-moves:pending"),
                settlementAdvisory != null ? settlementAdvisory.executionSafeguards() : List.of("execution:pending"),
                settlementAdvisory != null && settlementAdvisory.window() != null ? settlementAdvisory.window().risks() : List.of()
        ));

        nodes.add(node(
                "NEGOTIATION_MEMORY",
                "Memória negocial reaproveitável",
                "NegotiationMemoryService",
                negotiationMemory != null ? "MEDIUM" : "LOW",
                negotiationMemory != null ? negotiationMemory.learnedPatterns() : List.of("memory:pending"),
                negotiationMemory != null ? negotiationMemory.reusablePlaybooks() : List.of("playbooks:pending"),
                negotiationMemory != null ? negotiationMemory.repeatedFailureModes() : List.of()
        ));

        nodes.add(node(
                "NEGOTIATION_GOVERNANCE",
                "Governança institucional",
                "InstitutionalGovernanceContextService",
                governance != null && governance.governanceAlerts().isEmpty() ? "MEDIUM_HIGH" : "MEDIUM_LOW",
                governance != null ? governance.anchorDimensions() : List.of("governance:pending"),
                governance != null ? governance.policyGuards() : List.of("policy-guards:pending"),
                governance != null ? governance.governanceAlerts() : List.of()
        ));

        if (proposta == null) {
            openQuestions.add("Formalizar proposta-base ou parâmetro financeiro antes de consolidar a rodada explicável.");
            confidence -= 0.11d;
        }
        if (recentChat == null || recentChat.isEmpty()) {
            openQuestions.add("Registrar interação negocial mínima no chat para gerar trilha explicável mais forte.");
            confidence -= 0.08d;
        }
        if (settlementAdvisory != null && !settlementAdvisory.executable()) {
            openQuestions.addAll(limit(settlementAdvisory.executionSafeguards(), 2));
            confidence -= 0.08d;
        }
        if (negotiationMemory != null && !negotiationMemory.repeatedFailureModes().isEmpty()) {
            openQuestions.addAll(limit(negotiationMemory.repeatedFailureModes(), 3));
            confidence -= 0.05d;
        }
        if (governance != null && !governance.governanceAlerts().isEmpty()) {
            openQuestions.addAll(limit(governance.governanceAlerts(), 3));
            confidence -= 0.04d;
        }

        return new NegotiationExplainabilityReport(
                "NEGOTIATION",
                openQuestions.isEmpty() ? "NEGOTIATION_EXPLAINABILITY_STABLE" : "NEGOTIATION_EXPLAINABILITY_ATTENTION",
                round(clamp(confidence)),
                List.copyOf(nodes),
                List.copyOf(openQuestions),
                PayloadMaps.ofEntries(
                        "scope", "NEGOTIATION",
                        "processoId", processo.getId(),
                        "proposalId", proposta != null ? proposta.getId() : null,
                        "messageCount", recentChat != null ? recentChat.size() : 0,
                        "governanceStatus", governance != null ? governance.status() : null
                )
        );
    }

    private static List<String> extractChatInputs(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return List.of();
        }
        return recentChat.stream()
                .filter(Objects::nonNull)
                .map(ChatMensagem::getConteudo)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .limit(4)
                .toList();
    }

    private static List<String> extractChatOutputs(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> outputs = new LinkedHashSet<>();
        for (ChatMensagem msg : recentChat) {
            if (msg == null || blank(msg.getConteudo())) {
                continue;
            }
            String lower = NegotiationLanguageHeuristics.normalize(msg.getConteudo());
            if (!NegotiationLanguageHeuristics.containsSettlementFriction(lower) && lower.contains("aceit")) {
                outputs.add("Sinal de aceitabilidade apareceu no histórico.");
            }
            if (lower.contains("parcel")) {
                outputs.add("Parcelamento surgiu como formato operacional de convergência.");
            }
            if (lower.contains("multa") || lower.contains("garantia")) {
                outputs.add("Enforcement contratual apareceu como exigência recorrente.");
            }
        }
        return List.copyOf(outputs);
    }

    private static List<String> extractChatRisks(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> risks = new LinkedHashSet<>();
        for (ChatMensagem msg : recentChat) {
            if (msg == null || blank(msg.getConteudo())) {
                continue;
            }
            String lower = NegotiationLanguageHeuristics.normalize(msg.getConteudo());
            if (NegotiationLanguageHeuristics.containsSettlementFriction(lower)) {
                risks.add("O histórico registra impasse explícito.");
            }
            if (lower.contains("prazo") || lower.contains("urgente")) {
                risks.add("A pressão temporal pode reduzir margem de negociação segura.");
            }
            if (lower.contains("sigilo")) {
                risks.add("Há conteúdo negocial sensível que exige circulação controlada.");
            }
        }
        return List.copyOf(risks);
    }

    private static NegotiationExplainabilityReport.NegotiationNode node(String code,
                                                                        String title,
                                                                        String source,
                                                                        String confidenceBand,
                                                                        List<String> inputs,
                                                                        List<String> outputs,
                                                                        List<String> risks) {
        return new NegotiationExplainabilityReport.NegotiationNode(
                code,
                title,
                source,
                confidenceBand,
                List.copyOf(limit(inputs, 6)),
                List.copyOf(limit(outputs, 6)),
                List.copyOf(limit(risks, 6))
        );
    }

    private static String valueOrPlaceholder(String value) {
        return blank(value) ? "pending" : value;
    }

    private static List<String> limit(List<String> source, int max) {
        if (source == null || source.isEmpty() || max <= 0) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).doubleValue();
    }
}
