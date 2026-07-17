package com.tcc.pjb.backend.service.territorial;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.dto.processual.AncoraTerritorial;
import com.tcc.pjb.backend.model.dto.processual.EnderecosProcessuaisRequest;
import com.tcc.pjb.backend.model.entity.competencia.JurisdicaoTerritorial;
import com.tcc.pjb.backend.model.entity.competencia.ModoCompetencia;
import com.tcc.pjb.backend.model.entity.enums.processual.CriterioTerritorial;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.JurisdicaoTerritorialRepository;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class CompetenciaTerritorialResolverIT extends PjbIntegrationTestBase {

    private static final String IBGE_TESTE_UNIDADE_UNICA = "0000001";
    private static final String IBGE_TESTE_UNIDADES_CONCORRENTES = "0000002";
    private static final String IBGE_TESTE_FORA_DO_CATALOGO = "0000003";

    @Autowired
    private CompetenciaTerritorialResolver resolver;

    @Autowired
    private JurisdicaoTerritorialRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limparCatalogo() {
        jdbcTemplate.update("DELETE FROM tb_jurisdicao_territorial WHERE municipio_ibge IN (?, ?, ?)",
                IBGE_TESTE_UNIDADE_UNICA, IBGE_TESTE_UNIDADES_CONCORRENTES, IBGE_TESTE_FORA_DO_CATALOGO);
    }

    @Test
    void ritoTrabalhistaComMunicipioNoCatalogoResolveParaUnidadeCorreta() {
        repository.save(new JurisdicaoTerritorial(IBGE_TESTE_UNIDADE_UNICA, "Municipio Teste Unidade Unica", "CE",
                "TRABALHO", "ORIGINARIA", Set.of("TRT7-0023"), "TESTE", "TRT7, jurisdicao de unidades",
                LocalDate.of(2010, 1, 1), null));

        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial(IBGE_TESTE_UNIDADE_UNICA, "Municipio Teste Unidade Unica", "CE"),
                null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.criterio()).isEqualTo(CriterioTerritorial.LOCAL_PRESTACAO_SERVICO);
            assertThat(resolvida.unidadesElegiveis()).containsExactly("TRT7-0023");
            assertThat(resolvida.modo()).isEqualTo(ModoCompetencia.ORIGINARIA);
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TESTE");
        });
    }

    @Test
    void municipioComMultiplasUnidadesConcorrentesResolveTodasSemViolarExclusao() {
        Set<String> unidadesConcorrentes = Set.of(
                "TESTE-CONC-0001", "TESTE-CONC-0002", "TESTE-CONC-0003", "TESTE-CONC-0004",
                "TESTE-CONC-0005", "TESTE-CONC-0006", "TESTE-CONC-0007", "TESTE-CONC-0008",
                "TESTE-CONC-0009", "TESTE-CONC-0010", "TESTE-CONC-0011", "TESTE-CONC-0012",
                "TESTE-CONC-0013", "TESTE-CONC-0014", "TESTE-CONC-0015", "TESTE-CONC-0016",
                "TESTE-CONC-0017", "TESTE-CONC-0018");
        repository.save(new JurisdicaoTerritorial(IBGE_TESTE_UNIDADES_CONCORRENTES, "Municipio Teste Concorrente",
                "CE", "TRABALHO", "ORIGINARIA", unidadesConcorrentes, "TESTE",
                "Fixture sintetica, prova de suporte a unidades concorrentes no schema", LocalDate.of(2010, 1, 1), null));

        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial(IBGE_TESTE_UNIDADES_CONCORRENTES, "Municipio Teste Concorrente", "CE"),
                null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis())
                        .containsExactlyInAnyOrderElementsOf(unidadesConcorrentes));
    }

    @Test
    void ritoUsucapiaoSemLocalDoFatoInformadoRetornaAncoraAusente() {
        EnderecosProcessuaisRequest enderecos = EnderecosProcessuaisRequest.vazio();

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.CIVIL_USUCAPIAO,
                TipoJustica.ESTADUAL, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.AncoraAusente.class,
                ausente -> assertThat(ausente.criterioExigido()).isEqualTo(CriterioTerritorial.SITUACAO_DA_COISA));
    }

    @Test
    void ritoTrabalhistaComMunicipioValidoForaDoCatalogoRetornaMunicipioForaDoCatalogo() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial(IBGE_TESTE_FORA_DO_CATALOGO, "Municipio Teste Fora Do Catalogo", "CE"),
                null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.MunicipioForaDoCatalogo.class, foraDoCatalogo -> {
            assertThat(foraDoCatalogo.municipioIbge()).isEqualTo(IBGE_TESTE_FORA_DO_CATALOGO);
            assertThat(foraDoCatalogo.tipoJustica()).isEqualTo(TipoJustica.TRABALHO);
        });
    }

    @Test
    void ritoPrevidenciarioSemCriterioMapeadoRetornaCriterioNaoMapeado() {
        EnderecosProcessuaisRequest enderecos = EnderecosProcessuaisRequest.vazio();

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.PREVIDENCIARIO_COMUM,
                TipoJustica.FEDERAL, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.CriterioNaoMapeado.class,
                naoMapeado -> assertThat(naoMapeado.rito()).isEqualTo(RitoProcessual.PREVIDENCIARIO_COMUM));
    }

    @Test
    void domicilioReuDesconhecidoRetornaAncoraNulaMesmoComDomicilioReuInformado() {
        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, new AncoraTerritorial("2304400", "Fortaleza", "CE"), null, null, true);

        AncoraTerritorial ancora = enderecos.ancoraPara(CriterioTerritorial.DOMICILIO_REU);

        assertThat(ancora).isNull();
    }

    @Test
    void switchExaustivoSobreAsQuatroVariantesCompilaSemDefault() {
        ResolucaoTerritorial resolucao = new ResolucaoTerritorial.CriterioNaoMapeado(RitoProcessual.PREVIDENCIARIO_COMUM);

        String descricao = switch (resolucao) {
            case ResolucaoTerritorial.Resolvida resolvida -> "resolvida:" + resolvida.unidadesElegiveis();
            case ResolucaoTerritorial.CriterioNaoMapeado naoMapeado -> "naoMapeado:" + naoMapeado.rito();
            case ResolucaoTerritorial.AncoraAusente ausente -> "ausente:" + ausente.criterioExigido();
            case ResolucaoTerritorial.MunicipioForaDoCatalogo foraDoCatalogo -> "foraDoCatalogo:" + foraDoCatalogo.municipioIbge();
        };

        assertThat(descricao).isEqualTo("naoMapeado:PREVIDENCIARIO_COMUM");
    }
}
