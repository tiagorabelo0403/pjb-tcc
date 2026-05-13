package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudCheckpointCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataJudFeedServiceHelperMethodsTest {

    @Test
    void shouldExposeHelperSnapshotsAndProjection() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("TJCE");
        checkpoint.setLastProcessoId(123L);
        checkpoint.setTotalSent(10L);
        checkpoint.setLastSentAt(Instant.parse("2026-04-11T10:00:00Z"));
        checkpoint.setLastError("timeout");
        when(checkpointRepository.findByTribunalCodigo("TJCE")).thenReturn(Optional.of(checkpoint));

        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 200, 300000, 3, List.of("TJCE")));

        Processo processo = Processo.builder()
                .id(99L)
                .numeroUnificado("0000999-11.2026.8.06.0001")
                .tribunal("TJCE")
                .classeTpuCodigo("7")
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();

        var checkpointUpdate = service.checkpointUpdate("TJCE");
        var errorSnapshot = service.errorSnapshot("TJCE");
        var processoSnapshot = service.processoSnapshot(processo);
        var tribunalConfig = service.tribunalConfig("TJCE");
        var progress = service.progress("TJCE", 5);
        var projection = service.projection(processo);
        var checkpointSnapshot = service.checkpointSnapshot(new DataJudCheckpointCommand("TJCE"));
        var checkpointAudit = service.checkpointAudit("TJCE");
        var tribunalProgress = service.tribunalProgress("TJCE", 5);
        var entryAudit = service.entryAudit(processo);
        var tribunalWindow = service.tribunalWindow("TJCE");

        assertThat(checkpointUpdate.lastProcessoId()).isEqualTo(123L);
        assertThat(errorSnapshot.lastError()).isEqualTo("timeout");
        assertThat(processoSnapshot.processoId()).isEqualTo(99L);
        assertThat(tribunalConfig.enabled()).isTrue();
        assertThat(progress.batchSent()).isEqualTo(5);
        assertThat(projection.numeroUnificado()).isEqualTo("0000999-11.2026.8.06.0001");
        assertThat(checkpointSnapshot.lastProcessoId()).isEqualTo(123L);
        assertThat(checkpointAudit.totalSent()).isEqualTo(10L);
        assertThat(tribunalProgress.batchSent()).isEqualTo(5);
        assertThat(entryAudit.statusProcesso()).isEqualTo("EM_ANDAMENTO");
        assertThat(tribunalWindow.batchSize()).isEqualTo(200);
    }
}
