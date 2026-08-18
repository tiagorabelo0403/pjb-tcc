package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.competencia.Tribunal;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.repository.TribunalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class JurisdicaoTerritorialCatalogoConstraintIT extends PjbIntegrationTestBase {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TribunalRepository tribunalRepository;

    @BeforeEach
    void limparCatalogo() {
        jdbcTemplate.update("DELETE FROM tb_jurisdicao_territorial WHERE municipio_ibge = ?", "0000001");
    }

    @Test
    void exclusaoRejeitaSobreposicaoRealDeJurisdicao() {
        inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "ORIGINARIA", "TRT7-0023", "TESTE",
                "TRT7, jurisdicao de unidades", "2010-01-01", null);

        assertThatThrownBy(() -> inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "ORIGINARIA",
                "TRT7-0099", "TESTE", "TRT7, jurisdicao de unidades", "2015-01-01", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void exclusaoAceitaContinuidadeLegitimaNaFronteiraDeVigencia() {
        inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "ORIGINARIA", "TRT7-0023", "TESTE",
                "TRT7, jurisdicao de unidades", "2010-01-01", "2020-06-30");

        assertThatCode(() -> inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "ORIGINARIA",
                "TRT7-0024", "TESTE", "TRT7, jurisdicao de unidades", "2020-06-30", null))
                .doesNotThrowAnyException();
    }

    @Test
    void checkIbgeRejeitaFormatoInvalido() {
        assertThatThrownBy(() -> inserir("123", "Morada Nova", "CE", "TRABALHO", "ORIGINARIA",
                "TRT7-0023", "TESTE", "TRT7, jurisdicao de unidades", "2010-01-01", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkUfRejeitaFormatoInvalido() {
        assertThatThrownBy(() -> inserir("0000001", "Municipio Teste", "ce", "TRABALHO", "ORIGINARIA",
                "TRT7-0023", "TESTE", "TRT7, jurisdicao de unidades", "2010-01-01", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkTipoJusticaRejeitaValorForaDaLista() {
        assertThatThrownBy(() -> inserir("0000001", "Municipio Teste", "CE", "INEXISTENTE", "ORIGINARIA",
                "TRT7-0023", "TESTE", "TRT7, jurisdicao de unidades", "2010-01-01", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkModoCompetenciaRejeitaValorForaDaLista() {
        assertThatThrownBy(() -> inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "INEXISTENTE",
                "TRT7-0023", "TESTE", "TRT7, jurisdicao de unidades", "2010-01-01", null))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkVigenciaRejeitaFimAnteriorOuIgualAoInicio() {
        assertThatThrownBy(() -> inserir("0000001", "Municipio Teste", "CE", "TRABALHO", "ORIGINARIA",
                "TRT7-0023", "TESTE", "TRT7, jurisdicao de unidades", "2010-01-01", "2010-01-01"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private void inserir(String municipioIbge, String municipioNome, String uf, String tipoJustica,
            String modoCompetencia, String unidadeCodigo, String tribunalCodigo, String fonteNormativa,
            String vigenciaInicio, String vigenciaFim) {
        Long tribunalId = tribunalRepository.findBySigla(tribunalCodigo)
                .orElseGet(() -> tribunalRepository.save(new Tribunal(tribunalCodigo, "Tribunal de Teste",
                        TipoJustica.TRABALHO, GrauJurisdicao.SEGUNDO_GRAU, "CE")))
                .getId();
        Long jurisdicaoId = jdbcTemplate.queryForObject("""
                INSERT INTO tb_jurisdicao_territorial
                    (municipio_ibge, municipio_nome, uf, tipo_justica, modo_competencia,
                     tribunal_id, fonte_normativa, vigencia_inicio, vigencia_fim)
                VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS DATE), CAST(? AS DATE))
                RETURNING id
                """,
                Long.class,
                municipioIbge, municipioNome, uf, tipoJustica, modoCompetencia,
                tribunalId, fonteNormativa, vigenciaInicio, vigenciaFim);

        jdbcTemplate.update("""
                INSERT INTO tb_jurisdicao_territorial_unidade (jurisdicao_territorial_id, unidade_codigo)
                VALUES (?, ?)
                """,
                jurisdicaoId, unidadeCodigo);
    }
}
