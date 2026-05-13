package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.CompensarSagaPeticionamentoCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.GerarProtocoloSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.ValidarSagaPeticionamentoCommand;

class PeticionamentoSagaOrchestratorTest {

    @Test
    void shouldReportValidationErrorsWhenDraftIsIncomplete() {
        LaianePeticaoInicialDraftService draftService = mock(LaianePeticaoInicialDraftService.class);
        when(draftService.detalhar(10L)).thenReturn(new LaianePeticaoInicialDraftService.DraftView(10L, null, null, null, null, null, null, null, null, java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), java.util.List.of(), "", "", Instant.now(), Instant.now()));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(draftService, mock(LaianePeticaoInicialDraftSessionRepository.class));
        var result = orchestrator.validar(new ValidarSagaPeticionamentoCommand(10L));
        assertThat(result.ok()).isFalse();
        assertThat(result.erros()).isNotEmpty();
    }

    @Test
    void shouldNotCompensateWhenProtocolAlreadyRealized() {
        LaianePeticaoInicialDraftSessionRepository repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        LaianePeticaoInicialDraftSession entity = new LaianePeticaoInicialDraftSession();
        entity.setId(5L);
        entity.setStatus("PROTOCOLO_REALIZADO");
        when(repository.findById(5L)).thenReturn(Optional.of(entity));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(mock(LaianePeticaoInicialDraftService.class), repository);
        var result = orchestrator.compensar(new CompensarSagaPeticionamentoCommand(5L));
        assertThat(result.status()).isEqualTo("PROTOCOLO_REALIZADO");
    }

    @Test
    void shouldReturnProtocolResultFromDraftService() {
        LaianePeticaoInicialDraftService draftService = mock(LaianePeticaoInicialDraftService.class);
        when(draftService.protocolar(7L, null)).thenReturn(new LaianePeticaoInicialDraftService.ProtocolarResult(7L, 99L, "0001", "PROTOCOLO_REALIZADO", Instant.now(), "hash", "ref"));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(draftService, mock(LaianePeticaoInicialDraftSessionRepository.class));
        var result = orchestrator.gerarProtocolo(new GerarProtocoloSagaCommand(7L));
        assertThat(result.processoId()).isEqualTo(99L);
        assertThat(result.numeroProcesso()).isEqualTo("0001");
    }
}
