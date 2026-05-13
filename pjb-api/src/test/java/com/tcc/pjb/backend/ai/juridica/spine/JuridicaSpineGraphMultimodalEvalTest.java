package com.tcc.pjb.backend.ai.juridica.spine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaSpineGraphMultimodalEvalTest {

    @Test
    void v1KeepsGraphDisabledAndMinimalMultimodal() {
        var graph = new JuridicaGraphProfileService().resolve(ApiVersion.V1, "LEGAL_TRIAGE_V1", Map.of());
        var multimodal = new JuridicaMultimodalProfileService().resolve(ApiVersion.V1, "LEGAL_TRIAGE_V1", Map.of());
        var evaluation = new JuridicaEvaluationProfileService().resolve(ApiVersion.V1, "LEGAL_TRIAGE_V1", Map.of());

        assertFalse(graph.enabled());
        assertFalse(multimodal.evidenceIngestionEnabled());
        assertTrue(evaluation.evalSuites().contains("GROUNDING"));
    }

    @Test
    void v3EnablesGraphMultimodalAndReplayableEvals() {
        var graph = new JuridicaGraphProfileService().resolve(ApiVersion.V3, "LEGAL_DRAFT_V3", Map.of("citationFirst", true));
        var multimodal = new JuridicaMultimodalProfileService().resolve(ApiVersion.V3, "LEGAL_DRAFT_V3", Map.of());
        var evaluation = new JuridicaEvaluationProfileService().resolve(ApiVersion.V3, "LEGAL_DRAFT_V3", Map.of("citationFirst", true));

        assertTrue(graph.enabled());
        assertTrue(graph.traversalModes().contains("THESE_TRAVERSAL"));
        assertTrue(multimodal.evidenceIngestionEnabled());
        assertTrue(multimodal.enabledModalities().contains("VIDEO"));
        assertTrue(evaluation.replayEnabled());
        assertTrue(evaluation.evalSuites().contains("SIGILO_POLICY"));
    }
}
