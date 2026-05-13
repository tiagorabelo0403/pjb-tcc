package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensacaoSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ProtocoloSagaPeticionamentoResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaResult;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidacaoSagaPeticionamentoResult;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaWorkerTest {

    @Test
    void deveExporMapaDaValidacao() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        when(orchestrator.validar(any(com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidarSagaPeticionamentoCommand.class))).thenReturn(new ValidacaoSagaPeticionamentoResult(true, List.of()));
        PeticionamentoSagaWorker worker = new PeticionamentoSagaWorker(orchestrator, mock(AuditLedgerService.class));
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getVariablesAsMap()).thenReturn(Map.of("rascunhoId", 15L));

        Map<String, Object> result = worker.validarPeticao(job);

        assertThat(result).containsEntry("validacaoOk", true);
        assertThat(result).containsKey("erros");
    }

    @Test
    void deveExporMapaDoProtocolo() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        when(orchestrator.gerarProtocolo(any(com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand.class))).thenReturn(new ProtocoloSagaPeticionamentoResult(99L, "0001234-56", Instant.parse("2026-04-11T12:00:00Z"), "REF-1"));
        PeticionamentoSagaWorker worker = new PeticionamentoSagaWorker(orchestrator, mock(AuditLedgerService.class));
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getVariablesAsMap()).thenReturn(Map.of("rascunhoId", 9L));

        Map<String, Object> result = worker.gerarProtocolo(job);

        assertThat(result).containsEntry("numeroProtocolo", "0001234-56");
        assertThat(result).containsEntry("processoId", 99L);
        assertThat(result).containsEntry("referenciaConector", "REF-1");
    }

    @Test
    void deveExporMapasDasEtapasIntermediarias() {
        PeticionamentoSagaOrchestrator orchestrator = mock(PeticionamentoSagaOrchestrator.class);
        when(orchestrator.registrarNoProcesso(any())).thenReturn(new RegistrarNoProcessoSagaResult(1L, true, "REGISTRADO"));
        when(orchestrator.dispararTriagem(any(com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaCommand.class))).thenReturn(new DispararTriagemSagaResult(1L, true, "TRIAGEM"));
        when(orchestrator.notificarPartes(any(com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaCommand.class))).thenReturn(new NotificarPartesSagaResult(1L, true, "NOTIFICADO"));
        when(orchestrator.compensar(any(com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensarSagaPeticionamentoCommand.class))).thenReturn(new CompensacaoSagaPeticionamentoResult(1L, "COMPENSADO_SAGA"));
        PeticionamentoSagaWorker worker = new PeticionamentoSagaWorker(orchestrator, mock(AuditLedgerService.class));
        ActivatedJob job = mock(ActivatedJob.class);
        when(job.getVariablesAsMap()).thenReturn(Map.of("rascunhoId", 1L));

        assertThat(worker.registrarNoProcesso(job)).containsEntry("registrado", true).containsEntry("statusRegistro", "REGISTRADO");
        assertThat(worker.dispararTriagem(job)).containsEntry("triagemDisparada", true).containsEntry("statusTriagem", "TRIAGEM");
        assertThat(worker.notificarPartes(job)).containsEntry("partesNotificadas", true).containsEntry("statusNotificacao", "NOTIFICADO");
        worker.compensarPeticao(job);
    }
}
