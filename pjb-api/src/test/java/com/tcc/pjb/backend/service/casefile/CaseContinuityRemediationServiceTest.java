package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityIntegrationResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
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

class CaseContinuityRemediationServiceTest {

    @Test
    void shouldExposeAutomatedAndManualRepairTracks() {
        CaseContinuityObservabilityService observabilityService = mock(CaseContinuityObservabilityService.class);
        CaseContinuityConsistencyService consistencyService = mock(CaseContinuityConsistencyService.class);
        CaseContinuityReadinessService readinessService = mock(CaseContinuityReadinessService.class);
        CaseContinuityIntegrationService integrationService = mock(CaseContinuityIntegrationService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityRemediationService service = new CaseContinuityRemediationService(
                observabilityService,
                consistencyService,
                readinessService,
                integrationService,
                auditLedgerService,
                metrics
        );

        when(observabilityService.snapshot(77L)).thenReturn(new CaseContinuityObservabilityResponse(
                Instant.now(), 19L, 77L, 77L, "ROOT",
                CaseContinuityTrack.RECURSAL,
                4L, 1L, 2L,
                1L, 0L, 0L, 0L, 1L, 2L,
                Instant.now(),
                java.util.Map.of("RECURSAL", 2L), java.util.Map.of("RECURSAL", 1L), java.util.Map.of("RECURSO_INTERPOSTO", 2L),
                List.of("CASE_CONTINUITY_SYNCED"),
                List.of("warn-obs"),
                true,
                true
        ));
        when(consistencyService.snapshot(77L)).thenReturn(new CaseContinuityConsistencyResponse(
                Instant.now(), 19L, 77L,
                CaseContinuityTrack.RECURSAL,
                false,
                4L, 2L, 1L, 1L, 0L, 1L, 0L, 1L,
                List.of("warn-cons"),
                List.of("block-cons"),
                List.of("fix-cons")
        ));
        when(readinessService.snapshot(77L)).thenReturn(new CaseContinuityReadinessResponse(
                Instant.now(), 19L, 77L,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityTrack.TRANSITO,
                CaseContinuityReadinessLevel.ALERTA,
                false,
                2L, 1L, 1L, 1L,
                List.of("PROFERIR_VOTO[RECURSAL]->PROFERIR_VOTO"),
                List.of("CERTIFICAR_TRANSITO[TERMINAL]->CERTIFICAR_TRANSITO"),
                List.of("PROFERIR_VOTO#WRITE_JUDICIAL_ACT"),
                List.of("CERTIFICAR_TRANSITO#CERTIFY_TRANSIT"),
                List.of("warn-read"),
                List.of("block-read"),
                List.of("fix-read")
        ));
        when(integrationService.snapshot(77L)).thenReturn(new CaseContinuityIntegrationResponse(
                Instant.now(), 19L, 77L,
                CaseContinuityTrack.RECURSAL,
                CaseContinuityTrack.TRANSITO,
                CaseContinuityReadinessLevel.ALERTA,
                false,
                false,
                false,
                false,
                true,
                false,
                "v3",
                List.of("payments"),
                List.of("EDCL"),
                List.of("RESP"),
                java.util.Map.of("EDCL", "Embargos de declaração"),
                List.of("warn-int"),
                List.of("block-int"),
                List.of("fix-int")
        ));

        var response = service.snapshot(77L);

        assertThat(response.healthy()).isFalse();
        assertThat(response.autoRepairEligible()).isFalse();
        assertThat(response.automatedRepairActions()).isNotEmpty();
        assertThat(response.manualRepairActions()).isNotEmpty();
        assertThat(response.manualRepairActions()).anyMatch(item -> item.contains("malha recursal"));
        verify(metrics).recordRemediation(response);
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_REMEDIATION_INSPECT"), eq("CASE_FILE"), eq("19"), any(String.class));
    }
}
