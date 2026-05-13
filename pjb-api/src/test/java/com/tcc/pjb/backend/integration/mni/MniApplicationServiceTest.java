package com.tcc.pjb.backend.integration.mni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.mni.application.MniRecepcaoService;
import com.tcc.pjb.backend.integration.mni.application.MniRemessaService;
import com.tcc.pjb.backend.integration.mni.domain.MniEndpointHealthView;
import com.tcc.pjb.backend.integration.mni.domain.MniRecepcaoTimelineEntry;
import com.tcc.pjb.backend.integration.mni.domain.MniReprocessamentoSummary;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MniApplicationServiceTest {

    @Test
    void reprocessar_deveAuditarExecucaoManual() {
        MniRemessaService remessaService = mock(MniRemessaService.class);
        MniRecepcaoService recepcaoService = mock(MniRecepcaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(remessaService.reprocessarPendentes(new com.tcc.pjb.backend.integration.mni.domain.MniRemessaBatchCommand(10)))
                .thenReturn(new MniReprocessamentoSummary(2, 1, 0));
        MniApplicationService applicationService = new MniApplicationService(remessaService, recepcaoService, auditLedgerService);

        var result = applicationService.reprocessar(10);

        assertThat(result.processadas()).isEqualTo(2);
        verify(auditLedgerService).appendSafely(eq("MNI_REPROCESSAMENTO_RUN"), eq("MNI"), eq("10"), isNull(), eq("processadas=2 superseded=1"));
    }

    @Test
    void recepcaoTimeline_deveAuditarQuantidade() {
        MniRemessaService remessaService = mock(MniRemessaService.class);
        MniRecepcaoService recepcaoService = mock(MniRecepcaoService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(recepcaoService.timeline(4L)).thenReturn(List.of(
                new MniRecepcaoTimelineEntry("RECEIVED", Instant.parse("2026-04-11T12:00:00Z"), "hash-1"),
                new MniRecepcaoTimelineEntry("PROCESSED", Instant.parse("2026-04-11T12:10:00Z"), "hash-1")
        ));
        MniApplicationService applicationService = new MniApplicationService(remessaService, recepcaoService, auditLedgerService);

        var result = applicationService.recepcaoTimeline(4L);

        assertThat(result).hasSize(2);
        verify(auditLedgerService).appendSafely(eq("MNI_RECEPCAO_TIMELINE_QUERY"), eq("MNI_RECEPCAO"), eq("4"), isNull(), eq("entries=2"));
    }
}
