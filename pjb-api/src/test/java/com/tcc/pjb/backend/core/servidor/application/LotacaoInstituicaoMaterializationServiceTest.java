package com.tcc.pjb.backend.core.servidor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.FuncaoServidorJudiciario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeJudiciariaCompetenciaRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LotacaoInstituicaoMaterializationServiceTest {

    private UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    private UsuarioRepository usuarioRepository;
    private LotacaoInstituicaoRepository lotacaoInstituicaoRepository;
    private LotacaoInstituicaoMaterializationService service;

    @BeforeEach
    void setUp() {
        unidadeJudiciariaCompetenciaRepository = mock(UnidadeJudiciariaCompetenciaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        lotacaoInstituicaoRepository = mock(LotacaoInstituicaoRepository.class);
        service = new LotacaoInstituicaoMaterializationService(unidadeJudiciariaCompetenciaRepository,
                usuarioRepository, lotacaoInstituicaoRepository);
    }

    @Test
    void semPonteNaoMaterializaLotacao() {
        var unidade = unidadeSemPonte(5L);
        when(unidadeJudiciariaCompetenciaRepository.findById(5L)).thenReturn(Optional.of(unidade));

        service.materializarLotacaoSePonteExistir(10L, 5L, FuncaoServidorJudiciario.ESCRIVAO_JUDICIAL, LocalDate.now());

        verify(lotacaoInstituicaoRepository, never()).save(any());
    }

    @Test
    void comPonteMaterializaLotacaoNova() {
        UnidadeInstituicao unidadeInstituicao = new UnidadeInstituicao();
        var unidade = unidadeComPonte(5L, unidadeInstituicao);
        when(unidadeJudiciariaCompetenciaRepository.findById(5L)).thenReturn(Optional.of(unidade));
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        when(lotacaoInstituicaoRepository.findFirstByUsuarioAndUnidadeOrderByInicioDesc(usuario, unidadeInstituicao))
                .thenReturn(Optional.empty());

        service.materializarLotacaoSePonteExistir(10L, 5L, FuncaoServidorJudiciario.DIRETOR_SECRETARIA, LocalDate.now());

        var captor = org.mockito.ArgumentCaptor.forClass(LotacaoInstituicao.class);
        verify(lotacaoInstituicaoRepository).save(captor.capture());
        assertThat(captor.getValue().getUsuario()).isEqualTo(usuario);
        assertThat(captor.getValue().getUnidade()).isEqualTo(unidadeInstituicao);
        assertThat(captor.getValue().getFim()).isNull();
        assertThat(captor.getValue().getPapelNaUnidade()).isEqualTo(FuncaoServidorJudiciario.DIRETOR_SECRETARIA.label());
    }

    @Test
    void comPonteReativaLotacaoEncerradaExistente() {
        UnidadeInstituicao unidadeInstituicao = new UnidadeInstituicao();
        var unidade = unidadeComPonte(5L, unidadeInstituicao);
        when(unidadeJudiciariaCompetenciaRepository.findById(5L)).thenReturn(Optional.of(unidade));
        Usuario usuario = new Usuario();
        usuario.setId(10L);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));
        LotacaoInstituicao existente = new LotacaoInstituicao();
        existente.setFim(LocalDate.now().minusDays(1));
        when(lotacaoInstituicaoRepository.findFirstByUsuarioAndUnidadeOrderByInicioDesc(usuario, unidadeInstituicao))
                .thenReturn(Optional.of(existente));

        LocalDate dataInicio = LocalDate.now();
        service.materializarLotacaoSePonteExistir(10L, 5L, FuncaoServidorJudiciario.OFICIAL_MAIOR, dataInicio);

        var captor = org.mockito.ArgumentCaptor.forClass(LotacaoInstituicao.class);
        verify(lotacaoInstituicaoRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(captor.getValue().getFim()).isNull();
        assertThat(captor.getValue().getInicio()).isEqualTo(dataInicio);
        assertThat(captor.getValue().getPapelNaUnidade()).isEqualTo(FuncaoServidorJudiciario.OFICIAL_MAIOR.label());
    }

    private UnidadeJudiciariaCompetencia unidadeSemPonte(Long id) {
        var unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getUnidadeInstituicao()).thenReturn(null);
        return unidade;
    }

    private UnidadeJudiciariaCompetencia unidadeComPonte(Long id, UnidadeInstituicao unidadeInstituicao) {
        var unidade = mock(UnidadeJudiciariaCompetencia.class);
        when(unidade.getUnidadeInstituicao()).thenReturn(unidadeInstituicao);
        return unidade;
    }
}
