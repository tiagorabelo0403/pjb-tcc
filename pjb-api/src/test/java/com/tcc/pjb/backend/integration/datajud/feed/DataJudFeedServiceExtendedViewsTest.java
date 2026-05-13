package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointViewQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthQuery;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataJudFeedServiceExtendedViewsTest {

    @Test
    void shouldExposeExtendedCheckpointExecutionAndAuditViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("CNJ");
        checkpoint.setLastProcessoId(120L);
        checkpoint.setTotalSent(40L);
        checkpoint.setLastSentAt(Instant.parse("2026-04-11T12:00:00Z"));
        checkpoint.setLastError(null);
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(checkpoint));
        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 100, 300000, 3, List.of("CNJ"))
        );

        var checkpointViewResult = service.checkpointViewResult(new DataJudCheckpointViewQuery("CNJ"));
        var executionHealth = service.executionHealth("CNJ");
        var auditWindow = service.auditWindow("CNJ");
        var tribunalAuditEntry = service.tribunalAuditEntry("CNJ");
        var health = service.health(new DataJudHealthQuery("CNJ"));
        var tribunalHealth = service.tribunalHealth(new DataJudTribunalHealthQuery("CNJ"));
        Processo processo = Processo.builder().id(88L).numeroUnificado("000088-11.2026.8.06.0001").tribunal("CNJ").classeTpuCodigo("1234").build();
        var entryView = service.entryView(processo);

        assertThat(checkpointViewResult.view().lastProcessoId()).isEqualTo(120L);
        assertThat(checkpointViewResult.audit().lastSentAt()).isEqualTo(Instant.parse("2026-04-11T12:00:00Z"));
        assertThat(executionHealth.healthy()).isTrue();
        assertThat(executionHealth.totalSent()).isEqualTo(40L);
        assertThat(auditWindow.fromProcessoId()).isEqualTo(120L);
        assertThat(auditWindow.enabled()).isTrue();
        assertThat(tribunalAuditEntry.totalSent()).isEqualTo(40L);
        assertThat(health.view().totalSent()).isEqualTo(40L);
        assertThat(tribunalHealth.window().batchSize()).isEqualTo(100);
        assertThat(entryView.processoId()).isEqualTo(88L);
        assertThat(entryView.classeTpuCodigo()).isEqualTo("1234");
    }
}
