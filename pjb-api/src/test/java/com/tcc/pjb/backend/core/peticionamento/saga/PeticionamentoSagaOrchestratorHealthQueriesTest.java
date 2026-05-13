package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaAuditQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaCompensationQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaHealthQuery;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.SagaStepQuery;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaOrchestratorHealthQueriesTest {

    @Test
    void shouldExposeHealthAuditTimelineCompensationAndStepViews() {
        LaianePeticaoInicialDraftSessionRepository repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        LaianePeticaoInicialDraftSession entity = LaianePeticaoInicialDraftSession.builder()
                .id(19L)
                .status("EM_TRIAGEM")
                .conteudoHtml("<p>conteudo</p>")
                .numeroProtocolo("PROTO-19")
                .build();
        when(repository.findById(19L)).thenReturn(Optional.of(entity));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(mock(LaianePeticaoInicialDraftService.class), repository);

        var health = orchestrator.health(new SagaHealthQuery(19L));
        var executionHealth = orchestrator.executionHealth(19L);
        var audit = orchestrator.audit(new SagaAuditQuery(19L));
        var timeline = orchestrator.executionTimeline(19L);
        var compensation = orchestrator.compensation(new SagaCompensationQuery(19L, "manual"));
        var steps = orchestrator.stepViews(19L);
        var step = orchestrator.stepResult(new SagaStepQuery(19L, "TRIAGEM"));
        var commandAudit = orchestrator.commandAudit(19L, "TRIAGEM");

        assertThat(health.status()).isEqualTo("EM_TRIAGEM");
        assertThat(executionHealth.status()).isEqualTo("EM_TRIAGEM");
        assertThat(audit.triagem().rascunhoId()).isEqualTo(19L);
        assertThat(timeline.steps()).isNotEmpty();
        assertThat(compensation.rascunhoId()).isEqualTo(19L);
        assertThat(steps).isNotEmpty();
        assertThat(step.step().etapa()).isEqualTo("TRIAGEM");
        assertThat(commandAudit.etapa()).isEqualTo("TRIAGEM");
    }
}
