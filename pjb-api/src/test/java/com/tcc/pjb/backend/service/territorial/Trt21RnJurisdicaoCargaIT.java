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

class Trt21RnJurisdicaoCargaIT extends PjbIntegrationTestBase {

    @Autowired
    private CompetenciaTerritorialResolver resolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void natalSedeResolveComAs13VarasDoForumTrabalhistaDeNatal() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2408102", "Natal", "RN"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.unidadesElegiveis()).containsExactlyInAnyOrder(
                    "TRT21-0001", "TRT21-0002", "TRT21-0003", "TRT21-0004", "TRT21-0005", "TRT21-0006",
                    "TRT21-0007", "TRT21-0008", "TRT21-0009", "TRT21-0010", "TRT21-0011", "TRT21-0012",
                    "TRT21-0013");
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT21");
        });
    }

    @Test
    void angicosTrabalhaAquiVaraFicaEmAssu() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2400802", "Angicos", "RN"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT21-0016"));
    }

    @Test
    void mossoroResolveComAsQuatroVarasConcorrentes() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2408003", "Mossoró", "RN"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis())
                        .containsExactlyInAnyOrder("TRT21-0011", "TRT21-0012", "TRT21-0013", "TRT21-0014"));
    }

    @Test
    void saoGoncaloDoAmaranteHomonimoEntreRnECearaResolveParaAUnidadeCorretaDoRn() {
        EnderecosProcessuaisRequest enderecosRn = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2412005", "São Gonçalo do Amarante", "RN"), null, false);

        ResolucaoTerritorial resolucaoRn = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecosRn, LocalDate.of(2026, 1, 1));

        assertThat(resolucaoRn).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT21");
            assertThat(resolvida.unidadesElegiveis()).containsExactlyInAnyOrder(
                    "TRT21-0001", "TRT21-0002", "TRT21-0003", "TRT21-0004", "TRT21-0005", "TRT21-0006",
                    "TRT21-0007", "TRT21-0008", "TRT21-0009", "TRT21-0010", "TRT21-0011", "TRT21-0012",
                    "TRT21-0013");
        });

        EnderecosProcessuaisRequest enderecosCe = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2312403", "São Gonçalo do Amarante", "CE"), null, false);

        ResolucaoTerritorial resolucaoCe = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecosCe, LocalDate.of(2026, 1, 1));

        assertThat(resolucaoCe).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT7"));
    }

    @Test
    void casoTrabalhistaAntigoAntesDoDocumentoFonteAindaAssimResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2400802", "Angicos", "RN"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2015, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT21-0016"));
    }

    @Test
    void angicosNoPrimeiroDiaDeVigenciaConstitucionalJaResolve() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2400802", "Angicos", "RN"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(1988, 10, 5));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT21-0016"));
    }

    @Test
    void cargaDoRnCobreOs129MunicipiosComOs411ParesDeUnidade() {
        Integer totalMunicipios = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_jurisdicao_territorial WHERE tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT21')", Integer.class);
        Integer totalPares = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial_unidade u
                JOIN tb_jurisdicao_territorial j ON j.id = u.jurisdicao_territorial_id
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT21')
                """, Integer.class);
        Integer municipiosSemUnidade = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM tb_jurisdicao_territorial j
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT21')
                  AND NOT EXISTS (
                      SELECT 1 FROM tb_jurisdicao_territorial_unidade u
                      WHERE u.jurisdicao_territorial_id = j.id
                  )
                """, Integer.class);

        assertThat(totalMunicipios).isEqualTo(129);
        assertThat(totalPares).isEqualTo(411);
        assertThat(municipiosSemUnidade).isZero();
    }

    @Test
    void distribuicaoDeUnidadesPorMunicipioSeMantemConformeODocumentoFonte() {
        List<Integer> quantidadePorMunicipio = jdbcTemplate.queryForList("""
                SELECT COUNT(u.unidade_codigo) FROM tb_jurisdicao_territorial j
                JOIN tb_jurisdicao_territorial_unidade u ON u.jurisdicao_territorial_id = j.id
                WHERE j.tribunal_id = (SELECT id FROM tb_tribunal WHERE sigla = 'TRT21')
                GROUP BY j.id
                """, Integer.class);

        Map<Integer, Long> distribuicao = quantidadePorMunicipio.stream()
                .collect(Collectors.groupingBy(quantidade -> quantidade, Collectors.counting()));

        assertThat(distribuicao).isEqualTo(Map.of(1, 98L, 4, 10L, 13, 21L));
    }
}
