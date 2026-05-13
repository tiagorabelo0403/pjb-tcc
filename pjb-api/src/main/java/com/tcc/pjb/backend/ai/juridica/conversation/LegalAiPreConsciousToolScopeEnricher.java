package com.tcc.pjb.backend.ai.juridica.conversation;

import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiConversationToolScopeSnapshot;
import com.tcc.pjb.backend.model.dto.ai.legal.conversation.LegalAiPreConsciousFrameSnapshot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class LegalAiPreConsciousToolScopeEnricher {

    public LegalAiConversationToolScopeSnapshot enrich(LegalAiConversationToolScopeSnapshot snapshot,
                                                       LegalAiPreConsciousFrameSnapshot frame) {
        if (snapshot == null || frame == null) {
            return snapshot;
        }
        LinkedHashSet<String> allowed = new LinkedHashSet<>(snapshot.allowedToolIds() == null ? List.of() : snapshot.allowedToolIds());
        LinkedHashSet<String> blocked = new LinkedHashSet<>(snapshot.blockedToolIds() == null ? List.of() : snapshot.blockedToolIds());
        LinkedHashSet<String> stepUp = new LinkedHashSet<>(snapshot.stepUpToolIds() == null ? List.of() : snapshot.stepUpToolIds());
        List<String> reasons = new ArrayList<>(snapshot.reasons() == null ? List.of() : snapshot.reasons());
        LinkedHashMap<String, Object> diagnostics = new LinkedHashMap<>(snapshot.diagnostics() == null ? Map.of() : snapshot.diagnostics());
        putFrame(diagnostics, frame);
        if (!frame.responseAllowed()) {
            blocked.addAll(allowed);
            blocked.addAll(stepUp);
            allowed.clear();
            stepUp.clear();
            reasons.add("Moldura pré-consciente jurídica bloqueou ferramentas por risco material antes da resposta.");
            return scope("PRE_CONSCIOUS_BLOCKED", allowed, blocked, stepUp, reasons, diagnostics);
        }
        if (frame.humanReviewRequired()) {
            stepUp.addAll(allowed);
            reasons.add("Moldura pré-consciente jurídica elevou o turno para revisão assistida com metadados de autoridade.");
            return scope("PRE_CONSCIOUS_GATED", allowed, blocked, stepUp, reasons, diagnostics);
        }
        return scope(snapshot.status(), allowed, blocked, stepUp, reasons, diagnostics);
    }

    private void putFrame(LinkedHashMap<String, Object> diagnostics, LegalAiPreConsciousFrameSnapshot frame) {
        diagnostics.put("preConsciousStatus", frame.status());
        diagnostics.put("preConsciousMode", frame.mode());
        diagnostics.put("preConsciousAuthorityFloor", frame.authorityFloor());
        diagnostics.put("preConsciousCognitivePosture", frame.cognitivePosture());
        diagnostics.put("preConsciousRiskScore", frame.riskScore());
        diagnostics.put("preConsciousResponseAllowed", frame.responseAllowed());
        diagnostics.put("preConsciousHumanReviewRequired", frame.humanReviewRequired());
        diagnostics.put("preConsciousLearningCandidate", frame.learningCandidate());
        diagnostics.put("preConsciousLineages", frame.lineages().stream().map(item -> item.branchCode() + ':' + item.branchName()).toList());
        diagnostics.put("preConsciousDominantLenses", frame.dominantLenses());
        diagnostics.put("preConsciousAuthorityChecks", frame.authorityChecks());
        diagnostics.put("preConsciousSignals", frame.signals().stream().map(item -> item.code() + ':' + item.severity()).toList());
        diagnostics.put("preConsciousNextActions", frame.nextActions());
    }

    private LegalAiConversationToolScopeSnapshot scope(String status,
                                                       LinkedHashSet<String> allowed,
                                                       LinkedHashSet<String> blocked,
                                                       LinkedHashSet<String> stepUp,
                                                       List<String> reasons,
                                                       LinkedHashMap<String, Object> diagnostics) {
        return new LegalAiConversationToolScopeSnapshot(
                status,
                List.copyOf(allowed),
                List.copyOf(blocked),
                List.copyOf(stepUp),
                List.copyOf(reasons),
                ImmutableViewSupport.map(diagnostics)
        );
    }
}
