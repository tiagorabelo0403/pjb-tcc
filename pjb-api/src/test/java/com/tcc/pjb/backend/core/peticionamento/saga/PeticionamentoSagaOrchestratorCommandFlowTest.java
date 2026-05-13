package com.tcc.pjb.backend.core.peticionamento.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.peticionamento.saga.domain.DispararTriagemSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.NotificarPartesSagaCommand;
import com.tcc.pjb.backend.core.peticionamento.saga.domain.RegistrarNoProcessoSagaCommand;
import com.tcc.pjb.backend.model.entity.intelligence.LaianePeticaoInicialDraftSession;
import com.tcc.pjb.backend.model.repository.LaianePeticaoInicialDraftSessionRepository;
import com.tcc.pjb.backend.service.advogado.LaianePeticaoInicialDraftService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PeticionamentoSagaOrchestratorCommandFlowTest {

    @Test
    void shouldReturnStatusForRegisterTriagemAndNotificacaoCommands() {
        LaianePeticaoInicialDraftSessionRepository repository = mock(LaianePeticaoInicialDraftSessionRepository.class);
        LaianePeticaoInicialDraftSession entity = LaianePeticaoInicialDraftSession.builder()
                .id(77L)
                .status("EM_FLUXO")
                .conteudoHtml("<p>ok</p>")
                .build();
        when(repository.findById(77L)).thenReturn(Optional.of(entity));
        PeticionamentoSagaOrchestrator orchestrator = new PeticionamentoSagaOrchestrator(mock(LaianePeticaoInicialDraftService.class), repository);

        var registro = orchestrator.registrarNoProcesso(new RegistrarNoProcessoSagaCommand(77L));
        var triagem = orchestrator.dispararTriagem(new DispararTriagemSagaCommand(77L));
        var notificacao = orchestrator.notificarPartes(new NotificarPartesSagaCommand(77L));

        assertThat(registro.registrado()).isTrue();
        assertThat(registro.status()).isEqualTo("EM_FLUXO");
        assertThat(triagem.disparado()).isTrue();
        assertThat(triagem.status()).isEqualTo("EM_FLUXO");
        assertThat(notificacao.notificado()).isTrue();
        assertThat(notificacao.status()).isEqualTo("EM_FLUXO");
    }
}
