package com.tcc.pjb.backend.service.casefile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualCategoria;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualDescriptor;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityPolicyService;
import com.tcc.pjb.backend.core.processual.ato.AtoProcessualSecurityProfile;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CaseContinuityDecisionGateServiceTest {

    @Test
    void shouldBlockSensitiveActionWhenLifecycleAndIntegrationAreNotReady() {
        CaseContinuityReadinessService readinessService = mock(CaseContinuityReadinessService.class);
        CaseContinuityIntegrationService integrationService = mock(CaseContinuityIntegrationService.class);
        AtoProcessualSecurityPolicyService securityPolicyService = mock(AtoProcessualSecurityPolicyService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityDecisionGateService service = new CaseContinuityDecisionGateService(
                readinessService,
                integrationService,
                securityPolicyService,
                auditLedgerService,
                metrics
        );

        when(readinessService.snapshot(44L)).thenReturn(new CaseContinuityReadinessResponse(
                Instant.now(), 11L, 44L,
                CaseContinuityTrack.RECURSAL, CaseContinuityTrack.RECURSAL,
                CaseContinuityReadinessLevel.CRITICA,
                false,
                1L, 2L, 0L, 1L,
                List.of("INTERPOR_RECURSO[RECURSAL]->INTERPOR_RECURSO"),
                List.of("PROFERIR_VOTO[RECURSAL]->PROFERIR_VOTO"),
                List.of(),
                List.of("PROFERIR_VOTO#WRITE_JUDICIAL_ACT"),
                List.of("warn"),
                List.of("block"),
                List.of("fix")
        ));
        when(integrationService.snapshot(44L)).thenReturn(new CaseContinuityIntegrationResponse(
                Instant.now(), 11L, 44L,
                CaseContinuityTrack.RECURSAL, CaseContinuityTrack.RECURSAL,
                CaseContinuityReadinessLevel.CRITICA,
                false,
                true,
                false,
                false,
                true,
                true,
                "v3",
                List.of("risk"),
                List.of("EDCL"),
                List.of("RESP"),
                java.util.Map.of("EDCL", "Embargos de declaração"),
                List.of("warn-int"),
                List.of("block-int"),
                List.of("fix-int")
        ));
        when(securityPolicyService.descriptorForAction(ProcessoLifecycleAction.PROFERIR_VOTO)).thenReturn(new AtoProcessualDescriptor(
                "PROFERIR_VOTO",
                "Proferir voto",
                AtoProcessualCategoria.DECISORIO,
                WorkItemType.DECISAO,
                "Q",
                "I",
                null,
                AtoProcessualSecurityProfile.sovereignDecision()
        ));

        var response = service.snapshot(44L, ProcessoLifecycleAction.PROFERIR_VOTO);

        assertThat(response.allowed()).isFalse();
        assertThat(response.sensitive()).isTrue();
        assertThat(response.blockers()).isNotEmpty();
        assertThat(response.blockers()).anyMatch(item -> item.contains("PROFERIR_VOTO"));
        verify(metrics).recordDecisionGate(response);
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_DECISION_GATE"), eq("CASE_FILE"), eq("11"), any(String.class));
        assertThatThrownBy(() -> service.requireAllowed(44L, ProcessoLifecycleAction.PROFERIR_VOTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Gate estrutural bloqueou o ato PROFERIR_VOTO");
    }
}
