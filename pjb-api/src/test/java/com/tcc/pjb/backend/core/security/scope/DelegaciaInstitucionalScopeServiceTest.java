package com.tcc.pjb.backend.core.security.scope;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import java.nio.file.Files;
import java.nio.file.Path;
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
        verify(unidadeRepository, never()).findAncestorIdsInclusive(anyLong());
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
    void requireDelegaciaRegistroLotadaPorDepartamentoAncestral_permite() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao departamento = unidade(20L, TipoUnidadeInstitucional.DEPARTAMENTO_POLICIA, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        UnidadeInstituicao delegacia = delegacia(30L);
        when(unidadeRepository.findById(30L)).thenReturn(Optional.of(delegacia));
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(departamento)));
        when(unidadeRepository.findAncestorIdsInclusive(30L)).thenReturn(List.of(30L, 20L, 10L));

        UnidadeInstituicao resolved = service.requireDelegaciaRegistroLotada(usuario, 30L);

        assertThat(resolved).isSameAs(delegacia);
    }

    @Test
    void hasEscopoHierarquicoSecretariaAcessaSetorProfundo() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao secretaria = unidade(10L, TipoUnidadeInstitucional.SECRETARIA_SEGURANCA, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        UnidadeInstituicao setor = unidade(40L, TipoUnidadeInstitucional.SETOR, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(secretaria)));
        when(unidadeRepository.findAncestorIdsInclusive(40L)).thenReturn(List.of(40L, 30L, 20L, 10L));

        boolean allowed = service.hasEscopoHierarquico(usuario, setor);

        assertThat(allowed).isTrue();
    }

    @Test
    void requireDelegaciaRegistroLotadaUnidadeIrma_nega() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegaciaAlvo = delegacia(30L);
        UnidadeInstituicao delegaciaIrma = delegacia(31L);
        when(unidadeRepository.findById(30L)).thenReturn(Optional.of(delegaciaAlvo));
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(delegaciaIrma)));
        when(unidadeRepository.findAncestorIdsInclusive(30L)).thenReturn(List.of(30L, 20L, 10L));

        assertThrows(IllegalStateException.class, () -> service.requireDelegaciaRegistroLotada(usuario, 30L));
    }

    @Test
    void hasEscopoHierarquicoObjetoSemUnidade_negaSemConsultarCte() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao semId = delegacia(null);
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(delegacia(10L))));

        boolean allowed = service.hasEscopoHierarquico(usuario, semId);

        assertThat(allowed).isFalse();
        verify(unidadeRepository, never()).findAncestorIdsInclusive(anyLong());
    }

    @Test
    void hasEscopoHierarquicoLotacaoVazia_negaSemConsultarCte() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao delegacia = delegacia(30L);
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of());

        boolean allowed = service.hasEscopoHierarquico(usuario, delegacia);

        assertThat(allowed).isFalse();
        verify(unidadeRepository, never()).findAncestorIdsInclusive(anyLong());
    }

    @Test
    void hasEscopoHierarquicoLotacaoSemTerritorioNaoOpina() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao departamentoSemTerritorio = unidade(20L, TipoUnidadeInstitucional.DEPARTAMENTO_POLICIA, StatusUnidadeInstitucional.ATIVA, null, "CE");
        UnidadeInstituicao delegacia = delegacia(30L);
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(departamentoSemTerritorio)));

        boolean allowed = service.hasEscopoHierarquico(usuario, delegacia);

        assertThat(allowed).isFalse();
        verify(unidadeRepository, never()).findAncestorIdsInclusive(anyLong());
    }

    @Test
    void hasEscopoHierarquicoParentNuloNaCadeiaNaoLiberaUnidadeForaDaCadeia() {
        Usuario usuario = usuario(1L);
        UnidadeInstituicao secretaria = unidade(10L, TipoUnidadeInstitucional.SECRETARIA_SEGURANCA, StatusUnidadeInstitucional.ATIVA, "Fortaleza", "CE");
        UnidadeInstituicao delegacia = delegacia(30L);
        when(lotacaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao(secretaria)));
        when(unidadeRepository.findAncestorIdsInclusive(30L)).thenReturn(List.of(30L));

        boolean allowed = service.hasEscopoHierarquico(usuario, delegacia);

        assertThat(allowed).isFalse();
    }

    @Test
    void queryDeAncestraisMantemProtecaoContraCiclo() throws Exception {
        Path path = Path.of("src/main/java/com/tcc/pjb/backend/model/repository/UnidadeInstituicaoRepository.java");
        if (!Files.exists(path)) {
            path = Path.of("pjb-api/src/main/java/com/tcc/pjb/backend/model/repository/UnidadeInstituicaoRepository.java");
        }

        String source = Files.readString(path);

        assertThat(source)
                .contains("unidade_ancestral.depth < 50")
                .contains("NOT parent.id = ANY(unidade_ancestral.path)");
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
