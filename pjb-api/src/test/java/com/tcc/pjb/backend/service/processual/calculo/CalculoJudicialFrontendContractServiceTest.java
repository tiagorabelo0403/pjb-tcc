package com.tcc.pjb.backend.service.processual.calculo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.dto.processual.calculo.CalculoJudicialSolicitantePerfil;
import org.junit.jupiter.api.Test;

class CalculoJudicialFrontendContractServiceTest {

    private final CalculoJudicialFrontendContractService service = new CalculoJudicialFrontendContractService(new CalculoJudicialTabelaOficialService(), TestEconomicReferenceSupport.economicReferenceService());

    @Test
    void deveExporFingerprintCapabilitiesECacheCanonicos() {
        assertThat(service.version()).isEqualTo("v1");
        assertThat(service.fingerprint()).startsWith("pjb-calculo-front-");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsEntry("fingerprint", service.fingerprint());
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("routePolicy");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("officialTablesProfile");
        assertThat(service.apiContract("TRABALHISTA_CLT").get("routePolicy").toString()).contains("TRABALHISTA_VERBAS_RESCISORIAS_LEGACY");
        assertThat(service.frontendBindings("FAZENDA_TRIBUTARIO")).containsEntry("contractVersion", service.version());
        assertThat(service.apiCatalog().toString()).contains("CUSTAS_PROCESSUAIS");
        assertThat(service.uiCatalog(CalculoJudicialSolicitantePerfil.ADVOGADO).toString()).contains("Custas e Despesas");
        assertThat(service.frontendBindings("TRABALHISTA_CLT")).containsEntry("routePolicy", "metadata.routePolicy");
        assertThat(service.profileCapabilities(CalculoJudicialSolicitantePerfil.CIDADAO)).containsEntry("visibleSectionsMode", "essential_first");
        assertThat(service.cacheDescriptor("catalogo", "TRABALHISTA_CLT", CalculoJudicialSolicitantePerfil.ADVOGADO)).containsKeys("etag", "cacheControl", "fingerprint");
        assertThat(service.apiCatalog()).containsKey("tabelasOficiais");
        assertThat(service.apiCatalog()).containsKey("agentesIa");
        assertThat(service.apiCatalog()).containsKey("painelIaFinanceira");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("iaFinanceiraRoute");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("experiencePreferenceContextFields");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("economicReferencesRoute");
        assertThat(service.apiContract("TRABALHISTA_CLT")).containsKey("experiencePreferenceRoute");
        assertThat(service.profileCapabilities(CalculoJudicialSolicitantePerfil.CIDADAO)).containsEntry("canUseIaFinanceira", true);
        assertThat(service.profileCapabilities(CalculoJudicialSolicitantePerfil.CIDADAO)).containsEntry("canSeeFinancialAiPanel", true);
        assertThat(service.apiCatalog()).containsKey("experiencePreference");
        assertThat(service.apiCatalog()).containsKey("painelIaFinanceira");
        assertThat(service.profileCapabilities(CalculoJudicialSolicitantePerfil.ADVOGADO)).containsEntry("defaultExperienceMode", "manual_tradicional");
        assertThat(service.experienceModes("TRABALHISTA_CLT", CalculoJudicialSolicitantePerfil.ADVOGADO).toString()).contains("Versão manual");
        assertThat(service.experiencePreferenceContextFields()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(service.experiencePreferenceContextFields().toString()).contains("tribunal");
        assertThat(service.experiencePreferenceContextFields().toString()).contains("sistemaOrigem");
    }
}
