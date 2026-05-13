package com.tcc.pjb.backend.integration.judicial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class JudicialExternalSyncReplayServiceTest {

    @Test
    void replaysPendingSynchronizations() {
        ProcessoRepository processoRepository = Mockito.mock(ProcessoRepository.class);
        JudicialConnectorLifecycleService lifecycleService = Mockito.mock(JudicialConnectorLifecycleService.class);
        Processo processo = new Processo();
        processo.setId(101L);
        processo.setConnectorSystem("PJE");
        processo.setConnectorProtocolReference("PJE-101");
        processo.setConnectorSyncStatus("SYNC_ERROR");

        when(processoRepository.findConnectorSyncReplayCandidates(any(), eq(6), eq(PageRequest.of(0, 16))))
                .thenReturn(new PageImpl<>(List.of(processo)));
        Mockito.doAnswer(invocation -> {
            Processo target = invocation.getArgument(0);
            target.setConnectorSyncStatus("SNAPSHOT_SYNCED");
            return null;
        }).when(lifecycleService).synchronizeExternalState(eq(processo), any());

        JudicialExternalSyncReplayService service = new JudicialExternalSyncReplayService(processoRepository, lifecycleService);

        int synced = service.replayPendingSynchronizations(16, 6);

        assertThat(synced).isEqualTo(1);
        verify(processoRepository).save(processo);
        assertThat(processo.getConnectorSyncStatus()).isEqualTo("SNAPSHOT_SYNCED");
    }
}
