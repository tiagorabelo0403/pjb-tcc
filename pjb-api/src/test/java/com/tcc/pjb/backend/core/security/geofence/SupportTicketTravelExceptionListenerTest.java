package com.tcc.pjb.backend.core.security.geofence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.configs.datasource.ExecucaoEmContextoDeSistema;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import java.time.LocalDate;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SupportTicketTravelExceptionListenerTest {

    private final JudgeTravelExceptionRepository repository = mock(JudgeTravelExceptionRepository.class);
    private final ExecucaoEmContextoDeSistema execucaoEmContextoDeSistema = mock(ExecucaoEmContextoDeSistema.class);
    private final AuditLedgerService auditService = mock(AuditLedgerService.class);
    private final SupportTicketTravelExceptionListener listener =
            new SupportTicketTravelExceptionListener(repository, execucaoEmContextoDeSistema, auditService);

    SupportTicketTravelExceptionListenerTest() {
        when(execucaoEmContextoDeSistema.emTransacaoNova(any())).thenAnswer(inv -> inv.<Supplier<?>>getArgument(0).get());
    }

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
        verify(execucaoEmContextoDeSistema).emTransacaoNova(any());
    }

    @Test
    void janelaGravadaFicaNaTrilhaDeAuditoriaComOIdDaExcecao() {
        when(repository.save(any())).thenAnswer(inv -> {
            JudgeTravelException excecao = inv.getArgument(0);
            excecao.setId(77L);
            return excecao;
        });

        listener.aoResolverChamado(eventoAprovado());

        verify(auditService).appendSafely("GEOFENCE_EXCECAO_VIAGEM_CONCEDIDA", "JUDGE_TRAVEL_EXCEPTION", "77");
    }

    @Test
    void falhaAoGravarAJanelaFicaAuditadaENaoEscapaDoListener() {
        when(repository.save(any())).thenThrow(new IllegalStateException("nova linha viola a politica de RLS"));

        listener.aoResolverChamado(eventoAprovado());

        verify(auditService).appendSafely("GEOFENCE_EXCECAO_VIAGEM_NAO_GRAVADA", "SUPPORT_TICKET", "10");
        verify(auditService, never()).appendSafely(eq("GEOFENCE_EXCECAO_VIAGEM_CONCEDIDA"), anyString(), anyString());
    }

    private static SupportTicketResolvedEvent eventoAprovado() {
        return new SupportTicketResolvedEvent(10L, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                true, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));
    }

    @Test
    void chamadoNaoAprovadoNaoCriaJanela() {
        var evento = new SupportTicketResolvedEvent(10L, SupportTicketCategoria.EXCECAO_VIAGEM_CARREIRA_JURIDICA,
                false, 5L, "DF", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 10));

        listener.aoResolverChamado(evento);

        verifyNoInteractions(repository, auditService);
    }

    @Test
    void chamadoDeOutraCategoriaEIgnorado() {
        var evento = new SupportTicketResolvedEvent(10L, SupportTicketCategoria.TECNICO,
                true, 5L, null, null, null);

        listener.aoResolverChamado(evento);

        verifyNoInteractions(repository, auditService);
    }
}
