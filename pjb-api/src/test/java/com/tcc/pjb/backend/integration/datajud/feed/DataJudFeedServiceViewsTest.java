package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointCommand;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudHealthQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalHealthQuery;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudWindowQuery;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataJudFeedServiceViewsTest {

    @Test
    void shouldExposeCheckpointWindowAndHealthViews() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("CNJ");
        checkpoint.setLastProcessoId(77L);
        checkpoint.setTotalSent(12L);
        checkpoint.setLastSentAt(Instant.parse("2026-04-11T11:00:00Z"));
        checkpoint.setLastError(null);
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(checkpoint));
        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 100, 300000, 3, List.of("CNJ")));

        var checkpointView = service.checkpointSnapshot(new DataJudCheckpointCommand("CNJ"));
        var queryResult = service.consultarCheckpoint(new com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointQuery("CNJ"));
        var health = service.health(new DataJudHealthQuery("CNJ"));
        var tribunalHealth = service.tribunalHealth(new DataJudTribunalHealthQuery("CNJ"));
        var window = service.windowView(new DataJudWindowQuery("CNJ"));

        assertThat(checkpointView.lastProcessoId()).isEqualTo(77L);
        assertThat(queryResult.view().totalSent()).isEqualTo(12L);
        assertThat(health.health().healthy()).isTrue();
        assertThat(tribunalHealth.health().enabled()).isTrue();
        assertThat(window.fromProcessoId()).isEqualTo(77L);
    }
}
