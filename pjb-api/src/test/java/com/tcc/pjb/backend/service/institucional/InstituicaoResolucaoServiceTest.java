package com.tcc.pjb.backend.service.institucional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstituicaoResolucaoServiceTest {

    private InstituicaoResolucaoService service(InstituicaoRepository repo) {
        return new InstituicaoResolucaoService(repo);
    }

    @Test
    void kind_ministerio_publico_resolve_para_tipo_correto() {
        var repo = mock(InstituicaoRepository.class);
        Instituicao mp = new Instituicao();
        mp.setNome("MP Estadual — FICTÍCIO");
        mp.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        when(repo.findByTipo(TipoInstituicao.MINISTERIO_PUBLICO)).thenReturn(List.of(mp));

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO);

        assertEquals(1, resultado.size());
        assertEquals(TipoInstituicao.MINISTERIO_PUBLICO, resultado.get(0).getTipo());
    }

    @Test
    void delegacia_civil_consolida_para_delegacia_policia() {
        var repo = mock(InstituicaoRepository.class);
        Instituicao del = new Instituicao();
        del.setTipo(TipoInstituicao.DELEGACIA_POLICIA);
        when(repo.findByTipo(TipoInstituicao.DELEGACIA_POLICIA)).thenReturn(List.of(del));

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.DELEGACIA_POLICIA_CIVIL);

        assertEquals(1, resultado.size());
        verify(repo).findByTipo(TipoInstituicao.DELEGACIA_POLICIA);
    }

    @Test
    void perito_judicial_traduz_para_orgao_pericial() {
        var repo = mock(InstituicaoRepository.class);
        when(repo.findByTipo(TipoInstituicao.ORGAO_PERICIAL)).thenReturn(List.of());

        service(repo).resolverPorKind(DestinatarioInstitucionalKind.PERITO_JUDICIAL);

        verify(repo).findByTipo(TipoInstituicao.ORGAO_PERICIAL);
    }

    @Test
    void contadoria_judicial_retorna_lista_vazia_sem_chamar_repositorio() {
        var repo = mock(InstituicaoRepository.class);

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.CONTADORIA_JUDICIAL);

        assertTrue(resultado.isEmpty());
        verify(repo, never()).findByTipo(TipoInstituicao.OUTRO);
    }

    @Test
    void equipe_psicossocial_retorna_lista_vazia_sem_chamar_repositorio() {
        var repo = mock(InstituicaoRepository.class);

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL);

        assertTrue(resultado.isEmpty());
        verify(repo, never()).findByTipo(TipoInstituicao.OUTRO);
    }

    @Test
    void juizo_deprecado_retorna_lista_vazia() {
        var repo = mock(InstituicaoRepository.class);

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.JUIZO_DEPRECADO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void orgao_judicial_externo_retorna_lista_vazia() {
        var repo = mock(InstituicaoRepository.class);

        List<Instituicao> resultado = service(repo).resolverPorKind(DestinatarioInstitucionalKind.ORGAO_JUDICIAL_EXTERNO);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void kind_null_retorna_lista_vazia_sem_excecao() {
        var repo = mock(InstituicaoRepository.class);

        List<Instituicao> resultado = service(repo).resolverPorKind(null);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void enum_destinatario_mantém_método_isInstituicaoEssencialJustica_intacto() {
        assertTrue(DestinatarioInstitucionalKind.MINISTERIO_PUBLICO.isInstituicaoEssencialJustica());
        assertTrue(DestinatarioInstitucionalKind.DEFENSORIA_PUBLICA.isInstituicaoEssencialJustica());
        assertTrue(DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA.isInstituicaoEssencialJustica());
    }

    @Test
    void enum_destinatario_tem_23_valores_sem_remocao() {
        assertEquals(23, DestinatarioInstitucionalKind.values().length);
    }
}
