package com.tcc.pjb.backend.model.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.model.entity.Instituicao;
import com.tcc.pjb.backend.model.entity.UnidadeInstituicao;
import com.tcc.pjb.backend.model.entity.competencia.UnidadeJudiciariaCompetencia;
import com.tcc.pjb.backend.model.entity.enums.TipoInstituicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUnidadeInstitucional;
import com.tcc.pjb.backend.model.repository.InstituicaoRepository;
import com.tcc.pjb.backend.model.repository.UnidadeInstituicaoRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {
        "spring.cache.type=none",
        "pjb.workflow.enabled=false"
})
class UnidadeJudiciariaCompetenciaPonteUnidadeInstituicaoIT extends PjbIntegrationTestBase {

    @Autowired
    private UnidadeJudiciariaCompetenciaRepository unidadeJudiciariaCompetenciaRepository;
    @Autowired
    private UnidadeInstituicaoRepository unidadeInstituicaoRepository;
    @Autowired
    private InstituicaoRepository instituicaoRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void persisteEBuscaAPonteEntreUnidadeCompetenciaEUnidadeInstituicao() {
        String discriminador = UUID.randomUUID().toString().substring(0, 8);
        Long tribunalId = jdbcTemplate.queryForObject(
                "SELECT id FROM tb_tribunal WHERE sigla = 'TJCE'", Long.class);

        Instituicao instituicao = new Instituicao();
        instituicao.setTipo(TipoInstituicao.TRIBUNAL);
        instituicao.setNome("Instituicao Ponte " + discriminador);
        instituicao = instituicaoRepository.save(instituicao);
        UnidadeInstituicao unidadeInstituicao = new UnidadeInstituicao();
        unidadeInstituicao.setInstituicao(instituicao);
        unidadeInstituicao.setNome("Secretaria Ponte " + discriminador);
        unidadeInstituicao.setTipo(TipoUnidadeInstitucional.GENERICO);
        unidadeInstituicao = unidadeInstituicaoRepository.save(unidadeInstituicao);

        Long unidadeCompetenciaId = jdbcTemplate.queryForObject(
                "INSERT INTO tb_unidade_judiciaria_competencia (codigo, tribunal_id, tipo_vara, uf, comarca, versao) " +
                "VALUES (?, ?, 'CIVEL_GERAL', 'CE', ?, 0) RETURNING id",
                Long.class, "VARA-PONTE-" + discriminador, tribunalId, "Fortaleza " + discriminador);

        UnidadeJudiciariaCompetencia unidadeCompetencia = unidadeJudiciariaCompetenciaRepository
                .findById(unidadeCompetenciaId).orElseThrow();
        unidadeCompetencia.setUnidadeInstituicao(unidadeInstituicao);
        unidadeJudiciariaCompetenciaRepository.save(unidadeCompetencia);

        List<UnidadeJudiciariaCompetencia> encontradas = unidadeJudiciariaCompetenciaRepository
                .findByUnidadeInstituicao(unidadeInstituicao);
        assertThat(encontradas).extracting(UnidadeJudiciariaCompetencia::getId).containsExactly(unidadeCompetenciaId);

        List<UnidadeJudiciariaCompetencia> porComarca = unidadeJudiciariaCompetenciaRepository
                .findAllByUfIgnoreCaseAndComarcaIgnoreCase("ce", "fortaleza " + discriminador);
        assertThat(porComarca).extracting(UnidadeJudiciariaCompetencia::getId).containsExactly(unidadeCompetenciaId);
    }
}
