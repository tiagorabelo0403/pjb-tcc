package com.tcc.pjb.backend.core.quality.apisurface;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class PjbInstitutionalActivationAlignmentHardeningTest {

    @Test
    void institutionalDataPlaneFilterMustEvaluateActivationUsingSameAffiliationAndNominationContext() {
        String filter = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/configs/datasource/PjbInstitutionalDataPlaneFilter.java"));
        assertTrue(filter.contains("entryActivationDecisionApplicationService.avaliarEntradaAtual(affiliationId, nominationId)"));
    }

    @Test
    void activationDecisionServiceMustExposeExplicitNominationAwareOverload() {
        String service = ApiSurfaceTestSupport.read(Path.of("src/main/java/com/tcc/pjb/backend/core/comunicacao/institucional/entry/application/InstitutionalEntryActivationDecisionApplicationService.java"));
        assertTrue(service.contains("public InstitutionalEntryActivationBundle avaliarEntradaAtual(String affiliationId, String nominationId)"));
        assertTrue(service.contains("nominationRepository.findByNominationId(explicitNominationId.trim())"));
    }
}
