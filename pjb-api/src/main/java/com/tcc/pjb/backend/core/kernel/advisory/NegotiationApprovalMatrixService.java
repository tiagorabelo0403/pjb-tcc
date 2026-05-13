package com.tcc.pjb.backend.core.kernel.advisory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;

@Service
public class NegotiationApprovalMatrixService {

    public NegotiationApprovalMatrixReport analyzeProcess(Processo processo,
                                                          PropostaAcordo proposta,
                                                          List<ChatMensagem> recentChat,
                                                          InstitutionalGovernanceContextReport governance,
                                                          KernelOperationalGovernanceReport kernelOperationalGovernance,
                                                          NegotiationMemoryReport negotiationMemory,
                                                          NegotiationExplainabilityReport negotiationExplainability,
                                                          NegotiationChatDigestReport negotiationChatDigest) {
        Objects.requireNonNull(processo, "processo");
        ApprovalSignals signals = deriveSignals(recentChat);
        Set<String> approvalGates = new LinkedHashSet<>();
        Set<String> escalationLanes = new LinkedHashSet<>();
        Set<String> internalControls = new LinkedHashSet<>();
        Set<String> releaseChecklist = new LinkedHashSet<>();
        double confidence = 0.66d;

        if (proposta == null) {
            approvalGates.add("Formalizar proposta de acordo versionada antes de liberar mensagem de fechamento para a contraparte.");
            releaseChecklist.add("Definir versão-base de proposta com valor, cronograma e status negocial controlado.");
            confidence -= 0.08d;
        } else {
            if (proposta.getStatus() == StatusAcordo.RASCUNHO || proposta.getStatus() == StatusAcordo.AGUARDANDO_REVISAO_HUMANA) {
                approvalGates.add("A proposta vigente depende de revisão humana antes de qualquer escalada de fechamento no chat.");
                confidence -= 0.09d;
            }
            if (proposta.getAprovadoPor() == null || proposta.getDataAprovacao() == null) {
                approvalGates.add("Registrar responsável interno pela liberação da rodada negocial e trilha temporal de aprovação.");
                confidence -= 0.05d;
            } else {
                releaseChecklist.add("Validação interna registrada para a proposta vigente.");
                confidence += 0.04d;
            }
            if (proposta.getValorAcordo() == null || proposta.getValorAcordo().signum() <= 0) {
                approvalGates.add("Fixar materialidade econômica mínima antes do envio de contraproposta controlada.");
                confidence -= 0.06d;
            } else {
                releaseChecklist.add("Valor-base estruturado para a rodada atual.");
                confidence += 0.03d;
            }
        }

        if (signals.messageCount() == 0) {
            approvalGates.add("Abrir a negociação com mensagem inicial governada antes de qualquer liberação ampliada.");
            releaseChecklist.add("Preparar mensagem inaugural com objetivo, janela temporal e premissas de sigilo.");
            confidence -= 0.06d;
        } else {
            releaseChecklist.add("Histórico conversacional disponível para contextualizar a próxima rodada.");
            confidence += 0.02d;
        }

        if (signals.externalApprovalCount() > 0) {
            approvalGates.add("O próprio histórico indica dependência de alçada externa, cliente, diretoria ou compliance.");
            escalationLanes.add("Escalar a rodada para trilha de aprovação executiva ou patrocinador decisório antes do fechamento.");
            confidence -= 0.08d;
        }
        if (signals.urgencyCount() > 0) {
            internalControls.add("Operar com relógio de resposta definido e check de consistência antes de enviar mensagens sob pressão temporal.");
            releaseChecklist.add("Validar prazo crítico mencionado no chat antes do próximo envio.");
            confidence -= 0.02d;
        }
        if (signals.enforcementCount() > 0) {
            internalControls.add("Revisar cláusulas de multa, garantias e executabilidade antes de consolidar aceite escrito.");
            releaseChecklist.add("Conferir enforcement, cronograma e gatilhos de inadimplemento.");
        }
        if (signals.tensionCount() > signals.cooperationCount()) {
            escalationLanes.add("Escalada para revisão estratégica antes de nova âncora financeira devido a fricção predominante.");
            internalControls.add("Bloquear concessões impulsivas enquanto o histórico indicar impasse ou recusa.");
            confidence -= 0.07d;
        } else if (signals.cooperationCount() > 0) {
            releaseChecklist.add("Há abertura suficiente para mensagem progressiva de convergência com confirmação objetiva dos termos.");
            confidence += 0.03d;
        }

        if (governance != null) {
            approvalGates.addAll(limit(governance.governanceAlerts(), 3));
            escalationLanes.addAll(limit(governance.escalationPlaybooks(), 3));
            internalControls.addAll(limit(governance.policyGuards(), 3));
            confidence += governance.governanceAlerts().isEmpty() ? 0.02d : -0.04d;
        }

        if (kernelOperationalGovernance != null) {
            internalControls.addAll(limit(kernelOperationalGovernance.controls(), 3));
            internalControls.addAll(limit(kernelOperationalGovernance.watchpoints(), 2));
            releaseChecklist.addAll(limit(kernelOperationalGovernance.nextActions(), 3));
            confidence += kernelOperationalGovernance.watchpoints().isEmpty() ? 0.02d : -0.03d;
        }

        if (negotiationMemory != null) {
            escalationLanes.addAll(limit(negotiationMemory.repeatedFailureModes(), 2));
            releaseChecklist.addAll(limit(negotiationMemory.reusablePlaybooks(), 2));
            confidence += negotiationMemory.repeatedFailureModes().isEmpty() ? 0.02d : -0.03d;
        }

        if (negotiationExplainability != null) {
            releaseChecklist.addAll(limit(negotiationExplainability.openQuestions(), 2));
            confidence += negotiationExplainability.openQuestions().isEmpty() ? 0.01d : -0.02d;
        }

        if (negotiationChatDigest != null) {
            internalControls.addAll(limit(negotiationChatDigest.protectedTopics(), 3));
            escalationLanes.addAll(limit(negotiationChatDigest.escalationSignals(), 3));
            releaseChecklist.addAll(limit(negotiationChatDigest.nextTurnObjectives(), 3));
            releaseChecklist.addAll(limit(negotiationChatDigest.internalActions(), 2));
            confidence += "CONTROLLED_RELEASE".equals(negotiationChatDigest.sendMode()) || "CLOSEOUT_RELEASE".equals(negotiationChatDigest.sendMode()) ? 0.03d : -0.02d;
        }

        String approvalBand = resolveApprovalBand(proposta, approvalGates, escalationLanes, signals, negotiationChatDigest);
        String releaseMode = resolveReleaseMode(approvalBand, signals, proposalExecutable(proposta), negotiationChatDigest);
        String status = approvalGates.isEmpty() && escalationLanes.isEmpty() ? "NEGOTIATION_APPROVAL_STABLE" : "NEGOTIATION_APPROVAL_ATTENTION";

        return new NegotiationApprovalMatrixReport(
                "NEGOTIATION_APPROVAL",
                status,
                round(clamp(confidence)),
                approvalBand,
                releaseMode,
                List.copyOf(approvalGates),
                List.copyOf(escalationLanes),
                List.copyOf(internalControls),
                List.copyOf(releaseChecklist),
                PayloadMaps.ofEntries(
                        "scope", "NEGOTIATION_APPROVAL",
                        "processoId", processo.getId(),
                        "proposalId", proposta != null ? proposta.getId() : null,
                        "messageCount", signals.messageCount(),
                        "externalApprovalCount", signals.externalApprovalCount(),
                        "tensionCount", signals.tensionCount(),
                        "cooperationCount", signals.cooperationCount(),
                        "approvalBand", approvalBand,
                        "releaseMode", releaseMode
                )
        );
    }

