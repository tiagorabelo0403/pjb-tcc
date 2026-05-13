package com.tcc.pjb.backend.ai.juridica.spine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import com.tcc.pjb.backend.model.dto.ai.legal.spine.LegalAiSpineProfileResponse;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaLegalAiSpineServiceTest {

    private static JuridicaLegalAiSpineService newService() {
        return new JuridicaLegalAiSpineService(
                new JuridicaPolicyVariableService(),
                new JuridicaToolRoutingService(new com.tcc.pjb.backend.ai.juridica.mesh.JuridicaLegalToolCatalogService()),
                new JuridicaStructuredOutputProfileService(new LegalAiStructuredSchemaCatalog()),
                new JuridicaHybridRetrievalProfileService(),
                new JuridicaMemoryIsolationProfileService(),
                new JuridicaSymbolicValidationProfileService(),
                new JuridicaGraphProfileService(),
                new JuridicaMultimodalProfileService(),
                new JuridicaEvaluationProfileService(),
                new JuridicaAntiHallucinationProfileService(),
                new JuridicaTraceApprovalService()
        );
    }

    @Test
    void resolveProfileForV3Draft() {
        LegalAiSpineProfileResponse profile = newService().resolveForSurface("LEGAL_DRAFT_V3", ApiVersion.V3);

        assertEquals("LEGAL_AI_SPINE", profile.profileCode());
        assertEquals("V3", profile.version());
        assertFalse(profile.structuredOutputs().isEmpty());
        assertTrue(profile.structuredOutputs().stream().anyMatch(item -> "LEGAL_AI_DRAFT_ENVELOPE".equals(item.schemaId())));
        assertTrue(profile.trace().enabled());
        assertEquals(JuridicaSpineLabels.PIPELINE_LEGAL_HYBRID_RAG, profile.retrieval().pipelineCode());
        assertTrue(profile.memory().strictIsolation());
        assertTrue(profile.validation().evalsEnabled());
        assertTrue(profile.graph().enabled());
        assertTrue(profile.multimodal().provenanceRequired());
        assertTrue(profile.evaluation().replayEnabled());
    }

    @Test
    void resolveProfileForIaHonorsSigiloAndStepUp() {
        var request = com.tcc.pjb.backend.ai.contract.IARequest.builder()
                .withOrigem("TEST")
                .withAcao("protocolo_assistido_v3")
                .withPayload(Map.of("sigilo", true, "textoPeticaoLivre", "petição"))
                .build();

        LegalAiSpineProfileResponse profile = newService().resolveForIa(request, ApiVersion.V3, request.getAcao());

        assertTrue(profile.approval().approvalRequired());
        assertTrue(Boolean.TRUE.equals(profile.policyVariables().get("sigilo")));
        assertTrue(Boolean.TRUE.equals(profile.retrieval().retrievalPolicy().get("citationFirst")));
    }
}
