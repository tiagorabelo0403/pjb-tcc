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

class CompetenciaTerritorialResolverIT extends PjbIntegrationTestBase {

    @Autowired
    private CompetenciaTerritorialResolver resolver;

    @Autowired
    private JurisdicaoTerritorialRepository repository;

    @BeforeEach
    void limparCatalogo() {
        repository.deleteAll();
    }

    @Test
    void ritoTrabalhistaComMunicipioNoCatalogoResolveParaUnidadeCorreta() {
        repository.save(new JurisdicaoTerritorial("2307304", "Morada Nova", "CE", "TRABALHO", "ORIGINARIA",
                Set.of("VT-LIMOEIRO-0023"), "TRT7", "TRT7, jurisdicao de unidades", LocalDate.of(2010, 1, 1), null));

        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2307304", "Morada Nova", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class, resolvida -> {
            assertThat(resolvida.criterio()).isEqualTo(CriterioTerritorial.LOCAL_PRESTACAO_SERVICO);
            assertThat(resolvida.unidadesElegiveis()).containsExactly("VT-LIMOEIRO-0023");
            assertThat(resolvida.modo()).isEqualTo(ModoCompetencia.ORIGINARIA);
            assertThat(resolvida.tribunalCodigo()).isEqualTo("TRT7");
        });
    }

    @Test
    void municipioComMultiplasUnidadesConcorrentesResolveTodasSemViolarExclusao() {
        Set<String> varasDoTrabalhoFortaleza = Set.of(
                "VT-FORTALEZA-0001", "VT-FORTALEZA-0002", "VT-FORTALEZA-0003", "VT-FORTALEZA-0004",
                "VT-FORTALEZA-0005", "VT-FORTALEZA-0006", "VT-FORTALEZA-0007", "VT-FORTALEZA-0008",
                "VT-FORTALEZA-0009", "VT-FORTALEZA-0010", "VT-FORTALEZA-0011", "VT-FORTALEZA-0012",
                "VT-FORTALEZA-0013", "VT-FORTALEZA-0014", "VT-FORTALEZA-0015", "VT-FORTALEZA-0016",
                "VT-FORTALEZA-0017", "VT-FORTALEZA-0018");
        repository.save(new JurisdicaoTerritorial("2304400", "Fortaleza", "CE", "TRABALHO", "ORIGINARIA",
                varasDoTrabalhoFortaleza, "TRT7", "TST, mapa de jurisdicao das Varas do Trabalho do Ceara",
                LocalDate.of(2010, 1, 1), null));

        EnderecosProcessuaisRequest enderecos = new EnderecosProcessuaisRequest(
                null, null, new AncoraTerritorial("2304400", "Fortaleza", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.Resolvida.class,
                resolvida -> assertThat(resolvida.unidadesElegiveis())
                        .containsExactlyInAnyOrderElementsOf(varasDoTrabalhoFortaleza));
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
                null, null, new AncoraTerritorial("2304400", "Fortaleza", "CE"), null, false);

        ResolucaoTerritorial resolucao = resolver.resolver(RitoProcessual.TRABALHISTA_ORDINARIO,
                TipoJustica.TRABALHO, enderecos, LocalDate.of(2026, 1, 1));

        assertThat(resolucao).isInstanceOfSatisfying(ResolucaoTerritorial.MunicipioForaDoCatalogo.class, foraDoCatalogo -> {
            assertThat(foraDoCatalogo.municipioIbge()).isEqualTo("2304400");
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
