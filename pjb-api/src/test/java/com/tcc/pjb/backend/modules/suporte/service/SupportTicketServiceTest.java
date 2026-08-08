package com.tcc.pjb.backend.modules.suporte.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.suporte.dto.AbrirChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.dto.ResolverChamadoRequest;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicket;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketCategoria;
import com.tcc.pjb.backend.modules.suporte.entity.SupportTicketStatus;
import com.tcc.pjb.backend.modules.suporte.event.SupportTicketResolvedEvent;
import com.tcc.pjb.backend.modules.suporte.repository.SupportTicketRepository;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.notification.NotificationService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class SupportTicketServiceTest {

    private final SupportTicketRepository repository = mock(SupportTicketRepository.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final SupportTicketService service =
            new SupportTicketService(repository, notificationService, usuarioRepository, eventPublisher);

    private Usuario cidadao;

    @BeforeEach
    void setup() {
        cidadao = new Usuario();
        cidadao.setId(1L);
        cidadao.setNome("Maria");
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        when(repository.save(any(SupportTicket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void abrirChamadoTecnicoNaoExigeCamposDeViagem() {
        var request = new AbrirChamadoRequest(SupportTicketCategoria.TECNICO, "Nao consigo logar", "Erro X", null, null, null, null);

        var resposta = service.abrir(cidadao, request);

        assertThat(resposta.status()).isEqualTo(SupportTicketStatus.ABERTO);
        assertThat(resposta.abertoPorId()).isEqualTo(1L);
    }

    @Test
    void abrirExcecaoDeViagemSemDestinoLancaErroDeValidacao() {
        var request = new AbrirChamadoRequest(
                SupportTicketCategoria.EXCECAO_VIAGEM_MAGISTRATURA, "Viagem a Brasilia", "Evento oficial",
                null, LocalDate.now(), LocalDate.now().plusDays(3), "Congresso");

        assertThatThrownBy(() -> service.abrir(cidadao, request))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void abrirExcecaoDeViagemComDataFimAntesDoInicioLancaErro() {
        var request = new AbrirChamadoRequest(
                SupportTicketCategoria.EXCECAO_VIAGEM_MAGISTRATURA, "Viagem", "Evento",
                "DF", LocalDate.now().plusDays(5), LocalDate.now(), "Justificativa");

        assertThatThrownBy(() -> service.abrir(cidadao, request))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void assumirChamadoAbertoMudaParaEmAtendimento() {
        SupportTicket ticket = ticketAberto();
        when(repository.findById(10L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cidadao));

        var resposta = service.assumir(10L, atendente());

        assertThat(resposta.status()).isEqualTo(SupportTicketStatus.EM_ATENDIMENTO);
        assertThat(resposta.atendidoPorId()).isEqualTo(99L);
        verify(notificationService).notifyUser(any(), eq(null), any(), any(), any());
    }

    @Test
    void assumirChamadoJaEmAtendimentoLancaErro() {
        SupportTicket ticket = ticketAberto();
        ticket.setStatus(SupportTicketStatus.EM_ATENDIMENTO);
        when(repository.findById(10L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> service.assumir(10L, atendente()))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void resolverExcecaoDeViagemAprovadaPublicaEventoComDadosDaViagem() {
        SupportTicket ticket = ticketAberto();
        ticket.setStatus(SupportTicketStatus.EM_ATENDIMENTO);
        ticket.setCategoria(SupportTicketCategoria.EXCECAO_VIAGEM_MAGISTRATURA);
        ticket.setViagemUfOuPaisDestino("DF");
        ticket.setViagemDataInicio(LocalDate.of(2026, 9, 1));
        ticket.setViagemDataFim(LocalDate.of(2026, 9, 10));
        when(repository.findById(10L)).thenReturn(Optional.of(ticket));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cidadao));

        service.resolver(10L, atendente(), new ResolverChamadoRequest("Aprovado, boa viagem", true));

        var captor = org.mockito.ArgumentCaptor.forClass(SupportTicketResolvedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().aprovado()).isTrue();
        assertThat(captor.getValue().viagemUfOuPaisDestino()).isEqualTo("DF");
    }

    @Test
    void cancelarPorUsuarioDiferenteDeQuemAbriuLancaErro() {
        SupportTicket ticket = ticketAberto();
        when(repository.findById(10L)).thenReturn(Optional.of(ticket));
        Usuario outro = new Usuario();
        outro.setId(2L);

        assertThatThrownBy(() -> service.cancelar(10L, outro))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    private SupportTicket ticketAberto() {
        return SupportTicket.builder()
                .id(10L)
                .abertoPorId(1L)
                .abertoPorNome("Maria")
                .categoria(SupportTicketCategoria.TECNICO)
                .assunto("Assunto")
                .descricao("Descricao")
                .status(SupportTicketStatus.ABERTO)
                .criadoEm(java.time.Instant.now())
                .build();
    }

    private Usuario atendente() {
        Usuario u = new Usuario();
        u.setId(99L);
        u.setNome("Suporte Tecnico");
        return u;
    }
}
