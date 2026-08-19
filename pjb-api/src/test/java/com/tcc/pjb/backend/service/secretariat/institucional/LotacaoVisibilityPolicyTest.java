package com.tcc.pjb.backend.service.secretariat.institucional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.LotacaoInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.LotacaoInstituicaoRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class LotacaoVisibilityPolicyTest {

    private final LotacaoInstituicaoRepository lotacaoRepository = mock(LotacaoInstituicaoRepository.class);
    private final LotacaoVisibilityPolicy policy = new LotacaoVisibilityPolicy(lotacaoRepository);

    @Test
    void usuarioComLotacaoAtivaNaUnidadeExataVePermitido() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        UnidadeInstituicao unidade = unidade(1L);
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(promotor);
        lotacao.setUnidade(unidade);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        when(lotacaoRepository.findAtivasByUsuario(promotor)).thenReturn(List.of(lotacao));

        assertThat(policy.podeVer(promotor, unidade)).isTrue();
    }

    @Test
    void usuarioSemLotacaoNaquelaUnidadeNaoVe() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        UnidadeInstituicao unidadeDoUsuario = unidade(1L);
        UnidadeInstituicao unidadeAlvo = unidade(2L);
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(promotor);
        lotacao.setUnidade(unidadeDoUsuario);
        lotacao.setInicio(LocalDate.now().minusYears(1));
        when(lotacaoRepository.findAtivasByUsuario(promotor)).thenReturn(List.of(lotacao));

        assertThat(policy.podeVer(promotor, unidadeAlvo)).isFalse();
    }

    @Test
    void usuarioSemNenhumaLotacaoAtivaNaoVeNenhumaUnidade() {
        Usuario promotor = usuario(TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);
        when(lotacaoRepository.findAtivasByUsuario(promotor)).thenReturn(List.of());

        assertThat(policy.podeVer(promotor, unidade(1L))).isFalse();
    }

    @Test
    void administradorVeQualquerUnidadeSemConsultarLotacao() {
        Usuario admin = usuario(TipoUsuario.ADMINISTRADOR);

        assertThat(policy.podeVer(admin, unidade(1L))).isTrue();
        org.mockito.Mockito.verify(lotacaoRepository, org.mockito.Mockito.never()).findAtivasByUsuario(org.mockito.ArgumentMatchers.any());
    }

    private Usuario usuario(TipoUsuario tipo) {
        Usuario u = new Usuario();
        u.setId(1L);
        u.setTipoUsuario(tipo);
        return u;
    }

    private UnidadeInstituicao unidade(Long id) {
        UnidadeInstituicao u = new UnidadeInstituicao();
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }
}
