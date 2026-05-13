package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationSessionBootstrapService {

    public LegalAiConversationSessionBootstrapSnapshot inspect(LegalAiConversationRequest request,
                                                               String capability,
                                                               String version,
                                                               LegalAiConversationMemorySnapshot memory,
                                                               LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                               LegalAiConversationToolScopeSnapshot toolScope,
                                                               LegalAiConversationSessionDoctorSnapshot sessionDoctor) {
        String profile = normalize(request == null ? null : request.userProfile());
        String capabilityCode = normalize(capability);
        boolean criticalCapability = isCriticalCapability(capabilityCode, request == null ? null : request.message());
        String sigiloFence = resolveSigiloFence(request, profile, documentSecurity, sessionDoctor);
        String profileGate = resolveProfileGate(profile, capabilityCode);
        List<String> mandatorySkillIds = mandatorySkillIds(capabilityCode);
        List<String> mandatoryToolExampleIds = mandatoryToolExampleIds(capabilityCode);
        List<String> pinnedSkills = listValue(toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpSkillIds"));
        List<String> promotedExamples = listValue(toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpPromotedToolExampleIds"));
        List<String> pinnedExamples = promotedExamples.isEmpty()
                ? listValue(toolScope == null || toolScope.diagnostics() == null ? null : toolScope.diagnostics().get("mcpToolExampleIds"))
                : promotedExamples;
        LinkedHashSet<String> missingSkillIds = new LinkedHashSet<>(mandatorySkillIds);
        missingSkillIds.removeAll(pinnedSkills);
        LinkedHashSet<String> missingToolExampleIds = new LinkedHashSet<>(mandatoryToolExampleIds);
        missingToolExampleIds.removeAll(pinnedExamples);
        int retainedTurnCount = memory == null || memory.retainedTurns() == null ? 0 : memory.retainedTurns().size();
        boolean repeatedDriftDetected = sessionDoctor != null
                && sessionDoctor.driftDetected()
                && (retainedTurnCount >= 3 || countReasonOccurrences(sessionDoctor.reasons(), "contradi") >= 2 || countReasonOccurrences(sessionDoctor.reasons(), "insufici") >= 2);
        boolean mandatoryCoverageMissing = criticalCapability && (!missingSkillIds.isEmpty() || !missingToolExampleIds.isEmpty());
        boolean blockedCapability = "BLOCKED".equals(profileGate)
                || "BLOCKED".equals(sigiloFence)
                || (repeatedDriftDetected && criticalCapability)
                || (repeatedDriftDetected && "DEGRADED".equalsIgnoreCase(sigiloFence));
        String status = blockedCapability
                ? "BLOCKED"
                : "DEGRADED".equals(profileGate)
                || "DEGRADED".equals(sigiloFence)
                || repeatedDriftDetected
                || mandatoryCoverageMissing
                ? "DEGRADED"
                : "READY";
        String operationalMode = "BLOCKED".equals(status)
                ? "SESSION_BOOTSTRAP_LOCKDOWN"
                : "DEGRADED".equals(status)
                ? "SESSION_BOOTSTRAP_GATED"
                : "SESSION_BOOTSTRAP_READY";
        List<String> reasons = new ArrayList<>();
        if ("BLOCKED".equals(profileGate)) {
            reasons.add("O perfil jurídico atual não pode operar esta capability crítica nesta sessão.");
        } else if ("DEGRADED".equals(profileGate)) {
            reasons.add("A capability entrou em gate por perfil e exige rota assistida e surface reduzida.");
        }
        if ("BLOCKED".equals(sigiloFence)) {
            reasons.add("A sessão caiu em fence de sigilo incompatível com o perfil ou com o histórico operacional retido.");
        } else if ("DEGRADED".equals(sigiloFence)) {
            reasons.add("A sessão exige fence reforçada de sigilo antes de manter reuse automático nesta capability.");
        }
        if (repeatedDriftDetected) {
            reasons.add("Drift repetido da sessão bloqueou bootstrap automático de capability crítica até replay vencedor.");
        }
        if (mandatoryCoverageMissing) {
            reasons.add("A capability crítica não ficou coberta pelo conjunto mínimo de skills/examples exigido para este perfil.");
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("userProfile", request == null ? null : request.userProfile());
        diagnostics.put("criticalCapability", criticalCapability);
        diagnostics.put("retainedTurnCount", retainedTurnCount);
        diagnostics.put("profileGate", profileGate);
        diagnostics.put("sigiloFence", sigiloFence);
        diagnostics.put("sessionDoctorStatus", sessionDoctor == null ? null : sessionDoctor.status());
        diagnostics.put("sessionDoctorOperationalMode", sessionDoctor == null ? null : sessionDoctor.operationalMode());
        diagnostics.put("repeatedDriftDetected", repeatedDriftDetected);
        diagnostics.put("mandatorySkillIds", mandatorySkillIds);
        diagnostics.put("mandatoryToolExampleIds", mandatoryToolExampleIds);
        diagnostics.put("missingSkillIds", List.copyOf(missingSkillIds));
        diagnostics.put("missingToolExampleIds", List.copyOf(missingToolExampleIds));
        diagnostics.put("pinnedSkillIds", pinnedSkills);
        diagnostics.put("pinnedToolExampleIds", pinnedExamples);
        diagnostics.put("documentSecurityStatus", documentSecurity == null ? null : documentSecurity.status());
        return new LegalAiConversationSessionBootstrapSnapshot(
                status,
                blockedCapability,
                repeatedDriftDetected,
                operationalMode,
                profileGate,
                sigiloFence,
                mandatorySkillIds,
                mandatoryToolExampleIds,
                List.copyOf(missingSkillIds),
                List.copyOf(missingToolExampleIds),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private String resolveProfileGate(String profile, String capability) {
        if (capability == null) {
            return "READY";
        }
        if (profile == null) {
            return isCriticalCapability(capability, null) ? "DEGRADED" : "READY";
        }
        boolean decisory = capability.contains("DECISAO") || capability.contains("SENTENCA") || capability.contains("VOTO") || capability.contains("PLENARIA");
        boolean petitioning = capability.contains("PETICAO") || capability.contains("PROTOCOLO") || capability.contains("RECURSAL") || capability.contains("MINUTA") || capability.contains("PARECER");
        if ("CIDADAO".equals(profile)) {
            return petitioning || decisory ? "BLOCKED" : "READY";
        }
        if ("ADVOGADO".equals(profile) || profile.contains("DEFENSOR")) {
            return decisory ? "BLOCKED" : "READY";
        }
        if (profile.contains("MAGISTRADO") || "JUIZ".equals(profile) || "DESEMBARGADOR".equals(profile) || "MINISTRO".equals(profile)) {
            return "READY";
        }
        if (profile.contains("SERVIDOR") || profile.contains("SECRETARIA")) {
            return decisory ? "DEGRADED" : petitioning ? "DEGRADED" : "READY";
        }
        if (profile.contains("PROMOTOR") || profile.contains("PROCURADOR") || profile.contains("MP")) {
            return decisory ? "BLOCKED" : "READY";
        }
        return isCriticalCapability(capability, null) ? "DEGRADED" : "READY";
    }

    private String resolveSigiloFence(LegalAiConversationRequest request,
                                      String profile,
                                      LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                      LegalAiConversationSessionDoctorSnapshot sessionDoctor) {
        String sigilo = normalize(contextValue(request, "sigilo"));
        if (sigilo == null || sigilo.contains("public")) {
            return "READY";
        }
        if ("CIDADAO".equals(profile)) {
            return "BLOCKED";
        }
        if (documentSecurity != null && "HUMAN_REVIEW_REQUIRED".equalsIgnoreCase(documentSecurity.status())) {
            return "BLOCKED";
        }
        if (sessionDoctor != null && (sessionDoctor.blockedSurface() || sessionDoctor.driftDetected())) {
            return "DEGRADED";
        }
        return "DEGRADED";
    }

    private List<String> mandatorySkillIds(String capability) {
        if (capability == null) {
            return List.of();
        }
        if (capability.contains("RECURSAL")) {
            return List.of("LEGAL_SKILL_RECURSAL");
        }
        if (capability.contains("PETICAO") || capability.contains("PROTOCOLO")) {
            return List.of("LEGAL_SKILL_PROTOCOL");
        }
        if (capability.contains("DECISAO") || capability.contains("SENTENCA") || capability.contains("VOTO")) {
            return List.of("LEGAL_SKILL_DECISORY_DRAFT");
        }
        if (capability.contains("PARECER")) {
            return List.of("LEGAL_SKILL_PARECER");
        }
        return List.of();
    }

    private List<String> mandatoryToolExampleIds(String capability) {
        if (capability == null) {
            return List.of();
        }
        if (capability.contains("RECURSAL")) {
            return List.of("EXAMPLE_RECURSO");
        }
        if (capability.contains("PETICAO") || capability.contains("PROTOCOLO")) {
            return List.of("EXAMPLE_PROTOCOLO");
        }
        if (capability.contains("DECISAO") || capability.contains("SENTENCA") || capability.contains("VOTO")) {
            return List.of("EXAMPLE_DECISORIO");
        }
        if (capability.contains("PARECER")) {
            return List.of("EXAMPLE_PARECER");
        }
        return List.of();
    }

    private boolean isCriticalCapability(String capability, String message) {
        String combined = (capability == null ? "" : capability) + ' ' + (message == null ? "" : message);
        String normalized = normalize(combined);
        if (normalized == null) {
            return false;
        }
        return normalized.contains("recurs")
                || normalized.contains("petic")
                || normalized.contains("protocolo")
                || normalized.contains("minuta")
                || normalized.contains("parecer")
                || normalized.contains("decis")
                || normalized.contains("sentenc")
                || normalized.contains("voto");
    }

    private int countReasonOccurrences(List<String> reasons, String token) {
        if (reasons == null || token == null || token.isBlank()) {
            return 0;
        }
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return (int) reasons.stream()
                .filter(reason -> reason != null && reason.toLowerCase(Locale.ROOT).contains(normalizedToken))
                .count();
    }

    private String contextValue(LegalAiConversationRequest request, String key) {
        if (request == null || request.context() == null || key == null) {
            return null;
        }
        Object value = request.context().get(key);
        return value == null ? null : String.valueOf(value);
    }

    @SuppressWarnings("unchecked")
    private List<String> listValue(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
