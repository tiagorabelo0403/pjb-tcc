package com.tcc.pjb.backend.modules.acordo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.modules.acordo.api.UsuarioAcordoPort;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoConfidencialidadeNivel;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemTipo;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoMensagemVisibilidade;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoPapelParticipante;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoParticipanteStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoSessaoStatus;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoTipoSala;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AcordoProcessualChatBridgeServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-19T12:00:00Z");
    private final AcordoProcessualStorePort store = mock(AcordoProcessualStorePort.class);
    private final AcordoProcessualApplicationService applicationService = mock(AcordoProcessualApplicationService.class);
    private final UsuarioAcordoPort usuarioPort = mock(UsuarioAcordoPort.class);
    private final AcordoProcessualChatBridgeService service = new AcordoProcessualChatBridgeService(
            store,
            applicationService,
            usuarioPort,
            Clock.fixed(NOW, ZoneOffset.UTC)
    );

    @Test
    void canalAcordoExplicitoExigeSalaAtiva() {
        when(store.findSessaoAtivaByProcesso(eq(1L), any(Instant.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrarMensagemDoChat(command(true)))
                .isInstanceOf(AcordoApplicationException.class)
                .hasMessageContaining("sala de acordo processual ativa");

        verify(applicationService, never()).registrarMensagem(any());
    }

    @Test
    void mensagemNaoExplicitaSemSalaEIgnoradaSemBloquearChatLegado() {
        when(store.findSessaoAtivaByProcesso(eq(1L), any(Instant.class))).thenReturn(Optional.empty());

        var result = service.registrarMensagemDoChat(command(false));

        assertThat(result.espelhadaNaSala()).isFalse();
        assertThat(result.motivo()).contains("sem sala");
        verify(applicationService, never()).registrarMensagem(any());
    }

    @Test
    void mensagemDoChatERegistradaNaSalaQuandoParticipanteAceito() {
        when(store.findSessaoAtivaByProcesso(eq(1L), any(Instant.class))).thenReturn(Optional.of(sessao()));
        when(store.findParticipante(7L, 10L)).thenReturn(Optional.of(participanteAceito()));
        when(store.findSessao(7L)).thenReturn(Optional.of(sessao()));
        when(usuarioPort.usuarioPodeParticipar(1L, 10L)).thenReturn(true);
        when(applicationService.registrarMensagem(any())).thenReturn(new AcordoMensagemSnapshot(
                99L,
                7L,
                10L,
                AcordoMensagemTipo.TEXTO,
                "Proposta de acordo com multa",
                false,
                AcordoMensagemVisibilidade.PARTICIPANTES,
                NOW
        ));

        var result = service.registrarMensagemDoChat(command(true));

        assertThat(result.espelhadaNaSala()).isTrue();
        assertThat(result.sessaoId()).isEqualTo(7L);
        assertThat(result.mensagemSalaId()).isEqualTo(99L);
        ArgumentCaptor<AcordoProcessualApplicationService.RegistrarMensagemCommand> captor =
                ArgumentCaptor.forClass(AcordoProcessualApplicationService.RegistrarMensagemCommand.class);
        verify(applicationService).registrarMensagem(captor.capture());
        assertThat(captor.getValue().sessaoId()).isEqualTo(7L);
        assertThat(captor.getValue().autorId()).isEqualTo(10L);
        assertThat(captor.getValue().conteudo()).contains("multa");
    }

    @Test
    void mensagemDoChatNaoIgnoraAceiteDoParticipante() {
        when(store.findSessaoAtivaByProcesso(eq(1L), any(Instant.class))).thenReturn(Optional.of(sessao()));
        when(store.findParticipante(7L, 10L)).thenReturn(Optional.of(participanteConvidado()));
        when(usuarioPort.usuarioPodeParticipar(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.registrarMensagemDoChat(command(true)))
                .isInstanceOf(AcordoApplicationException.class)
                .hasMessageContaining("aceitar");

        verify(applicationService, never()).registrarMensagem(any());
    }

    @Test
    void contextoDoChatExpoeSalaAtivaDoProcesso() {
        when(store.findSessaoAtivaByProcesso(eq(1L), any(Instant.class))).thenReturn(Optional.of(sessao()));
        when(store.countParticipantesAceitos(7L)).thenReturn(2L);

        var context = service.obterContexto(1L);

        assertThat(context.sessaoId()).isEqualTo(7L);
        assertThat(context.status()).isEqualTo(AcordoSessaoStatus.OPEN.name());
        assertThat(context.salaAtiva()).isTrue();
        assertThat(context.participantesAceitos()).isEqualTo(2);
    }

    private AcordoProcessualChatBridgeService.AcordoProcessualChatMessageCommand command(boolean exigirSala) {
        return new AcordoProcessualChatBridgeService.AcordoProcessualChatMessageCommand(
                1L,
                10L,
                AcordoMensagemTipo.TEXTO,
                "Proposta de acordo com multa",
                false,
                exigirSala,
                AcordoOperationMetadata.empty()
        );
    }

    private AcordoSessaoSnapshot sessao() {
        return new AcordoSessaoSnapshot(
                7L,
                1L,
                AcordoTipoSala.PROCESSUAL_CONTROLADA,
                AcordoSessaoStatus.OPEN,
                10L,
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600),
                "Acordo",
                false,
                AcordoConfidencialidadeNivel.RESTRITA_A_PARTICIPANTES,
                false,
                null,
                null,
                NOW.minusSeconds(60)
        );
    }

    private AcordoParticipanteSnapshot participanteAceito() {
        return new AcordoParticipanteSnapshot(
                3L,
                7L,
                10L,
                AcordoPapelParticipante.ADVOGADO,
                AcordoParticipanteStatus.ACEITO,
                NOW.minusSeconds(30),
                null,
                NOW.minusSeconds(40)
        );
    }

    private AcordoParticipanteSnapshot participanteConvidado() {
        return new AcordoParticipanteSnapshot(
                3L,
                7L,
                10L,
                AcordoPapelParticipante.ADVOGADO,
                AcordoParticipanteStatus.CONVIDADO,
                null,
                null,
                NOW.minusSeconds(40)
        );
    }
}
