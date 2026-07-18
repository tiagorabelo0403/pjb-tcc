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

class Trt3MgJurisdicaoCargaIT extends PjbIntegrationTestBase {

    @Autowired
    private CompetenciaTerritorialResolver resolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void beloHorizonteSedeResolveComAs48VarasDoForumBhCentro() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("3106200", "Belo Horizonte", "MG"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.unidadesElegiveis()).containsExactlyInAnyOrder(
                    "TRT3-0001", "TRT3-0002", "TRT3-0003", "TRT3-0004", "TRT3-0005", "TRT3-0006",
                    "TRT3-0007", "TRT3-0008", "TRT3-0009", "TRT3-0010", "TRT3-0011", "TRT3-0012",
                    "TRT3-0013", "TRT3-0014", "TRT3-0015", "TRT3-0016", "TRT3-0017", "TRT3-0018",
                    "TRT3-0019", "TRT3-0020", "TRT3-0021", "TRT3-0022", "TRT3-0023", "TRT3-0024",
                    "TRT3-0025", "TRT3-0105", "TRT3-0106", "TRT3-0107", "TRT3-0108", "TRT3-0109",
                    "TRT3-0110", "TRT3-0111", "TRT3-0112", "TRT3-0113", "TRT3-0114", "TRT3-0136",
                    "TRT3-0137", "TRT3-0138", "TRT3-0139", "TRT3-0140", "TRT3-0179", "TRT3-0180",
                    "TRT3-0181", "TRT3-0182", "TRT3-0183", "TRT3-0184", "TRT3-0185", "TRT3-0186");
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT3");
        });
    }

    @Test
    void miradouroTrabalhaAquiVaraFicaEmMuriae() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("3142106", "Miradouro", "MG"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT3-0068"));
    }

    @Test
    void betimResolveComAsSeisVarasConcorrentes() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("3106705", "Betim", "MG"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactlyInAnyOrder(
                        "TRT3-0026", "TRT3-0027", "TRT3-0028", "TRT3-0087", "TRT3-0142", "TRT3-0163"));
    }

    @Test
    void casoTrabalhistaAntigoAntesDoDocumentoFonteAindaAssimResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("3142106", "Miradouro", "MG"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2015, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT3-0068"));
    }

    @Test
    void miradouroNoPrimeiroDiaDeVigenciaConstitucionalJaResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("3142106", "Miradouro", "MG"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(1988, 10, 5));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT3-0068"));
    }

    @Test
    void cargaDeMgCobreOs847MunicipiosComOs1498ParesDeUnidade() {
        Integer totalMunicipios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_jurisdicao_territorial WHERE tribunal_codigo = 'TRT3'", Integer.class);
        Integer totalPares = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial_unidade u
                JOIN tb_jurisdicao_territorial j ON j.id = u.jurisdicao_territorial_id
                WHERE j.tribunal_codigo = 'TRT3'
                """, Integer.class);
        Integer municipiosSemUnidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial j
                WHERE j.tribunal_codigo = 'TRT3'
                  AND NOT EXISTS (
                      SELECT 1 FROM tb_jurisdicao_territorial_unidade u
                      WHERE u.jurisdicao_territorial_id = j.id
                  )
                """, Integer.class);

        assertThat(totalMunicipios).isEqualTo(847);
        assertThat(totalPares).isEqualTo(1498);
        assertThat(municipiosSemUnidade).isZero();
    }

    @Test
    void distribuicaoDeUnidadesPorMunicipioSeMantemConformeODocumentoFonte() {
        List<Integer> quantidadePorMunicipio = jdbcTemplate.queryForList("""
                SELECT COUNT(u.unidade_codigo) FROM tb_jurisdicao_territorial j
                JOIN tb_jurisdicao_territorial_unidade u ON u.jurisdicao_territorial_id = j.id
                WHERE j.tribunal_codigo = 'TRT3'
                GROUP BY j.id
                """, Integer.class);

        Map<Integer, Long> distribuicao = quantidadePorMunicipio.stream()
                .collect(Collectors.groupingBy(quantidade -> quantidade, Collectors.counting()));

        assertThat(distribuicao).isEqualTo(
                Map.of(1, 539L, 2, 138L, 3, 103L, 4, 20L, 5, 30L, 6, 16L, 48, 1L));
    }
}
