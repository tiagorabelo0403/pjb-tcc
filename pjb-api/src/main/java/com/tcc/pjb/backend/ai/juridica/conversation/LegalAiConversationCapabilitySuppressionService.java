package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilityRecurrenceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationCapabilitySuppressionSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionBootstrapSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationSessionDoctorSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiConversationCapabilitySuppressionService {

    public LegalAiConversationCapabilitySuppressionSnapshot inspect(LegalAiConversationRequest request,
                                                                    String capability,
                                                                    String version,
                                                                    LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                                    LegalAiConversationToolScopeSnapshot toolScope,
                                                                    LegalAiConversationSessionDoctorSnapshot sessionDoctor,
                                                                    LegalAiConversationSessionBootstrapSnapshot sessionBootstrap,
                                                                    LegalAiConversationCapabilityRecurrenceSnapshot capabilityRecurrence) {
        String processClass = resolveProcessClass(request);
        String sigiloLevel = resolveSigiloLevel(request);
        boolean processScoped = request != null && request.processoId() != null && !request.processoId().isBlank();
        boolean strictClass = isStrictClass(processClass);
        boolean elevatedClass = strictClass || isElevatedClass(processClass);
        boolean sigiloSensitive = isSensitiveSigilo(sigiloLevel);
        boolean documentRestricted = documentSecurity != null && !"CLEARED".equalsIgnoreCase(documentSecurity.status());
        boolean doctorBlocked = sessionDoctor != null && (sessionDoctor.blockedSurface() || "BLOCKED".equalsIgnoreCase(sessionDoctor.status()));
        boolean bootstrapBlocked = sessionBootstrap != null && (sessionBootstrap.blockedCapability() || "BLOCKED".equalsIgnoreCase(sessionBootstrap.status()));
        boolean recurrenceLocked = capabilityRecurrence != null && "LOCKED".equalsIgnoreCase(capabilityRecurrence.status());
        boolean recurrenceEscalated = capabilityRecurrence != null && ("ESCALATED".equalsIgnoreCase(capabilityRecurrence.status()) || "HIGH".equalsIgnoreCase(capabilityRecurrence.riskTier()) || "CRITICAL".equalsIgnoreCase(capabilityRecurrence.riskTier()));
        boolean recurrenceDetected = capabilityRecurrence != null && capabilityRecurrence.recurrenceDetected();
        boolean suppressionDetected = elevatedClass && (sigiloSensitive || documentRestricted || recurrenceDetected || doctorBlocked || bootstrapBlocked);
        String suppressionScope = processScoped ? "PROCESS_CLASS_SIGILO" : "SESSION_CLASS_SIGILO";
        String policyTier = strictClass ? sigiloSensitive ? "STRICT_SIGILO" : "STRICT_PROCESS_CLASS" : elevatedClass ? sigiloSensitive ? "ELEVATED_SIGILO" : "ELEVATED_PROCESS_CLASS" : "BASELINE";
        LinkedHashSet<String> blockedToolIds = new LinkedHashSet<>();
        LinkedHashSet<String> elevatedStepUpToolIds = new LinkedHashSet<>();
        if (toolScope != null && toolScope.stepUpToolIds() != null) {
            elevatedStepUpToolIds.addAll(toolScope.stepUpToolIds());
        }
        if (toolScope != null && toolScope.allowedToolIds() != null && (strictClass || sigiloSensitive || recurrenceEscalated)) {
            elevatedStepUpToolIds.addAll(toolScope.allowedToolIds());
        }
        if (toolScope != null && toolScope.blockedToolIds() != null) {
            blockedToolIds.addAll(toolScope.blockedToolIds());
        }
        String status;
        String suppressionMode;
        if (!suppressionDetected) {
            status = "NOT_REQUIRED";
            suppressionMode = "NONE";
        } else if (strictClass && (sigiloSensitive || recurrenceLocked || bootstrapBlocked || doctorBlocked)) {
            if (toolScope != null && toolScope.allowedToolIds() != null) {
                blockedToolIds.addAll(toolScope.allowedToolIds());
            }
            if (toolScope != null && toolScope.stepUpToolIds() != null) {
                blockedToolIds.addAll(toolScope.stepUpToolIds());
            }
            elevatedStepUpToolIds.clear();
            status = "LOCKED";
            suppressionMode = sigiloSensitive ? "CLASS_SIGILO_HARD_LOCK" : "CLASS_HARD_LOCK";
        } else if (sigiloSensitive || documentRestricted || recurrenceEscalated) {
            status = "ESCALATED";
            suppressionMode = sigiloSensitive && strictClass ? "CLASS_SIGILO_HUMAN_REVIEW" : "CLASS_STEP_UP_GATED";
        } else {
            status = "MONITORED";
            suppressionMode = "CLASS_MONITORED";
        }
        List<String> unmetRequirements = new ArrayList<>();
        if (!elevatedClass) {
            unmetRequirements.add("PROCESS_CLASS_BASELINE_ONLY");
        }
        if (sigiloSensitive) {
            unmetRequirements.add("SIGILO_SENSITIVE_FLOW");
        }
        if (documentRestricted) {
            unmetRequirements.add("DOCUMENT_SECURITY_NOT_CLEARED");
        }
        if (doctorBlocked) {
            unmetRequirements.add("SESSION_DOCTOR_BLOCK_ACTIVE");
        }
        if (bootstrapBlocked) {
            unmetRequirements.add("SESSION_BOOTSTRAP_BLOCK_ACTIVE");
        }
        if (recurrenceEscalated || recurrenceLocked) {
            unmetRequirements.add("CAPABILITY_RECURRENCE_ESCALATED");
        }
        List<String> reasons = new ArrayList<>();
        if (!suppressionDetected) {
            reasons.add("Nenhuma supressão adaptativa adicional foi exigida para esta capability no ramo e no sigilo deste turno.");
        } else {
            reasons.add("A capability entrou em supressão adaptativa porque o ramo processual e o sigilo exigem contenção adicional antes de novo reuse automático.");
            if (strictClass) {
                reasons.add("Ramos como penal, família, infância e execução penal operam com trilha mais estrita para reduzir vazamento, mutação indevida e oscilação reincidente.");
            }
            if (sigiloSensitive) {
                reasons.add("Sigilo reforçado elevou a governança da capability para impedir promoção ingênua de tools, skills e examples.");
            }
            if (recurrenceDetected) {
                reasons.add("A reincidência material da capability/processo foi absorvida pela supressão adaptativa para evitar nova abertura instável em classe sensível.");
            }
        }
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>();
        diagnostics.put("capability", capability);
        diagnostics.put("version", version);
        diagnostics.put("processClass", processClass);
        diagnostics.put("sigiloLevel", sigiloLevel);
        diagnostics.put("policyTier", policyTier);
        diagnostics.put("strictClass", strictClass);
        diagnostics.put("elevatedClass", elevatedClass);
        diagnostics.put("sigiloSensitive", sigiloSensitive);
        diagnostics.put("documentRestricted", documentRestricted);
        diagnostics.put("doctorBlocked", doctorBlocked);
        diagnostics.put("bootstrapBlocked", bootstrapBlocked);
        diagnostics.put("recurrenceDetected", recurrenceDetected);
        diagnostics.put("recurrenceRiskTier", capabilityRecurrence == null ? null : capabilityRecurrence.riskTier());
        diagnostics.put("suppressionMode", suppressionMode);
        diagnostics.put("status", status);
        return new LegalAiConversationCapabilitySuppressionSnapshot(
                status,
                suppressionDetected,
                processScoped,
                suppressionScope,
                processClass,
                sigiloLevel,
                policyTier,
                suppressionMode,
                List.copyOf(blockedToolIds),
                List.copyOf(elevatedStepUpToolIds),
                List.copyOf(unmetRequirements),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }

    private String resolveProcessClass(LegalAiConversationRequest request) {
        String ramo = contextValue(request, "ramo");
        String classe = firstNonBlank(contextValue(request, "classe"), contextValue(request, "classeProcessual"), contextValue(request, "processClass"));
        String rito = contextValue(request, "rito");
        String candidate = firstNonBlank(ramo, classe, rito);
        if (candidate == null) {
            return "GENERAL";
        }
        String normalized = normalize(candidate);
        if (normalized.contains("penal") || normalized.contains("criminal")) {
            return "PENAL";
        }
        if (normalized.contains("famil")) {
            return "FAMILIA";
        }
        if (normalized.contains("infan") || normalized.contains("juvent")) {
            return "INFANCIA";
        }
        if (normalized.contains("execu") && normalized.contains("pen")) {
            return "EXECUCAO_PENAL";
        }
        if (normalized.contains("eleitor")) {
            return "ELEITORAL";
        }
        if (normalized.contains("militar")) {
            return "MILITAR";
        }
        if (normalized.contains("fazend") || normalized.contains("tribut")) {
            return "FAZENDA_PUBLICA";
        }
        if (normalized.contains("saud")) {
            return "SAUDE";
        }
        if (normalized.contains("trabalh")) {
            return "TRABALHISTA";
        }
        if (normalized.contains("civil") || normalized.contains("civel")) {
            return "CIVEL";
        }
        return candidate.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private String resolveSigiloLevel(LegalAiConversationRequest request) {
        String sigilo = firstNonBlank(contextValue(request, "sigilo"), contextValue(request, "sigiloNivel"), contextValue(request, "confidencialidade"));
        return sigilo == null ? "PUBLICO" : sigilo.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
    }

    private boolean isStrictClass(String processClass) {
        return "PENAL".equals(processClass)
                || "FAMILIA".equals(processClass)
                || "INFANCIA".equals(processClass)
                || "EXECUCAO_PENAL".equals(processClass);
    }

    private boolean isElevatedClass(String processClass) {
        return isStrictClass(processClass)
                || "ELEITORAL".equals(processClass)
                || "MILITAR".equals(processClass)
                || "FAZENDA_PUBLICA".equals(processClass)
                || "SAUDE".equals(processClass);
    }

    private boolean isSensitiveSigilo(String sigiloLevel) {
        String normalized = normalize(sigiloLevel);
        return normalized != null && (normalized.contains("SIGIL") || normalized.contains("RESTRIT") || normalized.contains("SEGRED") || normalized.contains("CONFID"));
    }

    private String contextValue(LegalAiConversationRequest request, String key) {
        if (request == null || request.context() == null || key == null) {
            return null;
        }
        Object value = request.context().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Normalizer.normalize(text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }
}
