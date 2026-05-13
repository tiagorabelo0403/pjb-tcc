package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthQuery;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataJudFeedServiceHealthViewsTest {

    @Test
    void shouldExposeExtendedHealthAndAuditViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("CNJ");
        checkpoint.setLastProcessoId(88L);
        checkpoint.setTotalSent(144L);
        checkpoint.setLastSentAt(Instant.parse("2026-04-11T11:30:00Z"));
        checkpoint.setLastError("none");
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(checkpoint));

        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> { },
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 100, 300000, 3, List.of("CNJ"))
        );

        var checkpointView = service.checkpointViewResult(new DataJudCheckpointViewQuery("CNJ"));
        var execution = service.executionHealth("CNJ");
        var auditWindow = service.auditWindow("CNJ");
        var tribunalAudit = service.tribunalAuditView("CNJ");
        var health = service.health(new DataJudHealthQuery("CNJ"));
        var windowHealth = service.windowHealth("CNJ");
        var entry = service.entryView(Processo.builder().id(88L).tribunal("CNJ").numeroUnificado("0001-22.2026.8.06.0001").classeTpuCodigo("123").build());

        assertThat(checkpointView.view().lastProcessoId()).isEqualTo(88L);
        assertThat(execution.healthy()).isTrue();
        assertThat(auditWindow.tribunalCodigo()).isEqualTo("CNJ");
        assertThat(tribunalAudit.tribunalCodigo()).isEqualTo("CNJ");
        assertThat(health.health().healthy()).isTrue();
        assertThat(windowHealth.tribunalCodigo()).isEqualTo("CNJ");
        assertThat(entry.processoId()).isEqualTo(88L);
    }
}
