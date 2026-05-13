package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaResult;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaWorkerStringIdTest {

    @Test
    void shouldAcceptStringRascunhoIdInWorkerPayload() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        when(orchestrator.notificarPartes(any())).thenReturn(new NotificarPartesSagaResult(12L, true, "NOTIFICADO"));
        PeticionamentoSagaWorker worker = new PeticionamentoSagaWorker(orchestrator, mock(AuditLedgerService.class));
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getVariablesAsMap()).thenReturn(Map.of("rascunhoId", "12"));

        var result = worker.notificarPartes(job);

        assertThat(result).containsEntry("partesNotificadas", true).containsEntry("statusNotificacao", "NOTIFICADO");
    }
}
