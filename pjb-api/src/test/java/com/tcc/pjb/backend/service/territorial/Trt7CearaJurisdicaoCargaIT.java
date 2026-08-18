package com.tcc.pjb.backend.service.territorial;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.AncoraTerritorial;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class Trt7CearaJurisdicaoCargaIT extends PjbIntegrationTestBase {

    @Autowired
    private CompetenciaTerritorialResolver resolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void fortalezaSedeResolveComAs18VarasDoForumAutranNunes() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2304400", "Fortaleza", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.unidadesElegiveis()).containsExactlyInAnyOrder(
                    "TRT7-0001", "TRT7-0002", "TRT7-0003", "TRT7-0004", "TRT7-0005", "TRT7-0006",
                    "TRT7-0007", "TRT7-0008", "TRT7-0009", "TRT7-0010", "TRT7-0011", "TRT7-0012",
                    "TRT7-0013", "TRT7-0014", "TRT7-0015", "TRT7-0016", "TRT7-0017", "TRT7-0018");
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT7");
        });
    }

    @Test
    void moradaNovaTrabalhaAquiVaraFicaEmLimoeiroDoNorte() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2308708", "Morada Nova", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT7-0023"));
    }

    @Test
    void sobralResolveComAsDuasVarasConcorrentes() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2312908", "Sobral", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis())
                        .containsExactlyInAnyOrder("TRT7-0024", "TRT7-0038"));
    }

    @Test
    void saoLuisDoCuruResolveApesarDaGrafiaDivergenteNoPdfFonte() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2312601", "São Luís do Curu", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT7-0039"));
    }

    @Test
    void juazeiroDoNorteNaRegiaoDoCaririResolveComAsTresVarasConcorrentes() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2307304", "Juazeiro do Norte", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis())
                        .containsExactlyInAnyOrder("TRT7-0027", "TRT7-0028", "TRT7-0037"));
    }

    @Test
    void casoTrabalhistaAntigoAntesDoDocumentoFonteAindaAssimResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2308708", "Morada Nova", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2015, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT7-0023"));
    }

    @Test
    void moradaNovaNoPrimeiroDiaDeVigenciaConstitucionalJaResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2308708", "Morada Nova", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(1988, 10, 5));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT7-0023"));
    }

    @Test
    void cargaDoCearaCobreOs184MunicipiosComOs288ParesDeUnidade() {
        Integer totalMunicipios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_jurisdicao_territorial WHERE tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT7')", Integer.class);
        Integer totalPares = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial_unidade u
                JOIN tb_jurisdicao_territorial j ON j.id = u.jurisdicao_territorial_id
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT7')
                """, Integer.class);
        Integer municipiosSemUnidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial j
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT7')
                  AND NOT EXISTS (
                      SELECT 1 FROM tb_jurisdicao_territorial_unidade u
                      WHERE u.jurisdicao_territorial_id = j.id
                  )
                """, Integer.class);

        assertThat(totalMunicipios).isEqualTo(184);
        assertThat(totalPares).isEqualTo(288);
        assertThat(municipiosSemUnidade).isZero();
    }

    @Test
    void distribuicaoDeUnidadesPorMunicipioSeMantemConformeODocumentoFonte() {
        List<Integer> quantidadePorMunicipio = jdbcTemplate.queryForList("""
                SELECT COUNT(u.unidade_codigo) FROM tb_jurisdicao_territorial j
                JOIN tb_jurisdicao_territorial_unidade u ON u.jurisdicao_territorial_id = j.id
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT7')
                GROUP BY j.id
                """, Integer.class);

        Map<Integer, Long> distribuicao = quantidadePorMunicipio.stream()
                .collect(Collectors.groupingBy(quantidade -> quantidade, Collectors.counting()));

        assertThat(distribuicao).isEqualTo(Map.of(1, 122L, 2, 35L, 3, 26L, 18, 1L));
    }
}
