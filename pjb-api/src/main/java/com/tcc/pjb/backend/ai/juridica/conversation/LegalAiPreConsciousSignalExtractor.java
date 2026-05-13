package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.LegalValidationResponse;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationDocumentSecuritySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationEvidenceProvenanceSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationMemorySnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationRequest;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationTrustZoneSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousSignal;
import com.tcc.pjb.backend.model.dto.ai.legal.knowledge.LegalKnowledgeCoverageSnapshot;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class LegalAiPreConsciousSignalExtractor {

    public List<LegalAiPreConsciousSignal> extract(LegalAiConversationRequest request,
                                                    String capability,
                                                    LegalAiConversationMemorySnapshot memory,
                                                    LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                                    LegalAiConversationToolScopeSnapshot toolScope,
                                                    LegalAiConversationTrustZoneSnapshot trustZone,
                                                    LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                                    LegalKnowledgeCoverageSnapshot knowledgeCoverage,
                                                    LegalValidationResponse validation,
                                                    LegalHallucinationGuardResponse guard) {
        List<LegalAiPreConsciousSignal> signals = new ArrayList<>();
        String text = normalize((request == null ? "" : request.message()) + " " + capability);
        collectProceduralSignals(text, signals);
        collectSecuritySignals(documentSecurity, toolScope, trustZone, evidenceProvenance, guard, signals);
        collectKnowledgeSignals(knowledgeCoverage, validation, guard, signals);
        collectMemorySignals(memory, signals);
        if (signals.isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.medium("BASELINE_GROUNDED_TURN", "Turno apto a seguir em modo grounded-first, com autoridade jurídica mínima explícita.", "PRE_CONSCIOUS_BASELINE"));
        }
        return signals.stream().distinct().toList();
    }

    private void collectProceduralSignals(String text, List<LegalAiPreConsciousSignal> signals) {
        if (containsAny(text, "prazo", "prescricao", "decadencia", "intempest")) {
            signals.add(LegalAiPreConsciousSignal.high("PROCEDURAL_TIME_RISK", "Validar termo inicial, suspensão, interrupção e regime de prazo antes da resposta conclusiva.", "PROCEDURAL_AXIS"));
        }
        if (containsAny(text, "recurso", "apelacao", "agravo", "embargos", "especial", "extraordinario")) {
            signals.add(LegalAiPreConsciousSignal.high("APPEAL_ADMISSIBILITY_RISK", "Validar cabimento, preparo, interesse, tempestividade, competência e órgão recursal.", "RECURSAL_AXIS"));
        }
        if (containsAny(text, "tutela", "liminar", "urgencia", "evidencia")) {
            signals.add(LegalAiPreConsciousSignal.high("PROVISIONAL_REMEDY_RISK", "Separar tutela provisória, pedido definitivo, probabilidade, perigo de dano e reversibilidade.", "PROCEDURAL_AXIS"));
        }
        if (containsAny(text, "prisao", "flagrante", "preventiva", "juri", "feminicidio", "homicidio")) {
            signals.add(LegalAiPreConsciousSignal.high("PENAL_GUARANTEE_RISK", "Exigir tipicidade, autoria, cadeia de custódia, competência e contraditório antes de conclusão penal.", "PENAL_AXIS"));
        }
    }

    private void collectSecuritySignals(LegalAiConversationDocumentSecuritySnapshot documentSecurity,
                                        LegalAiConversationToolScopeSnapshot toolScope,
                                        LegalAiConversationTrustZoneSnapshot trustZone,
                                        LegalAiConversationEvidenceProvenanceSnapshot evidenceProvenance,
                                        LegalHallucinationGuardResponse guard,
                                        List<LegalAiPreConsciousSignal> signals) {
        if (documentSecurity != null && !"CLEARED".equalsIgnoreCase(documentSecurity.status())) {
            signals.add(LegalAiPreConsciousSignal.high("DOCUMENT_SECURITY_RESTRICTED", "Documento, anexo ou fonte permanece fora do estado documental liberado.", "DOCUMENT_SECURITY"));
        }
        if (toolScope != null && toolScope.blockedToolIds() != null && !toolScope.blockedToolIds().isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.medium("TOOL_SCOPE_REDUCED", "Escopo de ferramentas já está reduzido por governança anterior.", "TOOL_SCOPE"));
        }
        if (trustZone != null && "LOCKED".equalsIgnoreCase(trustZone.status())) {
            signals.add(LegalAiPreConsciousSignal.critical("TRUST_ZONE_LOCK", "Fronteira soberana bloqueou ou travou fonte, anexo, capability ou sigilo.", "TRUST_ZONE"));
        }
        if (evidenceProvenance != null && "LOCKED".equalsIgnoreCase(evidenceProvenance.status())) {
            signals.add(LegalAiPreConsciousSignal.critical("EVIDENCE_PROVENANCE_LOCK", "Cadeia de proveniência impede promoção para RAG, grounding, minuta ou recovery lane.", "EVIDENCE_PROVENANCE"));
        }
        if (guard != null && "BLOCKED".equalsIgnoreCase(guard.status())) {
            signals.add(LegalAiPreConsciousSignal.critical("HALLUCINATION_GUARD_BLOCK", "Guard de alucinação bloqueou emissão livre de fundamento, artigo ou precedente.", "HALLUCINATION_GUARD"));
        }
    }

    private void collectKnowledgeSignals(LegalKnowledgeCoverageSnapshot coverage,
                                         LegalValidationResponse validation,
                                         LegalHallucinationGuardResponse guard,
                                         List<LegalAiPreConsciousSignal> signals) {
        if (coverage != null && coverage.matchedBranches() != null && coverage.matchedBranches().isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.high("LEGAL_BRANCH_NOT_RESOLVED", "Ramo jurídico não foi identificado com cobertura material suficiente.", "KNOWLEDGE_COVERAGE"));
        }
        if (validation != null && validation.missingEvidence() != null && !validation.missingEvidence().isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.learning("MISSING_EVIDENCE", "Evidência faltante deve virar sinal de aprendizagem e saneamento antes de reutilização.", "VALIDATION_ENVELOPE"));
        }
        if (validation != null && validation.contradictions() != null && !validation.contradictions().isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.high("VALIDATION_CONTRADICTION", "Contradição detectada no envelope jurídico antes da composição da resposta.", "VALIDATION_ENVELOPE"));
        }
        if (guard != null && guard.suspiciousSignals() != null && !guard.suspiciousSignals().isEmpty()) {
            signals.add(LegalAiPreConsciousSignal.learning("SUSPICIOUS_GROUNDING_SIGNAL", "Sinais suspeitos do guard devem alimentar autocorreção supervisionada.", "HALLUCINATION_GUARD"));
        }
    }

    private void collectMemorySignals(LegalAiConversationMemorySnapshot memory, List<LegalAiPreConsciousSignal> signals) {
        if (memory != null && memory.retainedTurns() != null && memory.retainedTurns().size() > 8) {
            signals.add(LegalAiPreConsciousSignal.medium("MEMORY_CONTEXT_PRESSURE", "Memória retida exige compactação e checagem de contradição entre turnos.", "MEMORY"));
        }
        if (memory != null && memory.scopedMemory() != null && memory.scopedMemory().containsKey("contradictions")) {
            signals.add(LegalAiPreConsciousSignal.high("MEMORY_CONTRADICTION_RESONANCE", "Memória escopada indica contradição anterior no mesmo domínio de conversa.", "MEMORY"));
        }
    }

    private static boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(normalize(token))) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        String safe = value == null ? "" : value.toLowerCase(Locale.ROOT);
        return Normalizer.normalize(safe, Normalizer.Form.NFD).replaceAll("\\p{M}", "").trim();
    }
}
