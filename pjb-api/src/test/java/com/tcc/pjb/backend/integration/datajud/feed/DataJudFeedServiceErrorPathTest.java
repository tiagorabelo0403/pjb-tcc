package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class DataJudFeedServiceErrorPathTest {

    @Test
    void shouldPersistLastErrorWhenBulkIndexFails() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        Processo processo = Processo.builder().id(5L).tribunal("CNJ").numeroUnificado("0005").classeTpuCodigo("123").assunto("tema").ramoDireito(RamoDireito.CIVIL).statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("CNJ");
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(checkpoint));
        when(processoRepository.findDataJudFeedBatch(eq(0L), eq("CNJ"), any(), any())).thenReturn(new SliceImpl<>(List.of(processo), PageRequest.of(0, 100), false));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DataJudFeedService service = new DataJudFeedService(processoRepository, checkpointRepository, entries -> { throw new RuntimeException("falha remota"); }, new DataJudFeedPayloadAssembler(), mock(AuditLedgerService.class), new ReadAfterWriteConsistencyPolicy(Clock.fixed(Instant.now(), ZoneOffset.UTC), Duration.ofSeconds(5)), new DataJudFeedProperties(true, 100, 300000, 3, List.of("CNJ")));
        var result = service.runIncremental("CNJ");
        assertThat(result.totalSent()).isZero();
        assertThat(service.errorSnapshot("CNJ").lastError()).contains("falha remota");
    }
}
