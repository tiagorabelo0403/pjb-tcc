package com.tcc.pjb.backend.model.entity;

import com.tcc.pjb.backend.model.entity.enums.StatusInstituicao;
import com.tcc.pjb.backend.model.entity.enums.StatusUnidadeInstitucional;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstituicaoArvoreFase1Test {

    @Test
    void unidade_filha_resolve_instituicao_diretamente() {
        Instituicao inst = new Instituicao();
        inst.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        inst.setNome("Ministério Público Estadual — FICTÍCIO");
        inst.setStatus(StatusInstituicao.ATIVA);

        UnidadeInstituicao raiz = new UnidadeInstituicao();
        raiz.setInstituicao(inst);
        raiz.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        raiz.setNome("Promotoria Capital");
        raiz.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);

        UnidadeInstituicao filha = new UnidadeInstituicao();
        filha.setInstituicao(inst);
        filha.setParent(raiz);
        filha.setTipo(TipoUnidadeInstitucional.GENERICO);
        filha.setNome("Núcleo de Apoio");
        filha.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);

        assertSame(inst, filha.getInstituicao());
        assertSame(raiz, filha.getParent());
    }

    @Test
    void lotacao_sem_fim_eh_ativa() {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setInicio(LocalDate.of(2024, 1, 1));
        lotacao.setFim(null);
        assertTrue(lotacao.isAtiva());
    }

    @Test
    void lotacao_com_fim_nao_eh_ativa() {
        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setInicio(LocalDate.of(2023, 1, 1));
        lotacao.setFim(LocalDate.of(2024, 12, 31));
        assertFalse(lotacao.isAtiva());
    }

    @Test
    void status_instituicao_suspensa_distingue_de_ativa() {
        Instituicao inst = new Instituicao();
        inst.setStatus(StatusInstituicao.SUSPENSA);
        assertNotEquals(StatusInstituicao.ATIVA, inst.getStatus());
        assertEquals(StatusInstituicao.SUSPENSA, inst.getStatus());
    }

    @Test
    void status_instituicao_inativa_distingue_de_ativa_e_suspensa() {
        Instituicao inst = new Instituicao();
        inst.setStatus(StatusInstituicao.INATIVA);
        assertNotEquals(StatusInstituicao.ATIVA, inst.getStatus());
        assertNotEquals(StatusInstituicao.SUSPENSA, inst.getStatus());
    }

    @Test
    void unidade_inativa_distingue_de_ativa() {
        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setStatusUnidade(StatusUnidadeInstitucional.INATIVA);
        assertNotEquals(StatusUnidadeInstitucional.ATIVA, unidade.getStatusUnidade());
    }

    @Test
    void unidade_raiz_nao_tem_parent() {
        Instituicao inst = new Instituicao();
        inst.setTipo(TipoInstituicao.DEFENSORIA_PUBLICA);
        inst.setNome("Defensoria Pública Estadual — FICTÍCIO");

        UnidadeInstituicao raiz = new UnidadeInstituicao();
        raiz.setInstituicao(inst);
        raiz.setParent(null);
        raiz.setTipo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        raiz.setNome("Núcleo Central");
        raiz.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);

        assertNull(raiz.getParent());
        assertSame(inst, raiz.getInstituicao());
    }

    @Test
    void tipo_unidade_cobre_casos_necessarios() {
        UnidadeInstituicao u = new UnidadeInstituicao();
        u.setTipo(TipoUnidadeInstitucional.DELEGACIA);
        assertEquals(TipoUnidadeInstitucional.DELEGACIA, u.getTipo());

        u.setTipo(TipoUnidadeInstitucional.CENTRAL_MANDADOS);
        assertEquals(TipoUnidadeInstitucional.CENTRAL_MANDADOS, u.getTipo());

        u.setTipo(TipoUnidadeInstitucional.CEJUSC);
        assertEquals(TipoUnidadeInstitucional.CEJUSC, u.getTipo());
    }

    @Test
    void lotacao_aditiva_nao_interfere_com_tipo_usuario() {
        Usuario usuario = new Usuario();
        usuario.setTipoUsuario(com.tcc.pjb.backend.model.entity.enums.TipoUsuario.MEMBRO_MINISTERIO_PUBLICO);

        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setNome("Promotoria de Teste");
        unidade.setStatusUnidade(StatusUnidadeInstitucional.ATIVA);

        LotacaoInstituicao lotacao = new LotacaoInstituicao();
        lotacao.setUsuario(usuario);
        lotacao.setUnidade(unidade);
        lotacao.setInicio(LocalDate.of(2024, 1, 1));
        lotacao.setFim(null);

        assertEquals(com.tcc.pjb.backend.model.entity.enums.TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
                usuario.getTipoUsuario());
        assertTrue(lotacao.isAtiva());
        assertSame(usuario, lotacao.getUsuario());
    }
}
