package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessActionSpec;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InstitutionalPanelProvisioningSupportTest {

    private final InstitutionalPanelProvisioningSupport support = new InstitutionalPanelProvisioningSupport();

    @Test
    void shouldRequireOpinionFlowWhenProfileSignalsPromotor() {
        boolean result = support.requiresOpinionFlow(profile("PROMOTOR"), catalogEntry(InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL), workspace("CONSULTAR", "VISUALIZAR", "LEITURA"));

        assertThat(result).isTrue();
    }

    @Test
    void shouldRequireCalculatorWhenWorkspaceExposesCalculationAction() {
        boolean result = support.requiresCalculator(profile("APOIO"), catalogEntry(InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL), workspace("CALCULO_LIQUIDACAO", "Calculadora Judicial", "Liquidacao automatizada"));

        assertThat(result).isTrue();
    }

    @Test
    void shouldDetectDomainSignalsAcrossSectionsActionsAndTabs() {
        boolean result = support.containsDomainSignals(
                Set.of("PARECERES"),
                Set.of("EMITIR_MINUTA"),
                Set.of("CALCULADORA"),
                "PARECER", "CALCULO"
        );

        assertThat(result).isTrue();
    }

    private InstitutionalOperationalProfileProjection profile(String processProfile) {
        return new InstitutionalOperationalProfileProjection(
                "profile-key",
                "ACTIVE",
                true,
                "aff-1",
                "nom-1",
                10L,
                "Ana",
                "SERVIDOR",
                "MP",
                "MINISTERIO_PUBLICO",
                "MPCE",
                "Ministerio Publico",
                "UNI-1",
                "Unidade 1",
                "CAIXA-1",
                "CAIXA",
                "TITULAR_INSTITUCIONAL",
                "Promotoria",
                processProfile,
                "PAINEL_ORGAO",
                "/painel",
                "VERDE",
                "AREA",
                "NIVEL_2_NOMEACAO_ATIVA",
                true,
                true,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNI-1",
                "Unidade 1",
                "Fortaleza",
                "HDP",
                "WRITE",
                "READ",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }

    private InstitutionalAccessProfileCatalogEntry catalogEntry(InstitutionalProcessProfile processProfile) {
        return new InstitutionalAccessProfileCatalogEntry(
                "MP__PROMOTOR",
                "Promotoria",
                InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                InstitutionalNominationRole.TITULAR_INSTITUCIONAL,
                processProfile,
                InstitutionalEntryLandingPanel.PAINEL_ORGAO,
                InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                Set.of(),
                List.of(),
                List.of()
        );
    }

    private InstitutionalProcessWorkspace workspace(String code, String title, String description) {
        return new InstitutionalProcessWorkspace(
                "profile-key",
                "Painel",
                "PAINEL_ORGAO",
                "PROMOTOR",
                "NIVEL_2_NOMEACAO_ATIVA",
                "VERDE",
                "COMUM",
                "CONHECIMENTO",
                "ATIVO",
                "PUBLICO",
                List.of("PARECERES"),
                List.of(),
                List.of(),
                List.of(),
                List.of(new InstitutionalProcessActionSpec(code, title, description, "VERDE", false, false, false, List.of(), List.of(), List.of())),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
