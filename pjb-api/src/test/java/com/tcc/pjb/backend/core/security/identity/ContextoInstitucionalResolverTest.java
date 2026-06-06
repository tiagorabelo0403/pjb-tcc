package com.tcc.pjb.backend.core.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ContextoInstitucionalResolverTest {

    private final LotacaoInstituicaoRepository lotacaoInstituicaoRepository =
            mock(LotacaoInstituicaoRepository.class);
    private final ContextoInstitucionalResolver resolver =
            new ContextoInstitucionalResolver(lotacaoInstituicaoRepository);

    @Test
    void umaLotacaoAtivaComPapelResolveContexto() {
        Usuario usuario = new Usuario();
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        LotacaoInstituicao lotacao = lotacao(unidade, "DELEGADO");
        when(lotacaoInstituicaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao));

        ContextoResolucao resolucao = resolver.resolver(usuario);

        assertThat(resolucao).isInstanceOf(ContextoResolvido.class);
        ContextoInstitucional contexto = ((ContextoResolvido) resolucao).contexto();
        assertThat(contexto.unidade()).isSameAs(unidade);
        assertThat(contexto.papelNaUnidade()).isEqualTo("DELEGADO");
    }

    @Test
    void umaLotacaoAtivaSemPapelNegaContexto() {
        Usuario usuario = new Usuario();
        LotacaoInstituicao lotacao = lotacao(new UnidadeInstituicao(), " ");
        when(lotacaoInstituicaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of(lotacao));

        ContextoResolucao resolucao = resolver.resolver(usuario);

        assertThat(resolucao).isEqualTo(new ContextoNegado(MotivoContexto.SEM_PAPEL_NA_UNIDADE));
    }

    @Test
    void multiplasLotacoesAtivasExigemSelecaoSemFiltrar() {
        Usuario usuario = new Usuario();
        LotacaoInstituicao primeira = lotacao(new UnidadeInstituicao(), null);
        LotacaoInstituicao segunda = lotacao(new UnidadeInstituicao(), "DELEGADO");
        when(lotacaoInstituicaoRepository.findAtivasByUsuario(usuario))
                .thenReturn(List.of(primeira, segunda));

        ContextoResolucao resolucao = resolver.resolver(usuario);

        assertThat(resolucao).isInstanceOf(PendenteSelecao.class);
        assertThat(((PendenteSelecao) resolucao).lotacoesAtivas())
                .containsExactly(primeira, segunda);
    }

    @Test
    void nenhumaLotacaoAtivaNegaContexto() {
        Usuario usuario = new Usuario();
        when(lotacaoInstituicaoRepository.findAtivasByUsuario(usuario)).thenReturn(List.of());

        ContextoResolucao resolucao = resolver.resolver(usuario);

        assertThat(resolucao).isEqualTo(new ContextoNegado(MotivoContexto.SEM_LOTACAO_ATIVA));
    }

    private static LotacaoInstituicao lotacao(UnidadeInstituicao unidade, String papelNaUnidade) {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUnidade(unidade);
        lotacao.setPapelNaUnidade(papelNaUnidade);
        return lotacao;
    }
}
