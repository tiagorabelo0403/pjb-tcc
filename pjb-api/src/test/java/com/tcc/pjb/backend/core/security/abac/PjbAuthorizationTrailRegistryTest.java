package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbAuthorizationTrailRegistryTest {

    @Test
    void mustFilterInstitutionalAndIntegrationSnapshots() {
        PjbAuthorizationTrailRegistry registry = new PjbAuthorizationTrailRegistry();
        registry.register(trail(
                "INSTITUTIONAL_BOX_CAPABILITY",
                "CAIXA_INSTITUCIONAL",
                "TJCE:INBOX:RECEBER:EXP-1",
                true,
                PjbAuthorizationGovernanceAssessment.satisfied("INSTITUTIONAL_MEMBERSHIP", "institutional_capability_satisfied", "RECEBER"),
                PjbAuthorizationStepUpAssessment.notRequired("NONE", "institutional_capability")
        ));
        registry.register(trail(
                "REQUEST_INFOJUD",
                "INTEGRACAO_EXTERNA",
                "INFOJUD",
                false,
                PjbAuthorizationGovernanceAssessment.requiredButMissing("DELEGATION", "delegation_required", "INFOJUD", "Delegação requerida"),
                PjbAuthorizationStepUpAssessment.notRequired("NONE", "INFOJUD")
        ));

        Instant inicioJanela = Instant.now().minusSeconds(60);
        Instant fimJanela = Instant.now().plusSeconds(60);

        List<PjbAuthorizationTrailSnapshot> institucional = registry.search(new PjbAuthorizationTrailQueryCriteria(
                null,
                "CAIXA_INSTITUCIONAL",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "TJCE",
                "INBOX",
                "RECEBER",
                null,
                null,
                null,
                null,
                null,
                null,
                inicioJanela,
                fimJanela,
                50
        ));
        List<PjbAuthorizationTrailSnapshot> integracoes = registry.search(new PjbAuthorizationTrailQueryCriteria(
                null,
                "INTEGRACAO_EXTERNA",
                null,
                null,
                null,
                null,
                null,
                null,
                "INFOJUD",
                null,
                null,
                null,
                "DELEGATION",
                "INFOJUD",
                null,
                null,
                null,
                null,
                inicioJanela,
                fimJanela,
                50
        ));

        assertEquals(1, institucional.size());
        assertEquals("RECEBER", institucional.getFirst().institutionalCapabilityCode());
        assertEquals(1, integracoes.size());
        assertEquals("INFOJUD", integracoes.getFirst().integrationCode());
        assertTrue(integracoes.getFirst().governanceRequired());
    }

    private PjbAuthorizationDecisionTrail trail(String action,
                                                String resourceType,
                                                String resourceId,
                                                boolean allowed,
                                                PjbAuthorizationGovernanceAssessment governance,
                                                PjbAuthorizationStepUpAssessment stepUp) {
        return new PjbAuthorizationDecisionTrail(
                action,
                resourceType,
                resourceId,
                allowed,
                allowed ? "granted" : "denied",
                "abac-v1",
                "sha256-policy",
                41L,
                "SERVIDOR",
                "req-1",
                "teste",
                NivelSigilo.PUBLICO,
                allowed ? PjbAuthorizationRiskLevel.MODERADO : PjbAuthorizationRiskLevel.ALTO,
                allowed ? 35 : 70,
                stepUp,
                governance
        );
    }
}
