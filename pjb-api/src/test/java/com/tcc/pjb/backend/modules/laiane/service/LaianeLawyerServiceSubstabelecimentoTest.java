package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.time.PjbTimeService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import com.tcc.pjb.backend.model.repository.WorkItemRepository;
import com.tcc.pjb.backend.modules.auditoria.AuditoriaInteligenteService;
import com.tcc.pjb.backend.modules.laiane.entity.LaianeProcuracao;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeCaseBundleRepository;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeDeadlineDelegationRepository;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeTeseRepository;
import com.tcc.pjb.backend.modules.laiane.util.LaianeRoleGuard;
import com.tcc.pjb.backend.service.exception.ErroDeValidacaoException;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.processual.representacao.RepresentacaoProcessualPolicyService;
import java.time.LocalDate;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LaianeLawyerServiceSubstabelecimentoTest {

    private final LaianeRoleGuard guard = mock(LaianeRoleGuard.class);
    private final LaianeProcuracaoRepository procuracaoRepository = mock(LaianeProcuracaoRepository.class);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    private final LaianeLawyerService service = new LaianeLawyerService(
            guard,
            procuracaoRepository,
            mock(LaianeTeseRepository.class),
            mock(WorkItemRepository.class),
            usuarioRepository,
            mock(ProcessoRepository.class),
            mock(LaianeDeadlineDelegationRepository.class),
            mock(LaianeCaseBundleRepository.class),
            mock(AuditoriaInteligenteService.class),
            new ObjectMapper(),
            mock(ProceduralCatalogService.class),
            mock(RepresentacaoProcessualPolicyService.class),
            mock(PjbTimeService.class));

    private Usuario advogado(Long id) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTipoUsuario(TipoUsuario.ADVOGADO);
        return u;
    }

    @Test
    void substabelecimentoSemReservaCriaNovaProcuracaoERevogaAOrigem() {
        Usuario substabelecente = advogado(1L);
        Usuario destinatario = advogado(2L);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(10L)
                .advogado(substabelecente)
                .clienteId(500L)
                .processoId(700L)
                .status(LaianeProcuracaoStatus.ATIVA)
                .inicioVigencia(LocalDate.now().minusDays(5))
                .poderes("Ad judicia et extra")
                .build();
        when(procuracaoRepository.findById(10L)).thenReturn(Optional.of(origem));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(destinatario));
        when(procuracaoRepository.save(org.mockito.ArgumentMatchers.any(LaianeProcuracao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LaianeProcuracao novaProcuracao = service.substabelecer(10L, 2L, false);

        assertThat(novaProcuracao.getAdvogado()).isEqualTo(destinatario);
        assertThat(novaProcuracao.getSubstabelecidoDe()).isEqualTo(origem);
        assertThat(novaProcuracao.getStatus()).isEqualTo(LaianeProcuracaoStatus.ATIVA);
        assertThat(novaProcuracao.getPoderes()).isEqualTo("Ad judicia et extra");
        assertThat(origem.getStatus()).isEqualTo(LaianeProcuracaoStatus.REVOGADA);
    }

    @Test
    void substabelecimentoComReservaMantemAOrigemAtiva() {
        Usuario substabelecente = advogado(1L);
        Usuario destinatario = advogado(2L);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(11L)
                .advogado(substabelecente)
                .status(LaianeProcuracaoStatus.ATIVA)
                .build();
        when(procuracaoRepository.findById(11L)).thenReturn(Optional.of(origem));
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(destinatario));
        when(procuracaoRepository.save(org.mockito.ArgumentMatchers.any(LaianeProcuracao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LaianeProcuracao novaProcuracao = service.substabelecer(11L, 2L, true);

        assertThat(novaProcuracao.isComReservaDePoderes()).isTrue();
        assertThat(origem.getStatus()).isEqualTo(LaianeProcuracaoStatus.ATIVA);
    }

    @Test
    void rejeitaSubstabelecimentoDeProcuracaoQueNaoPertenceAoAdvogado() {
        Usuario outroAdvogado = advogado(1L);
        Usuario dono = advogado(99L);
        when(guard.requireAdvogado()).thenReturn(outroAdvogado);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(12L)
                .advogado(dono)
                .status(LaianeProcuracaoStatus.ATIVA)
                .build();
        when(procuracaoRepository.findById(12L)).thenReturn(Optional.of(origem));

        assertThatThrownBy(() -> service.substabelecer(12L, 2L, false))
                .isInstanceOf(SecurityException.class);
    }

    @Test
    void rejeitaSubstabelecimentoDeProcuracaoJaRevogada() {
        Usuario substabelecente = advogado(1L);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(13L)
                .advogado(substabelecente)
                .status(LaianeProcuracaoStatus.REVOGADA)
                .build();
        when(procuracaoRepository.findById(13L)).thenReturn(Optional.of(origem));

        assertThatThrownBy(() -> service.substabelecer(13L, 2L, false))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void rejeitaSubstabelecimentoParaDestinatarioQueNaoEAdvogado() {
        Usuario substabelecente = advogado(1L);
        Usuario destinatarioNaoAdvogado = new Usuario();
        destinatarioNaoAdvogado.setId(3L);
        destinatarioNaoAdvogado.setTipoUsuario(TipoUsuario.CIDADAO);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(14L)
                .advogado(substabelecente)
                .status(LaianeProcuracaoStatus.ATIVA)
                .build();
        when(procuracaoRepository.findById(14L)).thenReturn(Optional.of(origem));
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(destinatarioNaoAdvogado));

        assertThatThrownBy(() -> service.substabelecer(14L, 3L, false))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void rejeitaSubstabelecimentoParaSiMesmo() {
        Usuario substabelecente = advogado(1L);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        LaianeProcuracao origem = LaianeProcuracao.builder()
                .id(15L)
                .advogado(substabelecente)
                .status(LaianeProcuracaoStatus.ATIVA)
                .build();
        when(procuracaoRepository.findById(15L)).thenReturn(Optional.of(origem));

        assertThatThrownBy(() -> service.substabelecer(15L, 1L, false))
                .isInstanceOf(ErroDeValidacaoException.class);
    }

    @Test
    void rejeitaSubstabelecimentoDeProcuracaoInexistente() {
        Usuario substabelecente = advogado(1L);
        when(guard.requireAdvogado()).thenReturn(substabelecente);
        when(procuracaoRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.substabelecer(999L, 2L, false))
                .isInstanceOf(NoSuchElementException.class);
    }
}
