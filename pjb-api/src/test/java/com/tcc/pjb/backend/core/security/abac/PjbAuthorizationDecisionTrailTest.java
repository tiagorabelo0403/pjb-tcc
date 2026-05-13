package com.tcc.pjb.backend.core.security.abac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import org.junit.jupiter.api.Test;

class PjbAuthorizationDecisionTrailTest {

    @Test
    void mustGenerateStableAuditMetadata() {
        PjbAuthorizationDecisionTrail trail = new PjbAuthorizationDecisionTrail(
                "READ_PROCESSO",
                "PROCESSO",
                "0001234-55.2026.8.06.0001",
                false,
                "step_up_mfa_requerido",
                "abac-v1.0",
                "sha256-policy",
                99L,
                "CIDADAO",
                "req-1",
                "consulta motivada",
                NivelSigilo.SIGILO_N2,
                PjbAuthorizationRiskLevel.CRITICO,
                92,
                PjbAuthorizationStepUpAssessment.requiredButMissing("MFA", "step_up_mfa_requerido", "MFA requerido"),
                PjbAuthorizationGovernanceAssessment.requiredButMissing("DELEGATION", "delegation_required", "INFOJUD", "Delegação requerida")
        );
        assertEquals("AUTHZ_READ_PROCESSO_DENY", trail.auditEventCode());
        assertNotNull(trail.payloadHash());
        assertFalse(trail.payloadHash().isBlank());
        assertTrue(trail.auditDescription().contains("allowed=false"));
        assertTrue(trail.auditDescription().contains("risk=CRITICO:92"));
        assertTrue(trail.auditDescription().contains("governance=DELEGATION:DELEGATION_REQUIRED:INFOJUD:true:false"));
    }

    @Test
    void governanceAssessmentMustExposeDeniedState() {
        PjbAuthorizationGovernanceAssessment assessment = PjbAuthorizationGovernanceAssessment.requiredButMissing(
                "FORMAL_CONTEXT",
                "formal_context_required",
                "ENDERECO_ESTRITO",
                "Contexto formal requerido"
        );
        assertTrue(assessment.deniedByGovernance());
        assertEquals("FORMAL_CONTEXT", assessment.channel());
        assertEquals("ENDERECO_ESTRITO", assessment.scope());
    }

    @Test
    void riskLevelMustRespectScoreBands() {
        assertEquals(PjbAuthorizationRiskLevel.BAIXO, PjbAuthorizationRiskLevel.fromScore(10));
        assertEquals(PjbAuthorizationRiskLevel.MODERADO, PjbAuthorizationRiskLevel.fromScore(30));
        assertEquals(PjbAuthorizationRiskLevel.ALTO, PjbAuthorizationRiskLevel.fromScore(60));
        assertEquals(PjbAuthorizationRiskLevel.CRITICO, PjbAuthorizationRiskLevel.fromScore(85));
    }
}
