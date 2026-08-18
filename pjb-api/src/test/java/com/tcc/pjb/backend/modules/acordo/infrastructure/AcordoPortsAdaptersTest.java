package com.tcc.pjb.backend.modules.acordo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.entity.Jurisdicao;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoCommand;
import com.tcc.pjb.backend.modules.acordo.api.MovimentacaoAcordoCommand;
import com.tcc.pjb.backend.modules.acordo.domain.AcordoAuditoriaEvento;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaEntity;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaJpaRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AcordoPortsAdaptersTest {

    @Test
    void processoAdapterConverteEntityLegadaParaContextoInterno() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
        Processo processo = processo(1L);
        when(processoRepository.findContextoCompletoById(1L)).thenReturn(Optional.of(processo));

        PjbProcessoAcordoAdapter adapter = new PjbProcessoAcordoAdapter(processoRepository, movimentacaoRepository);

        var contexto = adapter.obterContextoProcessual(1L);

        assertThat(contexto.processoId()).isEqualTo(1L);
        assertThat(contexto.classeProcessual()).isEqualTo("PROCEDIMENTO_COMUM");
        assertThat(contexto.unidadeJudiciariaId()).isEqualTo(44L);
        assertThat(contexto.segredoJustica()).isTrue();
        assertThat(contexto.permiteAcordo()).isTrue();
        assertThat(contexto).isNotInstanceOf(Processo.class);
    }

    @Test
    void usuarioAdapterValidaParticipacaoERetornaContextoSemEntity() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        Processo processo = processo(1L);
        Usuario usuario = usuario(10L, TipoUsuario.ADVOGADO, true);
        usuario.setCpf("12345678900");
        processo.setParteAutoraCpf("123.456.789-00");
        when(processoRepository.findContextoCompletoById(1L)).thenReturn(Optional.of(processo));
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        PjbUsuarioAcordoAdapter adapter = new PjbUsuarioAcordoAdapter(processoRepository, usuarioRepository);

        var contexto = adapter.obterContextoUsuario(1L, 10L);

        assertThat(adapter.usuarioPodeParticipar(1L, 10L)).isTrue();
        assertThat(contexto.usuarioId()).isEqualTo(10L);
        assertThat(contexto.nomeExibicao()).isEqualTo("Usuario Teste");
        assertThat(contexto.papeis()).contains("ADVOGADO");
        assertThat(contexto.podeParticipar()).isTrue();
        assertThat(contexto).isNotInstanceOf(Usuario.class);
    }

    @Test
    void movimentacaoAdapterRegistraMovimentacaoPorCommand() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        MovimentacaoProcessualRepository movimentacaoRepository = mock(MovimentacaoProcessualRepository.class);
        Processo processo = processo(1L);
        Usuario usuario = usuario(99L, TipoUsuario.JUIZ, true);
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(usuario));

        PjbMovimentacaoAcordoAdapter adapter = new PjbMovimentacaoAcordoAdapter(processoRepository, usuarioRepository, movimentacaoRepository);
        adapter.registrarHomologacao(new MovimentacaoAcordoCommand(1L, "ACORDO_HOMOLOGADO", "Homologado por sentenca.", 99L, "ACORDO_PROCESSUAL"));

        ArgumentCaptor<MovimentacaoProcessual> captor = ArgumentCaptor.forClass(MovimentacaoProcessual.class);
        verify(movimentacaoRepository).save(captor.capture());
        assertThat(captor.getValue().getProcesso()).isSameAs(processo);
        assertThat(captor.getValue().getAtor()).isSameAs(usuario);
        assertThat(captor.getValue().getDescricao()).contains("ACORDO_PROCESSUAL", "ACORDO_HOMOLOGADO");
    }

    @Test
    void auditoriaAdapterRegistraEventoSensivelComOrigem() {
        AcordoAuditoriaJpaRepository repository = mock(AcordoAuditoriaJpaRepository.class);
        JpaAuditoriaAcordoAdapter adapter = new JpaAuditoriaAcordoAdapter(repository, new ObjectMapper());
        Instant createdAt = Instant.parse("2026-05-19T12:00:00Z");

        adapter.registrarEventoSensivel(new AuditoriaAcordoCommand(
                7L,
                99L,
                AcordoAuditoriaEvento.HOMOLOGACAO,
                Map.of("termoId", 5L),
                "ACORDO_PROCESSUAL",
                "ip-hash",
                "ua-hash",
                createdAt
        ));

        ArgumentCaptor<AcordoAuditoriaEntity> captor = ArgumentCaptor.forClass(AcordoAuditoriaEntity.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getSessaoId()).isEqualTo(7L);
        assertThat(captor.getValue().getUsuarioId()).isEqualTo(99L);
        assertThat(captor.getValue().getEvento()).isEqualTo(AcordoAuditoriaEvento.HOMOLOGACAO);
        assertThat(captor.getValue().getDetalhesJson()).contains("ACORDO_PROCESSUAL", "sensivel");
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(createdAt);
    }

    private Processo processo(Long id) {
        Processo processo = new Processo();
        processo.setId(id);
        processo.setFaseAtual(FaseProcessual.CITACAO);
        processo.setSigilo(NivelSigilo.SEGREDO_JUSTICA);
        processo.setClasseProcessual("PROCEDIMENTO_COMUM");
        processo.setPotencialAcordoScore(80);
        processo.setJanelaAcordoResumo("Requerimento de parte para conciliacao");
        Jurisdicao jurisdicao = new Jurisdicao();
        jurisdicao.setId(44L);
        processo.setJurisdicao(jurisdicao);
        return processo;
    }

    private Usuario usuario(Long id, TipoUsuario tipo, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setNome("Usuario Teste");
        usuario.setTipoUsuario(tipo);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
