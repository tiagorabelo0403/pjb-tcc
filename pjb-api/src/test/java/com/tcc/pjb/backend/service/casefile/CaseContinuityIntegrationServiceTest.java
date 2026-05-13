package com.tcc.pjb.backend.service.casefile;

import com.tcc.pjb.backend.ai.financeira.router.FinanceiraAiVersionSelector;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.financial.ai.FinancialAiDescriptor;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityConsistencyResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityObservabilityResponse;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessLevel;
import com.tcc.pjb.backend.model.dto.processual.observability.continuity.CaseContinuityReadinessResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.casefile.CaseContinuityTrack;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaseContinuityIntegrationServiceTest {

    @Test
    void shouldExposeFragilitiesWhenPenalTrackRequiresAppealTypesOutsideCurrentMesh() {
        CaseContinuityObservabilityService observabilityService = mock(CaseContinuityObservabilityService.class);
        CaseContinuityConsistencyService consistencyService = mock(CaseContinuityConsistencyService.class);
        CaseContinuityReadinessService readinessService = mock(CaseContinuityReadinessService.class);
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        FinanceiraAiVersionSelector selector = mock(FinanceiraAiVersionSelector.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        CaseContinuityObservabilityMetrics metrics = mock(CaseContinuityObservabilityMetrics.class);
        CaseContinuityIntegrationService service = new CaseContinuityIntegrationService(
                observabilityService,
                consistencyService,
                readinessService,
                processoRepository,
                selector,
                auditLedgerService,
                metrics
        );

        when(observabilityService.snapshot(88L)).thenReturn(new CaseContinuityObservabilityResponse(
                Instant.now(), 50L, 88L, 88L, "ROOT", CaseContinuityTrack.RECURSAL, 3L, 2L, 4L,
                1L, 0L, 0L, 0L, 0L, 0L, Instant.now(),
                java.util.Map.of("RECURSAL", 1L), java.util.Map.of("RECURSAL", 1L), java.util.Map.of("RECURSO_INTERPOSTO", 1L),
                List.of("CASE_CONTINUITY_SYNCED"), List.of(), true, false
        ));
        when(consistencyService.snapshot(88L)).thenReturn(new CaseContinuityConsistencyResponse(
                Instant.now(), 50L, 88L, CaseContinuityTrack.RECURSAL, true, 3L, 1L, 0L, 0L, 0L, 0L, 0L, 0L,
                List.of(), List.of(), List.of()
        ));
        when(readinessService.snapshot(88L)).thenReturn(new CaseContinuityReadinessResponse(
                Instant.now(), 50L, 88L, CaseContinuityTrack.RECURSAL, CaseContinuityTrack.RECURSAL, CaseContinuityReadinessLevel.ALERTA,
                true, 5L, 2L, 2L, 1L,
                List.of("PROFERIR_VOTO[DECISAO]->PROFERIR_VOTO"), List.of("ARQUIVAR[TERMINAL]->ARQUIVAR"),
                List.of("PROFERIR_VOTO#JUDGMENT"), List.of("ARQUIVAR#ARCHIVE_CASE"),
                List.of(), List.of(), List.of()
        ));

        Processo processo = new Processo();
        processo.setId(88L);
        processo.setRamoDireito(RamoDireito.PENAL);
        processo.setRito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM);
        processo.setFaseAtual(FaseProcessual.RECURSAL);
        processo.setStatusProcesso(StatusProcesso.RECURSO_INTERPOSTO);
        when(processoRepository.findById(88L)).thenReturn(Optional.of(processo));
        when(selector.descriptor(ApiVersion.V3)).thenReturn(new FinancialAiDescriptor(
                "FINANCIAL_AI", ApiVersion.V3, "ok", java.util.Set.of("payments", "risk", "audit"), Instant.now(Clock.fixed(Instant.parse("2026-03-20T12:00:00Z"), ZoneOffset.UTC))
        ));

        var response = service.snapshot(88L);

        assertThat(response.recursalMatrixReady()).isFalse();
        assertThat(response.unresolvedAppealTypes()).contains("RESE", "HABEAS_CORPUS");
        assertThat(response.blockers()).isNotEmpty();
        assertThat(response.financialAiReady()).isTrue();
        verify(metrics).recordIntegration(response);
        verify(auditLedgerService).appendSafely(eq("CASE_CONTINUITY_INTEGRATION_INSPECT"), eq("CASE_FILE"), eq("50"), any(String.class));
    }
}
