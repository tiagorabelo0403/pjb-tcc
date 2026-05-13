package com.tcc.pjb.backend.core.comunicacao.institucional.panel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalHearingSchedulingGovernanceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingRiteGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.domain.InstitutionalHearingSchedulingGovernance;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InstitutionalHearingSchedulingGovernanceApplicationServiceTest {

    @Test
    void mustAllowDocumentOrganizationWithoutAutonomousSchedulingForDocumentSupportProfile() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry entry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("PROMOTORIA__PROMOTORIA_DOCUMENTOS"))
                .findFirst()
                .orElseThrow();
        InstitutionalProcessWorkspace workspace = workspaceService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        InstitutionalHearingSchedulingGovernance governance = new InstitutionalHearingSchedulingGovernanceApplicationService()
                .avaliar(profile(entry, "ANALISTA", "PAINEL_CAIXA"), entry, workspace);

        assertNotNull(governance);
        assertTrue(governance.sectionVisible());
        assertTrue(governance.canRequestHearing());
        assertTrue(governance.canOrganizeDocket());
        assertFalse(governance.canOperationallySchedule());
        assertTrue(governance.canPrepareHearingBundle());
        assertTrue(governance.requiresUnitIsolation());
        assertTrue(governance.forbiddenActs().contains("agendamento_autonomo_por_apoio_documental"));
        assertTrue(governance.allowedRiteGroups().contains("PENAL"));
        InstitutionalHearingRiteGovernance penal = governance.riteGovernances().stream()
                .filter(item -> item.riteCode().equals("PENAL"))
                .findFirst()
                .orElseThrow();
        assertFalse(penal.canOperationallySchedule());
        assertTrue(penal.canRequestHearing());
        assertTrue(penal.canPrepareHearingBundle());
        assertTrue(penal.requiresUnitIsolation());
        assertTrue(penal.requestActors().contains("APOIO_DOCUMENTAL_INSTITUCIONAL"));
        assertTrue(penal.preparatoryActors().contains("APOIO_DOCUMENTAL_INSTITUCIONAL"));
    }

    @Test
    void mustRequireJudicialAuthorizationForOperationalHearingSchedulingBySecretariat() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry entry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("CENTRAL_AUDIENCIAS__CENTRAL_AUDIENCIA_SECRETARIA"))
                .findFirst()
                .orElseThrow();
        InstitutionalProcessWorkspace workspace = workspaceService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        InstitutionalHearingSchedulingGovernance governance = new InstitutionalHearingSchedulingGovernanceApplicationService()
                .avaliar(profile(entry, "GESTOR_CAIXA", "PAINEL_AUDIENCIAS_CONCILIACAO"), entry, workspace);

        assertNotNull(governance);
        assertTrue(governance.sectionVisible());
        assertTrue(governance.canOperationallySchedule());
        assertTrue(governance.canReserveRoom());
        assertTrue(governance.canIssueHearingCommunications());
        assertTrue(governance.canPrepareHearingBundle());
        assertTrue(governance.requiresUnitIsolation());
        assertTrue(governance.requiresJudicialAuthorization());
        assertFalse(governance.requiresSecretariatCoordination());
        assertTrue(governance.allowedRiteGroups().contains("CONCILIACAO_MEDIACAO"));
        InstitutionalHearingRiteGovernance conciliacao = governance.riteGovernances().stream()
                .filter(item -> item.riteCode().equals("CONCILIACAO_MEDIACAO"))
                .findFirst()
                .orElseThrow();
        assertTrue(conciliacao.canOperationallySchedule());
        assertTrue(conciliacao.canIssueHearingCommunications());
        assertTrue(conciliacao.canPrepareHearingBundle());
        assertTrue(conciliacao.requiresUnitIsolation());
        assertTrue(conciliacao.communicationActors().contains("SECRETARIA_RESPONSAVEL_PELA_INTIMACAO"));
        assertTrue(conciliacao.operationalActors().contains("CENTRAL_AUDIENCIA_OU_CEJUSC"));
        assertTrue(conciliacao.allowedActs().contains("agendar_audiencia"));
    }



    @Test
    void mustExposeScopeQueuesAndSegregationForOperationalAudiences() {
        InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalog = new InstitutionalOrganizationBlueprintCatalogApplicationService();
        InstitutionalAccessProfileCatalogApplicationService accessCatalog = new InstitutionalAccessProfileCatalogApplicationService(blueprintCatalog);
        InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
                accessCatalog,
                new com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService(),
                mock(ProcessoRepository.class)
        );
        InstitutionalAccessProfileCatalogEntry entry = accessCatalog.listarPerfis().stream()
                .filter(item -> item.codigo().equals("CENTRAL_AUDIENCIAS__CENTRAL_AUDIENCIA_SECRETARIA"))
                .findFirst()
                .orElseThrow();
        InstitutionalProcessWorkspace workspace = workspaceService.detalharPerfil(entry.codigo(), null, null, null, null, null);
        InstitutionalHearingSchedulingGovernance governance = new InstitutionalHearingSchedulingGovernanceApplicationService()
                .avaliar(profile(entry, "GESTOR_CAIXA", "PAINEL_AUDIENCIAS_CONCILIACAO"), entry, workspace);

        assertNotNull(governance.schedulingScopeKey());
        assertTrue(governance.schedulingScopeKey().contains("TJCE"));
        assertTrue(governance.operationalQueues().stream().anyMatch(item -> item.startsWith("INTIMACOES_AUDIENCIA:")));
        assertTrue(governance.operationalQueues().stream().anyMatch(item -> item.startsWith("FILTRO_COMPETENCIA:")));
        assertTrue(governance.segregationGuards().stream().anyMatch(item -> item.startsWith("unidade=")));
        assertTrue(governance.segregationGuards().contains("segregacao_central_de_audiencias"));
        assertTrue(governance.oversightActors().contains("MAGISTRADO_DO_FEITO"));
    }
    private InstitutionalOperationalProfileProjection profile(InstitutionalAccessProfileCatalogEntry entry,
                                                             String funcaoOperacional,
                                                             String panelCode) {
        return new InstitutionalOperationalProfileProjection(
                entry.codigo() + "|NOM-1",
                "ATIVO_NO_PJB",
                true,
                "AFF-1",
                "NOM-1",
                200L,
                entry.nomeExibicao(),
                "SERVIDOR",
                entry.codigo().split("__")[0],
                entry.codigo().split("__")[0],
                "ORG-1",
                "Órgão 1",
                "UNI-1",
                "Unidade 1",
                "CX-1",
                "SECRETARIA",
                entry.nominationRole().name(),
                funcaoOperacional,
                entry.processProfile().name(),
                panelCode,
                InstitutionalApiRoutes.painelExecutivo(),
                "#2563eb",
                "AREA-1",
                entry.trustFloor().name(),
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
                "CE|ORG-1|UNI-1|CX-1|0",
                "WRITE-1",
                "READ-1",
                entry.capacidadesPadrao().stream().map(Enum::name).toList(),
                List.of("PJB"),
                List.of("PJB"),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }
}
