package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaOrchestratorViewsTest {

    @Test
    void shouldExposeHealthAuditStepViewsAndEnvelope() {
        LaianePeticaoInicialDraftSessionRepository repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        LaianePeticaoInicialDraftSession entity = LaianePeticaoInicialDraftSession.builder().id(5L).status("PROTOCOLO_REALIZADO").conteudoHtml("<p>ok</p>").build();
        when(repository.findById(5L)).thenReturn(Optional.of(entity));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(mock(LaianePeticaoInicialDraftService.class), repository);

        var health = orchestrator.health(new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthQuery(5L));
        var audit = orchestrator.executionAuditView(5L);
        var step = orchestrator.stepResult(new com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepQuery(5L, "VALIDAR"));
        var envelope = orchestrator.envelope(5L, "VALIDAR");

        assertThat(health.status()).isEqualTo("PROTOCOLO_REALIZADO");
        assertThat(audit.timeline().steps()).isNotEmpty();
        assertThat(step.step().etapa()).isEqualTo("VALIDAR");
        assertThat(envelope.payloadHash()).isNotBlank();
    }
}
