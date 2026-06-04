package com.tcc.pjb.backend.core.security.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.criminal.BoletimOcorrenciaDigital;
import com.tcc.pjb.backend.model.entity.criminal.InqueritoPolicialDigital;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DelegaciaInstitucionalScopeServiceTest {

    private final UnidadeInstituicaoRepository unidadeRepository = mock(UnidadeInstituicaoRepository.class);
    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final DelegaciaInstitucionalScopeService service = new DelegaciaInstitucionalScopeService(
            unidadeRepository,
            lotacaoRepository
    );

    @Test
    void requireDelegaciaAtivaComTerritorioUnidadeNula_nega() {
        assertThrows(IllegalStateException.class, () -> service.requireDelegaciaAtivaComTerritorio(null));
    }

    @Test
    void requireDelegaciaRegistroLotadaSemLotacaoAtiva_nega() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegacia = delegacia(10L);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> service.requireDelegaciaRegistroLotada(usuario, 10L));
    }

    @Test
    void requireDelegaciaRegistroLotadaComLotacaoDireta_permite() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegacia = delegacia(10L);
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(delegacia)));

        UnidadeInstituicao resolved = service.requireDelegaciaRegistroLotada(usuario, 10L);

        assertThat(resolved).isSameAs(delegacia);
    }

    @Test
    void delegaciasAtivasComTerritorioDoUsuarioFiltraNuloForumInativaESemTerritorio() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegacia = delegacia(10L);
        UnidadeInstituicao forum = unidade(11L, TipoUnidadeInstitucional.FORUM, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        UnidadeInstituicao inativa = unidade(12L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.INATIVA, "Fortaleza", "CE");
        UnidadeInstituicao semTerritorio = unidade(13L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA, null, "CE");
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(
                lotacao(null),
                lotacao(forum),
                lotacao(inativa),
                lotacao(semTerritorio),
                lotacao(delegacia)
        ));

        List<UnidadeInstituicao> unidades = service.delegaciasAtivasComTerritorioDoUsuario(usuario);

        assertThat(unidades).containsExactly(delegacia);
    }

    @Test
    void unidadesAtivasDoUsuarioAtualPreservaComportamentoFrouxoConhecido() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegacia = delegacia(10L);
        UnidadeInstituicao forum = unidade(11L, TipoUnidadeInstitucional.FORUM, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        UnidadeInstituicao semTerritorio = unidade(12L, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA, null, "CE");
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(
                lotacao(null),
                lotacao(forum),
                lotacao(semTerritorio),
                lotacao(delegacia)
        ));

        List<UnidadeInstituicao> unidades = service.unidadesAtivasDoUsuarioAtual(usuario);

        assertThat(unidades).containsExactly(forum, semTerritorio, delegacia);
    }

    @Test
    void requireDelegaciaApuracaoLotadaUnidadeInvalidaPreservaIllegalArgument() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao forum = unidade(10L, TipoUnidadeInstitucional.FORUM, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        when(unidadeRepository.findById(10L)).thenReturn(Optional.of(forum));

        assertThrows(IllegalArgumentException.class, () -> service.requireDelegaciaApuracaoLotada(usuario, 10L));
    }

    @Test
    void hasLotacaoDiretaNaUnidadeAtualComLotacaoEmOutraUnidade_nega() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao alvo = delegacia(10L);
        UnidadeInstituicao outra = delegacia(11L);
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(outra)));

        boolean allowed = service.hasLotacaoDiretaNaUnidadeAtual(usuario, alvo);

        assertThat(allowed).isFalse();
    }

    @Test
    void requireMesmoRegistroInstitucionalMesmaDelegacia_permite() {
        UnidadeInstituicao delegacia = delegacia(10L);
        BoletimOcorrenciaDigital boletim = boletim(delegacia);
        InqueritoPolicialDigital inquerito = inquerito(delegacia);

        service.requireMesmoRegistroInstitucional(boletim, inquerito);
    }

    @Test
    void requireMesmoRegistroInstitucionalDelegaciaDivergente_nega() {
        BoletimOcorrenciaDigital boletim = boletim(delegacia(10L));
        InqueritoPolicialDigital inquerito = inquerito(delegacia(11L));

        assertThrows(IllegalStateException.class, () -> service.requireMesmoRegistroInstitucional(boletim, inquerito));
    }

    private Usuario usuario(Long id) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        return usuario;
    }

    private LotacaoInstituicao lotacao(UnidadeInstituicao unidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        return lotacao;
    }

    private UnidadeInstituicao delegacia(Long id) {
        return unidade(id, TipoUnidadeInstitucional.DELEGACIA, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
    }

    private UnidadeInstituicao unidade(Long id,
                                       TipoUnidadeInstitucional tipo,
                                       StatusUnidadeInstitucional status,
                                       String comarca,
                                       String uf) {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        ReflectionTestUtils.setField(unidade, "id", id);
        unidade.setTipo(tipo);
        unidade.setStatusUnidade(status);
        unidade.setComarca(comarca);
        unidade.setUf(uf);
        return unidade;
    }

    private BoletimOcorrenciaDigital boletim(UnidadeInstituicao unidade) {
        BoletimOcorrenciaDigital boletim = new BoletimOcorrenciaDigital();
        boletim.setUnidadeRegistro(unidade);
        return boletim;
    }

    private InqueritoPolicialDigital inquerito(UnidadeInstituicao unidade) {
        InqueritoPolicialDigital inquerito = new InqueritoPolicialDigital();
        inquerito.setUnidadeApuracao(unidade);
        return inquerito;
    }
}
