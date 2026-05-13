package com.tcc.pjb.backend.ai.juridica.spine;

import com.tcc.pjb.backend.ai.juridica.schema.LegalAiStructuredSchemaCatalog;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.dto.ai.legal.LegalHallucinationGuardRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JuridicaHallucinationGuardServiceTest {

    @Test
    void mustBlockUngroundedArticleAndPrecedentClaims() {
        var service = new JuridicaHallucinationGuardService(spineService());

        var response = service.evaluate(new LegalHallucinationGuardRequest(
                "Conforme art. 489 do CPC e jurisprudencia pacifica do STJ, o caso esta resolvido.",
                "civel",
                "comum",
                "apelacao",
                List.of(),
                Map.of("sigilo", false)
        ));

        assertEquals("BLOCKED", response.status());
        assertTrue(response.articleReferenceVerificationRequired());
        assertTrue(response.precedentVerificationRequired());
        assertFalse(response.blockedReasons().isEmpty());
    }

    @Test
    void mustAllowGroundedClaimsAndKeepPlaceholderPolicy() {
        var service = new JuridicaHallucinationGuardService(spineService());

        var response = service.evaluate(new LegalHallucinationGuardRequest(
                "Fundamento confirmado: art. 489 do CPC. Precedente confirmado: Tema 123.",
                "civel",
                "comum",
                "apelacao",
                List.of("art. 489 CPC", "Tema 123"),
                Map.of("sigilo", false)
        ));

        assertEquals("ALIGNED", response.status());
        assertEquals("[NAO_CONFIRMADO]", response.unresolvedCitationPlaceholder());
        assertTrue(response.freeFormCitationBlocked());
    }

    private JuridicaLegalAiSpineService spineService() {
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
}
