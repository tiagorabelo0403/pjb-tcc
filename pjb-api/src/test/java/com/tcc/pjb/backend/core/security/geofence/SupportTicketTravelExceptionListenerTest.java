package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportTicketTravelExceptionListenerTest {

    private final JudgeTravelExceptionRepository repository = mock(JudgeTravelExceptionRepository.class);
    private final SupportTicketTravelExceptionListener listener =
            new SupportTicketTravelExceptionListener(repository);

    @Test
    void chamadoDeExcecaoDeViagemAprovadoCriaJanela() {
        var evento = new SupportTicketResolvedEvent(10L, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                true, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listener.aoResolverChamado(evento);

        ArgumentCaptor<JudgeTravelException> captor = ArgumentCaptor.forClass(JudgeTravelException.class);
        verify(repository).save(captor.capture());
        JudgeTravelException salvo = captor.getValue();
        assertThat(salvo.getUsuarioId()).isEqualTo(5L);
        assertThat(salvo.getUfOuPaisDestino()).isEqualTo("DF");
        assertThat(salvo.getDataInicio()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(salvo.getDataFim()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(salvo.getTicketOrigemId()).isEqualTo(10L);
    }

    @Test
    void chamadoNaoAprovadoNaoCriaJanela() {
        var evento = new SupportTicketResolvedEvent(10L, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                false, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        listener.aoResolverChamado(evento);

        verifyNoInteractions(repository);
    }

    @Test
    void chamadoDeOutraCategoriaEIgnorado() {
        var evento = new SupportTicketResolvedEvent(10L, SupportTicketCategoria.TECNICO,
                true, 5L, null, null, null);

        listener.aoResolverChamado(evento);

        verifyNoInteractions(repository);
    }
}
