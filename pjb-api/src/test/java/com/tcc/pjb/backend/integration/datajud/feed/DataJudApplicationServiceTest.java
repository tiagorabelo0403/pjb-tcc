package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedRunSummary;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalAuditView;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalProgressSnapshot;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowView;
import org.junit.jupiter.api.Test;

class DataJudApplicationServiceTest {

    @Test
    void run_deveAuditarExecucaoManual() {
        DataJudFeedService feedService = mock(DataJudFeedService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(feedService.runIncremental("TJCE")).thenReturn(new DataJudFeedRunSummary("TJCE", 5, 20L, true));
        DataJudApplicationService applicationService = new DataJudApplicationService(feedService, auditLedgerService);

        var result = applicationService.run("tjce");

        assertThat(result.totalSent()).isEqualTo(5);
        verify(auditLedgerService).appendSafely(eq("DATAJUD_RUN_MANUAL"), eq("DATAJUD"), eq("TJCE"), isNull(), eq("totalSent=5"));
    }

    @Test
    void window_deveAuditarConsulta() {
        DataJudFeedService feedService = mock(DataJudFeedService.class);
        AuditLedgerService auditLedgerService = mock(AuditLedgerService.class);
        when(feedService.windowView(new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowQuery("TJCE")))
                .thenReturn(new DataJudWindowView("TJCE", 10L, 100, true));
        DataJudApplicationService applicationService = new DataJudApplicationService(feedService, auditLedgerService);

        var result = applicationService.window("tjce");

        assertThat(result.batchSize()).isEqualTo(100);
        verify(auditLedgerService).appendSafely(eq("DATAJUD_WINDOW_QUERY"), eq("DATAJUD"), eq("TJCE"), isNull(), eq("batchSize=100"));
    }
}
