package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiJuridicalLineageDescriptor;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousFrameSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousSignal;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LegalAiPreConsciousFrameService {

    private final LegalAiJuridicalLineageRegistry lineageRegistry;
    private final LegalAiPreConsciousSignalExtractor signalExtractor;

    public LegalAiPreConsciousFrameService(LegalAiJuridicalLineageRegistry lineageRegistry,
                                           LegalAiPreConsciousSignalExtractor signalExtractor) {
        this.lineageRegistry = Objects.requireNonNull(lineageRegistry, "lineageRegistry");
        this.signalExtractor = Objects.requireNonNull(signalExtractor, "signalExtractor");
    }

    public LegalAiPreConsciousFrameSnapshot inspect(LegalAiConversationRequest request,
                                                    String capability,
                                                    String version,
                                                    LegalAiConversationMemorySnapshot memory,
                                                    LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                    LegalAiConversationToolScopeSnapshot toolScope,
                                                    LegalAiConversationTrustZoneSnapshot trustZone,
                                                    LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                                    LegalKnowledgeCoverageSnapshot knowledgeCoverage,
                                                    LegalValidationResponse validation,
                                                    LegalHallucinationGuardResponse guard) {
        List<LegalAiJuridicalLineageDescriptor> lineages = lineageRegistry.resolve(request, capability);
        List<LegalAiPreConsciousSignal> signals = signalExtractor.extract(request, capability, memory, documentSecurity, toolScope, trustZone, evidenceProvenance, knowledgeCoverage, validation, guard);
        int riskScore = riskScore(signals, documentSecurity, trustZone, evidenceProvenance, validation, guard);
        String status = status(riskScore, signals);
        String mode = mode(status, riskScore);
        boolean responseAllowed = !"BLOCKED".equals(status);
        boolean humanReviewRequired = riskScore >= 45 || containsSeverity(signals, "HIGH") || containsSeverity(signals, "CRITICAL");
        boolean learningCandidate = signals.stream().anyMatch(signal -> "LEARNING".equals(signal.type())) || riskScore >= 60;
        List<String> lenses = lineages.stream().flatMap(item -> item.hermeneuticLenses().stream()).distinct().limit(12).toList();
        List<String> checks = lineages.stream().flatMap(item -> item.authorityChecks().stream()).distinct().limit(14).toList();
        String authorityFloor = authorityFloor(signals, lineages, validation, guard, evidenceProvenance);
        LinkedHashMap<String, Object> metadata = metadata(request, capability, version, riskScore, lineages, signals, documentSecurity, toolScope, trustZone, evidenceProvenance, knowledgeCoverage);
        return new LegalAiPreConsciousFrameSnapshot(
                status,
                mode,
                authorityFloor,
                cognitivePosture(status, riskScore, humanReviewRequired),
                riskScore,
                responseAllowed,
                humanReviewRequired,
                learningCandidate,
                lineages,
                signals,
                metadata.keySet().stream().toList(),
                lenses,
                checks,
                nextActions(status, authorityFloor, signals, checks),
                ImmutableViewSupport.map(metadata)
        );
    }

    private int riskScore(List<LegalAiPreConsciousSignal> signals,
                          LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                          LegalAiConversationTrustZoneSnapshot trustZone,
                          LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                          LegalValidationResponse validation,
                          LegalHallucinationGuardResponse guard) {
        int score = signals.stream().mapToInt(this::signalWeight).sum();
        score += documentSecurity != null && !"CLEARED".equalsIgnoreCase(documentSecurity.status()) ? 12 : 0;
        score += trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status()) ? 25 : 0;
        score += evidenceProvenance != null && "LOCKED".equalsIgnoreCase(evidenceProvenance.status()) ? 25 : 0;
        score += validation != null && validation.contradictions() != null ? validation.contradictions().size() * 8 : 0;
        score += validation != null && validation.missingEvidence() != null ? validation.missingEvidence().size() * 6 : 0;
        score += guard != null && "BLOCKED".equalsIgnoreCase(guard.status()) ? 25 : 0;
        return Math.max(0, Math.min(100, score));
    }

    private int signalWeight(LegalAiPreConsciousSignal signal) {
        return switch (signal.severity()) {
            case "CRITICAL" -> 18;
            case "HIGH" -> 12;
            case "MEDIUM" -> 6;
            default -> 3;
        };
    }

    private String status(int riskScore, List<LegalAiPreConsciousSignal> signals) {
        if (riskScore >= 80 || containsCode(signals, "TRUST_ZONE_LOCK") || containsCode(signals, "EVIDENCE_PROVENANCE_LOCK") || containsCode(signals, "HALLUCINATION_GUARD_BLOCK")) {
            return "BLOCKED";
        }
        if (riskScore >= 45 || containsSeverity(signals, "HIGH")) {
            return "ESCALATED";
        }
        return "READY";
    }

    private String mode(String status, int riskScore) {
        return switch (status) {
            case "BLOCKED" -> "SOVEREIGN_PRE_RESPONSE_LOCK";
            case "ESCALATED" -> riskScore >= 65 ? "ASSISTED_PHD_REVIEW" : "ASSISTED_GROUNDED_REASONING";
            default -> "GROUNDED_AUTONOMOUS_REASONING";
        };
    }

    private String authorityFloor(List<LegalAiPreConsciousSignal> signals,
                                  List<LegalAiJuridicalLineageDescriptor> lineages,
                                  LegalValidationResponse validation,
                                  LegalHallucinationGuardResponse guard,
                                  LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance) {
        if (guard != null && "BLOCKED".equalsIgnoreCase(guard.status())) {
            return "CITATION_FIRST_BLOCKING";
        }
        if (evidenceProvenance != null && "LOCKED".equalsIgnoreCase(evidenceProvenance.status())) {
            return "SOVEREIGN_EVIDENCE_CHAIN_REQUIRED";
        }
        if (containsCode(signals, "APPEAL_ADMISSIBILITY_RISK") || containsCode(signals, "PROCEDURAL_TIME_RISK")) {
            return "PROCEDURAL_ADMISSIBILITY_REQUIRED";
        }
        if (validation != null && validation.citationFirst()) {
            return "NORMATIVE_AND_PRECEDENT_SOURCE_REQUIRED";
        }
        return lineages.isEmpty() ? "GROUNDED_LEGAL_REASONING" : lineages.getFirst().branchCode() + "_AUTHORITY_REQUIRED";
    }

    private String cognitivePosture(String status, int riskScore, boolean humanReviewRequired) {
        if ("BLOCKED".equals(status)) {
            return "INHIBITORY_CONTROL_ACTIVE";
        }
        if (humanReviewRequired || riskScore >= 45) {
            return "REFLECTIVE_SYSTEM_ACTIVE";
        }
        return "FAST_SCREENING_WITH_LEGAL_GROUNDING";
    }

    private List<String> nextActions(String status, String authorityFloor, List<LegalAiPreConsciousSignal> signals, List<String> checks) {
        LinkedHashSet<String> actions = new LinkedHashSet<>();
        if ("BLOCKED".equals(status)) {
            actions.add("Bloquear emissão operacional até saneamento de evidência, trust zone ou grounding.");
        }
        if ("ESCALATED".equals(status)) {
            actions.add("Submeter resposta a revisão assistida antes de minuta, protocolo ou orientação conclusiva.");
        }
        actions.add("Aplicar piso de autoridade: " + authorityFloor + '.');
        signals.stream().map(LegalAiPreConsciousSignal::message).limit(4).forEach(actions::add);
        checks.stream().limit(4).map(item -> "Checar " + item + '.').forEach(actions::add);
        return actions.stream().toList();
    }

    private LinkedHashMap<String, Object> metadata(LegalAiConversationRequest request,
                                                   String capability,
                                                   String version,
                                                   int riskScore,
                                                   List<LegalAiJuridicalLineageDescriptor> lineages,
                                                   List<LegalAiPreConsciousSignal> signals,
                                                   LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                   LegalAiConversationToolScopeSnapshot toolScope,
                                                   LegalAiConversationTrustZoneSnapshot trustZone,
                                                   LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                                   LegalKnowledgeCoverageSnapshot coverage) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("capability", capability == null ? "" : capability);
        out.put("version", version == null ? "" : version);
        out.put("conversationId", request == null || request.conversationId() == null ? "" : request.conversationId());
        out.put("processoId", request == null || request.processoId() == null ? "" : request.processoId());
        out.put("riskScore", riskScore);
        out.put("lineageCodes", lineages.stream().map(LegalAiJuridicalLineageDescriptor::branchCode).toList());
        out.put("signalCodes", signals.stream().map(LegalAiPreConsciousSignal::code).toList());
        out.put("documentSecurityStatus", documentSecurity == null ? "" : documentSecurity.status());
        out.put("toolScopeStatus", toolScope == null ? "" : toolScope.status());
        out.put("trustZoneStatus", trustZone == null ? "" : trustZone.status());
        out.put("evidenceProvenanceStatus", evidenceProvenance == null ? "" : evidenceProvenance.status());
        out.put("knowledgeCoverageStatus", coverage == null ? "" : coverage.status());
        return out;
    }

    private boolean containsCode(List<LegalAiPreConsciousSignal> signals, String code) {
        return signals.stream().anyMatch(signal -> code.equals(signal.code()));
    }

    private boolean containsSeverity(List<LegalAiPreConsciousSignal> signals, String severity) {
        return signals.stream().anyMatch(signal -> severity.equals(signal.severity()));
    }
}
