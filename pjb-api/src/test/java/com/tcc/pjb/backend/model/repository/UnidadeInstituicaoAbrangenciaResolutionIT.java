package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstitucionalAbrangencia;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UnidadeInstituicaoAbrangenciaResolutionIT extends PjbIntegrationTestBase {

    @Autowired
    InstituicaoRepository instituicaoRepository;

    @Autowired
    UnidadeInstituicaoRepository unidadeRepository;

    @Autowired
    UnidadeInstitucionalAbrangenciaRepository abrangenciaRepository;

    @Test
    void unidadeSediadaNaComarcaEEncontradaDiretamentePorTipoEComarca() {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.MINISTERIO_PUBLICO);
        instituicao.setNome("Ministerio Publico do Ceara");
        instituicao = instituicaoRepository.save(instituicao);

        UnidadeInstituicao unidade = new UnidadeInstituicao();
        unidade.setInstituicao(instituicao);
        unidade.setNome("1a Promotoria Criminal de Fortaleza");
        unidade.setTipo(TipoUnidadeInstitucional.PROMOTORIA);
        unidade.setComarca("Fortaleza");
        unidade.setUf("CE");
        unidade = unidadeRepository.save(unidade);

        List<UnidadeInstituicao> encontradas = unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROMOTORIA, "Fortaleza");

        assertThat(encontradas).extracting(UnidadeInstituicao::getId).containsExactly(unidade.getId());
    }

    @Test
    void unidadeRegionalECobreComarcaSemUnidadePropriaViaAbrangencia() {
        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.DEFENSORIA_PUBLICA);
        instituicao.setNome("Defensoria Publica do Ceara");
        instituicao = instituicaoRepository.save(instituicao);

        UnidadeInstituicao regional = new UnidadeInstituicao();
        regional.setInstituicao(instituicao);
        regional.setNome("Nucleo Regional da Defensoria");
        regional.setTipo(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA);
        regional.setComarca("Fortaleza");
        regional.setUf("CE");
        regional = unidadeRepository.save(regional);

        UnidadeInstitucionalAbrangencia abrangencia = new UnidadeInstitucionalAbrangencia();
        abrangencia.setUnidadeInstitucionalId(regional.getId());
        abrangencia.setComarcaAtendida("Aquiraz");
        abrangenciaRepository.save(abrangencia);

        List<UnidadeInstitucionalAbrangencia> cobertura = abrangenciaRepository.findByUnidadeInstitucionalId(regional.getId());

        assertThat(cobertura).extracting(UnidadeInstitucionalAbrangencia::getComarcaAtendida).containsExactly("Aquiraz");
        assertThat(unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.NUCLEO_DEFENSORIA, "Aquiraz")).isEmpty();
    }

    @Test
    void comarcaSemNenhumaUnidadeSediadaRetornaListaVazia() {
        List<UnidadeInstituicao> encontradas = unidadeRepository.findByTipoAndComarca(TipoUnidadeInstitucional.PROCURADORIA_PUBLICA, "Comarca Sem Cobertura Nenhuma");

        assertThat(encontradas).isEmpty();
    }
}