    private static String resolveApprovalBand(PropostaAcordo proposta,
                                              Set<String> approvalGates,
                                              Set<String> escalationLanes,
                                              ApprovalSignals signals,
                                              NegotiationChatDigestReport negotiationChatDigest) {
        if (!approvalGates.isEmpty()) {
            if (signals.externalApprovalCount() > 0 || containsToken(approvalGates, "alçada") || containsToken(approvalGates, "cliente") || containsToken(approvalGates, "diretoria")) {
                return "EXECUTIVE_ESCALATION";
            }
            if (proposta != null && (proposta.getStatus() == StatusAcordo.AGUARDANDO_REVISAO_HUMANA || proposta.getStatus() == StatusAcordo.RASCUNHO)) {
                return "INTERNAL_REVIEW_REQUIRED";
            }
            return "CONTROLLED_HOLD";
        }
        if (!escalationLanes.isEmpty() || signals.tensionCount() > signals.cooperationCount()) {
            return "STRATEGIC_REVIEW";
        }
        if (negotiationChatDigest != null && "CONVERGING".equals(negotiationChatDigest.conversationStage())) {
            return "READY_FOR_RELEASE";
        }
        return "GUIDED_PROGRESS";
    }

    private static String resolveReleaseMode(String approvalBand,
                                             ApprovalSignals signals,
                                             boolean executableProposal,
                                             NegotiationChatDigestReport negotiationChatDigest) {
        if ("EXECUTIVE_ESCALATION".equals(approvalBand) || "CONTROLLED_HOLD".equals(approvalBand)) {
            return "BLOCKED_RELEASE";
        }
        if ("INTERNAL_REVIEW_REQUIRED".equals(approvalBand) || "STRATEGIC_REVIEW".equals(approvalBand)) {
            return "MANUAL_REVIEW";
        }
        if (negotiationChatDigest != null && "CLOSEOUT_RELEASE".equals(negotiationChatDigest.sendMode()) && executableProposal) {
            return "CLOSEOUT_RELEASE";
        }
        if (signals.tensionCount() > 0) {
            return "GUIDED_RELEASE";
        }
        return executableProposal ? "CONTROLLED_RELEASE" : "GUIDED_RELEASE";
    }

