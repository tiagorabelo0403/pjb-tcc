package com.tcc.pjb.backend.core.eleitoral;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralCalendarioHealthView;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoTimelineEntry;
import com.tcc.pjb.backend.core.eleitoral.domain.EleitoralFeitoTimelineResult;
import com.tcc.pjb.backend.model.entity.eleitoral.ProcessoZonaEleitoral;
import com.tcc.pjb.backend.model.repository.ProcessoZonaEleitoralRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class EleitoralApplicationServiceTest {

    @Test
    void timeline_deveDelegarEAuditarQuantidadeDeEntradas() {
        FeitoEleitoralService feitoService = mock(FeitoEleitoralService.class);
        ProcessoZonaEleitoralRepository zonaRepository = mock(ProcessoZonaEleitoralRepository.class);
        FeitoEleitoralDiplomacaoScheduler scheduler = mock(FeitoEleitoralDiplomacaoScheduler.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(feitoService.timeline(any())).thenReturn(new EleitoralFeitoTimelineResult(
                22L,
                List.of(
                        new EleitoralFeitoTimelineEntry("CRIADO", Instant.parse("2026-04-11T12:00:00Z"), "AIJE"),
                        new EleitoralFeitoTimelineEntry("DIPLOMADO", Instant.parse("2026-10-06T03:00:00Z"), "DIPLOMADO"))));
        EleitoralApplicationService applicationService = new EleitoralApplicationService(
                feitoService,
                zonaRepository,
                scheduler,
                new EleitoralTseProperties(true, "spca", "resultado", true, null),
                auditLedgerService);

        var result = applicationService.timeline(22L);

        assertThat(result.entries()).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("ELEITORAL_TIMELINE_QUERY"), eq("PROCESSO"), eq("22"), isNull(), eq("entries=2"));
    }

    @Test
    void zonaHealthResult_deveAuditarCriterioEStatus() {
        FeitoEleitoralService feitoService = mock(FeitoEleitoralService.class);
        ProcessoZonaEleitoralRepository zonaRepository = mock(ProcessoZonaEleitoralRepository.class);
        FeitoEleitoralDiplomacaoScheduler scheduler = mock(FeitoEleitoralDiplomacaoScheduler.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(zonaRepository.findById(14L)).thenReturn(Optional.of(ProcessoZonaEleitoral.builder()
                .processoId(14L)
                .zonaEleitoral("083")
                .municipio("Fortaleza")
                .uf("CE")
                .cartorioCodigo("ZE083")
                .updatedAt(Instant.parse("2026-04-11T12:00:00Z"))
                .build()));
        EleitoralApplicationService applicationService = new EleitoralApplicationService(
                feitoService,
                zonaRepository,
                scheduler,
                new EleitoralTseProperties(true, "spca", "resultado", false, null),
                auditLedgerService);

        var result = applicationService.zonaHealthResult(14L, "cartorio");

        assertThat(result.ok()).isTrue();
        verify(auditLedgerService).appendSafely(eq("ELEITORAL_ZONA_HEALTH_QUERY"), eq("PROCESSO"), eq("14"), isNull(), eq("criterio=cartorio status=OK"));
    }

    @Test
    void diplomacaoSyncRun_deveExecutarSchedulerEAuditarDryRun() {
        FeitoEleitoralService feitoService = mock(FeitoEleitoralService.class);
        ProcessoZonaEleitoralRepository zonaRepository = mock(ProcessoZonaEleitoralRepository.class);
        FeitoEleitoralDiplomacaoScheduler scheduler = mock(FeitoEleitoralDiplomacaoScheduler.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(feitoService.listarFeitosPendentesDeDiplomacao()).thenReturn(List.of());
        doNothing().when(scheduler).sincronizarDiplomacoes();
        EleitoralApplicationService applicationService = new EleitoralApplicationService(
                feitoService,
                zonaRepository,
                scheduler,
                new EleitoralTseProperties(true, "spca", "resultado", true, null),
                auditLedgerService);

        var result = applicationService.diplomacaoSyncRun();

        assertThat(result.dryRun()).isTrue();
        verify(auditLedgerService).appendSafely(eq("ELEITORAL_DIPLOMACAO_SYNC_RUN"), eq("ELEITORAL"), eq("DIPLOMACAO"), isNull(), eq("dryRun=true pendentesAntes=0"));
    }

    @Test
    void calendarioHealth_deveNormalizarUfEAuditarStatus() {
        FeitoEleitoralService feitoService = mock(FeitoEleitoralService.class);
        ProcessoZonaEleitoralRepository zonaRepository = mock(ProcessoZonaEleitoralRepository.class);
        FeitoEleitoralDiplomacaoScheduler scheduler = mock(FeitoEleitoralDiplomacaoScheduler.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(feitoService.calendarioHealth(any())).thenReturn(new EleitoralCalendarioHealthView("CE", LocalDate.of(2026, 10, 1), true, "DIPLOMACAO"));
        EleitoralApplicationService applicationService = new EleitoralApplicationService(
                feitoService,
                zonaRepository,
                scheduler,
                new EleitoralTseProperties(true, "spca", "resultado", false, null),
                auditLedgerService);

        var result = applicationService.calendarioHealth("ce", LocalDate.of(2026, 10, 1));

        assertThat(result.uf()).isEqualTo("CE");
        verify(auditLedgerService).appendSafely(eq("ELEITORAL_CALENDARIO_HEALTH_QUERY"), eq("ELEITORAL_CALENDARIO"), eq("CE"), isNull(), eq("status=DIPLOMACAO"));
    }
}
