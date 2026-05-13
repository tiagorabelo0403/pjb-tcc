package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudFeedBatchCommand;
import com.tcc.pjb.backend.integration.datajud.feed.domain.DataJudTribunalRunCommand;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.entity.judicial.DataJudFeedCheckpoint;
import com.tcc.pjb.backend.model.repository.DataJudFeedCheckpointRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

class DataJudFeedServiceCommandVariantsTest {

    @Test
    void shouldRunFromTribunalCommand() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        Processo processo = Processo.builder().id(12L).tribunal("CNJ").numeroUnificado("00012").classeTpuCodigo("321").assunto("tema").ramoDireito(RamoDireito.CIVIL).statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(DataJudFeedCheckpoint.init("CNJ")));
        when(processoRepository.findDataJudFeedBatch(eq(0L), eq("CNJ"), any(), any())).thenReturn(new SliceImpl<>(List.of(processo), PageRequest.of(0, 10), false));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 10, 1000, 2, List.of("CNJ")));

        var result = service.runIncremental(new DataJudTribunalRunCommand("CNJ", 2));

        assertThat(result.tribunalCodigo()).isEqualTo("CNJ");
        assertThat(result.totalSent()).isEqualTo(1);
    }

    @Test
    void shouldUseBatchCommandWindowAndReturnSuccess() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        Processo processo = Processo.builder().id(13L).tribunal("CNJ").numeroUnificado("00013").classeTpuCodigo("654").assunto("tema").ramoDireito(RamoDireito.CIVIL).statusProcesso(StatusProcesso.EM_ANDAMENTO).build();
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(DataJudFeedCheckpoint.init("CNJ")));
        when(processoRepository.findDataJudFeedBatch(eq(0L), eq("CNJ"), any(), any())).thenReturn(new SliceImpl<>(List.of(processo), PageRequest.of(0, 5), false));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                mock(ReadAfterWriteConsistencyPolicy.class),
                new DataJudFeedProperties(true, 5, 1000, 1, List.of("CNJ")));

        var result = service.runIncremental(new DataJudFeedBatchCommand("CNJ", 1, 5));

        assertThat(result.success()).isTrue();
        assertThat(result.totalSent()).isEqualTo(1);
    }
}