    private static ApprovalSignals deriveSignals(List<ChatMensagem> recentChat) {
        if (recentChat == null || recentChat.isEmpty()) {
            return new ApprovalSignals(0, 0, 0, 0, 0, 0);
        }
        int messageCount = 0;
        int externalApprovalCount = 0;
        int urgencyCount = 0;
        int enforcementCount = 0;
        int tensionCount = 0;
        int cooperationCount = 0;
        for (ChatMensagem message : recentChat) {
            if (message == null || blank(message.getConteudo())) {
                continue;
            }
            messageCount++;
            String lower = normalize(message.getConteudo());
            if (containsAny(lower, "cliente", "diretoria", "diretoria", "sócio", "socio", "gestor", "compliance", "financeiro", "aprovação", "aprovacao")) {
                externalApprovalCount++;
            }
            if (containsAny(lower, "hoje", "amanhã", "amanha", "urgente", "prazo", "deadline")) {
                urgencyCount++;
            }
            if (containsAny(lower, "garantia", "multa", "inadimpl", "homolog", "execuç", "execuc")) {
                enforcementCount++;
            }
            boolean settlementFriction = NegotiationLanguageHeuristics.containsSettlementFriction(lower);
            if (settlementFriction) {
                tensionCount++;
            }
            if (!settlementFriction && containsAny(lower, "aceit", "concord", "fechar", "consenso", "avançar", "avancar")) {
                cooperationCount++;
            }
        }
        return new ApprovalSignals(messageCount, externalApprovalCount, urgencyCount, enforcementCount, tensionCount, cooperationCount);
    }

    private static boolean proposalExecutable(PropostaAcordo proposta) {
        return proposta != null && proposta.getValorAcordo() != null && proposta.getValorAcordo().compareTo(BigDecimal.ZERO) > 0;
    }

    private static boolean containsToken(Set<String> values, String token) {
        if (values == null || values.isEmpty() || blank(token)) {
            return false;
        }
        return values.stream().filter(Objects::nonNull).map(NegotiationApprovalMatrixService::normalize).anyMatch(value -> value.contains(normalize(token)));
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> limit(List<String> source, int max) {
        if (source == null || source.isEmpty() || max <= 0) {
            return List.of();
        }
        return source.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).distinct().limit(max).toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
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

    private record ApprovalSignals(
            int messageCount,
            int externalApprovalCount,
            int urgencyCount,
            int enforcementCount,
            int tensionCount,
            int cooperationCount
    ) {
    }
}
