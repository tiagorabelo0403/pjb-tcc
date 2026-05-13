package com.tcc.pjb.backend.integration.datajud.feed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ReadAfterWriteConsistencyPolicy;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
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

class DataJudFeedServiceSuccessPathTest {

    @Test
    void shouldAdvanceCheckpointWhenBatchSucceeds() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        DataJudFeedCheckpointRepository checkpointRepository = mock(DataJudFeedCheckpointRepository.class);
        ReadAfterWriteConsistencyPolicy rawPolicy = mock(ReadAfterWriteConsistencyPolicy.class);
        Processo processo = Processo.builder()
                .id(8L)
                .tribunal("CNJ")
                .numeroUnificado("0008")
                .classeTpuCodigo("123")
                .assunto("tema")
                .ramoDireito(RamoDireito.CIVIL)
                .statusProcesso(StatusProcesso.EM_ANDAMENTO)
                .build();
        DataJudFeedCheckpoint checkpoint = DataJudFeedCheckpoint.init("CNJ");
        when(checkpointRepository.findByTribunalCodigo("CNJ")).thenReturn(Optional.of(checkpoint));
        when(processoRepository.findDataJudFeedBatch(eq(0L), eq("CNJ"), any(), any())).thenReturn(new SliceImpl<>(List.of(processo), PageRequest.of(0, 100), false));
        when(checkpointRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        DataJudFeedService service = new DataJudFeedService(
                processoRepository,
                checkpointRepository,
                entries -> {},
                new DataJudFeedPayloadAssembler(),
                mock(AuditLedgerService.class),
                rawPolicy,
                new DataJudFeedProperties(true, 100, 300000, 3, List.of("CNJ")));

        var result = service.runIncremental("CNJ");

        assertThat(result.totalSent()).isEqualTo(1);
        assertThat(result.lastProcessoId()).isEqualTo(8L);
        assertThat(service.checkpointView("CNJ").lastProcessoId()).isEqualTo(8L);
        verify(rawPolicy).markWrite();
    }
}
