package com.tcc.pjb.backend.model.entity.enums.processual;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class CriterioTerritorialTest {

    @Test
    void ritoTrabalhistaResolveLocalPrestacaoServico() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.TRABALHISTA_ORDINARIO.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.LOCAL_PRESTACAO_SERVICO);
    }

    @Test
    void usucapiaoResolveSituacaoDaCoisaENaoDomicilioDoReu() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.CIVIL_USUCAPIAO.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.SITUACAO_DA_COISA);
    }

    @Test
    void inventarioResolveDomicilioAutorHeranca() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.CIVIL_INVENTARIO_ARROLAMENTO.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.DOMICILIO_AUTOR_HERANCA);
    }

    @Test
    void alimentosResolveDomicilioAlimentando() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.CIVIL_FAMILIA_ALIMENTOS.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.DOMICILIO_ALIMENTANDO);
    }

    @Test
    void ritoPenalResolveLocalDoFato() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.PROCEDIMENTO_PENAL_COMUM.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.LOCAL_DO_FATO);
    }

    @Test
    void ritoPrevidenciarioDeclaraIgnoranciaEmVezDeChutar() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.PREVIDENCIARIO_COMUM.criterioTerritorial();

        assertThat(criterio).isEmpty();
    }

    @Test
    void dissolucaoCasamentoDeclaraIgnoranciaPorCascataDoArt53NaoModelada() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.CIVIL_DISSOLUCAO_CASAMENTO.criterioTerritorial();

        assertThat(criterio).isEmpty();
    }

    @Test
    void todoValorDoEnumTemFundamentoLegalNaoBranco() {
        for (CriterioTerritorial valor : CriterioTerritorial.values()) {
            assertThat(valor.fundamentoLegal()).isNotBlank();
        }
    }

    @Test
    void interditoProibitorioResolveSituacaoDaCoisa() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.CIVIL_INTERDITO_PROIBITORIO.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.SITUACAO_DA_COISA);
    }

    @Test
    void agrarioPosseTerraResolveSituacaoDaCoisa() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.AGRARIO_POSSE_TERRA.criterioTerritorial();

        assertThat(criterio).contains(CriterioTerritorial.SITUACAO_DA_COISA);
    }

    @Test
    void desapropriacaoDeclaraIgnoranciaPorCascataCondicionalDoExpropriante() {
        Optional<CriterioTerritorial> criterio = RitoProcessual.AGRARIO_DESAPROPRIACAO.criterioTerritorial();

        assertThat(criterio).isEmpty();
    }
}
