package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalHearingSchedulingGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalOperationalDeskGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelProvisioningReadinessApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalPanelProvisioningReadiness;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalPanelProvisioningReadinessApplicationServiceTest {

    @Test
    void mustResolveCompletePanelProvisioningForInstitutionalTitularProfile() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalPanelBlueprintApplicationService panelBlueprints = new InstitutionalPanelBlueprintApplicationService();
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                panelBlueprints,
                mock(ProcessoRepository.class)
        );
        InstitutionalPanelProvisioningReadinessApplicationService service = new InstitutionalPanelProvisioningReadinessApplicationService(
                accessCatalog,
                panelBlueprints,
                workspaceService,
                new InstitutionalHearingSchedulingGovernanceApplicationService(),
                new InstitutionalOperationalDeskGovernanceApplicationService()
        );

        InstitutionalOperationalProfileProjection profile = profile(
                "AFF-1|NOM-1",
                "PROMOTORIA",
                "TITULAR_INSTITUCIONAL",
                "PROMOTOR",
                "PAINEL_TITULAR",
                InstitutionalApiRoutes.painelExecutivo("PROMOTORIA")
        );

        InstitutionalPanelProvisioningReadiness readiness = service.avaliar(profile);

        assertNotNull(readiness);
        assertTrue(readiness.blueprintMatched());
        assertTrue(readiness.workspaceBound());
        assertTrue(readiness.complete());
        assertTrue(readiness.totalPrimarySections() > 0);
        assertTrue(readiness.totalQuickActions() > 0);
        assertTrue(readiness.totalTabs() > 0);
        assertTrue(readiness.totalWorkspaceActions() > 0);
        assertTrue(readiness.totalAuthorityBands() > 0);
        assertTrue(readiness.totalSeparators() > 0);
        assertTrue(readiness.notificationsReady());
        assertTrue(readiness.calendarReady());
        assertTrue(readiness.hearingsReady());
        assertTrue(readiness.readingModeReady());
        assertTrue(readiness.triageReady());
        assertTrue(readiness.presentationReady());
        assertTrue(readiness.colorSystemReady());
        assertTrue(readiness.opinionFlowReady());
        assertTrue(readiness.calculatorReady());
        assertTrue(readiness.sharedExperienceReady());
        assertNotNull(readiness.hearingGovernance());
        assertNotNull(readiness.deskGovernance());
        assertTrue(readiness.hearingGovernance().sectionVisible());
        assertTrue(readiness.hearingGovernance().canRequestHearing());
        assertTrue(readiness.deskGovernance().sectionVisible());
        assertTrue(readiness.deskGovernance().segregatedByUnit());
        assertTrue(readiness.readySharedExperienceSurfaces().contains("CALENDARIO_UNIFICADO"));
        assertTrue(readiness.findings().isEmpty());
    }


    @Test
    void mustUseInjectedInstantSourceForProvisioningSnapshot() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalPanelBlueprintApplicationService panelBlueprints = new InstitutionalPanelBlueprintApplicationService();
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                panelBlueprints,
                mock(ProcessoRepository.class)
        );
        Instant fixedInstant = Instant.parse("2026-04-03T15:30:00Z");
        InstitutionalPanelProvisioningReadinessApplicationService service = new InstitutionalPanelProvisioningReadinessApplicationService(
                accessCatalog,
                panelBlueprints,
                workspaceService,
                new InstitutionalHearingSchedulingGovernanceApplicationService(),
                new InstitutionalOperationalDeskGovernanceApplicationService()
        );

        InstitutionalPanelProvisioningReadiness readiness = service.avaliar(profile(
                "AFF-1|NOM-1",
                "PROMOTORIA",
                "TITULAR_INSTITUCIONAL",
                "PROMOTOR",
                "PAINEL_TITULAR",
                InstitutionalApiRoutes.painelExecutivo("PROMOTORIA")
        ));

        assertNotNull(readiness);
        assertNotNull(readiness.generatedAt());
    }

    @Test
    void mustFlagIncompletePanelWhenCanonicalBlueprintIsMissing() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalPanelBlueprintApplicationService panelBlueprints = new InstitutionalPanelBlueprintApplicationService();
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                panelBlueprints,
                mock(ProcessoRepository.class)
        );
        InstitutionalPanelProvisioningReadinessApplicationService service = new InstitutionalPanelProvisioningReadinessApplicationService(
                accessCatalog,
                panelBlueprints,
                workspaceService,
                new InstitutionalHearingSchedulingGovernanceApplicationService(),
                new InstitutionalOperationalDeskGovernanceApplicationService()
        );

        InstitutionalOperationalProfileProjection profile = profile(
                "AFF-X|NOM-X",
                "PROMOTORIA",
                "ASSESSORIA_INSTITUCIONAL",
                "PROMOTOR",
                "PAINEL_INEXISTENTE",
                null
        );

        InstitutionalPanelProvisioningReadiness readiness = service.avaliar(profile);

        assertNotNull(readiness);
        assertFalse(readiness.blueprintMatched());
        assertFalse(readiness.complete());
        assertFalse(readiness.sharedExperienceReady());
        assertTrue(readiness.findings().contains("painel_sem_blueprint_canonico"));
        assertTrue(readiness.findings().contains("painel_sem_workspace_processual"));
        assertTrue(readiness.findings().contains("painel_sem_superficie_calendario"));
        assertTrue(readiness.findings().contains("painel_sem_superficie_calculadora_judicial"));
    }

    private InstitutionalOperationalProfileProjection profile(String profileKey,
                                                             String organizationScope,
                                                             String nominationRole,
                                                             String processProfile,
                                                             String panelCode,
                                                             String landingPath) {
        return new InstitutionalOperationalProfileProjection(
                profileKey,
                "ATIVO_NO_PJB",
                true,
                "AFF-1",
                "NOM-1",
                101L,
                "Usuário Institucional",
                "SERVIDOR",
                organizationScope,
                "PROMOTORIA",
                "MPCE",
                "Ministério Público",
                "UNI-1",
                "1ª Vara Cível de Fortaleza",
                "CX-1",
                "ASSESSORIA",
                nominationRole,
                "ANALISE",
                processProfile,
                panelCode,
                landingPath,
                "#2563eb",
                "AREA-1",
                "NIVEL_3_CERTIFICADO_QUALIFICADO",
                true,
                true,
                true,
                false,
                false,
                true,
                "LOCAL",
                "TJCE",
                "UNI-1",
                "1ª Vara Cível de Fortaleza",
                "Fortaleza",
                "CE|MPCE|UNI-1|CX-1|0",
                "WRITE-1",
                "READ-CE-1",
                List.of("RECEBER", "ASSINAR"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of("PJB", "DIRETOR_GERAL"),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }
}
