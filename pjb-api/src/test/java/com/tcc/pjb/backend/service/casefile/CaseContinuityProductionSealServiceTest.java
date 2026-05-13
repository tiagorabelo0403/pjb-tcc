package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.processo.lifecycle.ProcessoLifecycleAction;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityDecisionGateResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityProductionSealLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityRemediationResponse;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseContinuityProductionSealServiceTest {

    @Test
    void shouldBlockSealWhenCriticalGateFails() {
        CaseContinuityReadinessService readinessService = mock(CaseContinuityReadinessService.class);
        CaseContinuityIntegrationService integrationService = mock(CaseContinuityIntegrationService.class);
        CaseContinuityRemediationService remediationService = mock(CaseContinuityRemediationService.class);
        CaseContinuityDecisionGateService decisionGateService = mock(CaseContinuityDecisionGateService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityProductionSealService service = new CaseContinuityProductionSealService(
                readinessService,
                integrationService,
                remediationService,
                decisionGateService,
                auditLedgerService,
                metrics
        );

        when(readinessService.snapshot(99L)).thenReturn(new CaseContinuityReadinessResponse(
                Instant.now(), 31L, 99L,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityReadinessLevel.ALERTA,
                true,
                5L, 1L, 4L, 1L,
                List.of("PROFERIR_VOTO[RECURSAL]->PROFERIR_VOTO"),
                List.of("ARQUIVAR[TERMINAL]->ARQUIVAR"),
                List.of("PROFERIR_VOTO#WRITE_JUDICIAL_ACT"),
                List.of("ARQUIVAR#ARCHIVE_CASE"),
                List.of(), List.of(), List.of()
        ));
        when(integrationService.snapshot(99L)).thenReturn(new CaseContinuityIntegrationResponse(
                Instant.now(), 31L, 99L,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityReadinessLevel.ALERTA,
                true,
                true,
                true,
                true,
                true,
                true,
                "v3",
                List.of("payments", "risk"),
                List.of("EDCL"),
                List.of(),
                java.util.Map.of("EDCL", "Embargos de declaração"),
                List.of(), List.of(), List.of()
        ));
        when(remediationService.snapshot(99L)).thenReturn(new CaseContinuityRemediationResponse(
                Instant.now(), 31L, 99L,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityReadinessLevel.ALERTA,
                true,
                true,
                1L,
                1L,
                0L,
                List.of("Sincronizar proceedings"),
                List.of(),
                List.of(),
                List.of(),
                List.of("Saneamento leve")
        ));

        for (ProcessoLifecycleAction action : List.of(
                ProcessoLifecycleAction.ASSINAR_DESPACHO,
                ProcessoLifecycleAction.PROFERIR_SENTENCA,
                ProcessoLifecycleAction.PROFERIR_VOTO,
                ProcessoLifecycleAction.LAVRAR_ACORDAO,
                ProcessoLifecycleAction.CERTIFICAR_TRANSITO,
                ProcessoLifecycleAction.INICIAR_CUMPRIMENTO,
                ProcessoLifecycleAction.ARQUIVAR
        )) {
            boolean allow = action != ProcessoLifecycleAction.PROFERIR_VOTO;
            when(decisionGateService.snapshot(99L, action)).thenReturn(new CaseContinuityDecisionGateResponse(
                    Instant.now(), 31L, 99L,
                    action,
                    action.name(),
                    CaseContinuityTrack.RECURSAL,
                    CaseContinuityTrack.RECURSAL,
                    CaseContinuityReadinessLevel.ALERTA,
                    allow,
                    true,
                    allow,
                    true,
                    allow,
                    true,
                    true,
                    List.of(),
                    allow ? List.of() : List.of("Gate bloqueou o voto"),
                    List.of("Revisar gate")
            ));
        }

        var response = service.snapshot(99L);

        assertThat(response.sealLevel()).isEqualTo(CaseContinuityProductionSealLevel.BLOQUEADO);
        assertThat(response.blockedActions()).contains("PROFERIR_VOTO");
        assertThat(response.blockedSensitiveActions()).isGreaterThan(0);
        verify(metrics).recordProductionSeal(response);
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_PRODUCTION_SEAL"), eq("CASE_FILE"), eq("31"), any(String.class));
    }
}
