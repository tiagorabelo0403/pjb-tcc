package com.tcc.pjb.backend.core.comunicacao.institucional.processual;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.domain.InstitutionalProcessWorkspace;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;

class InstitutionalProcessWorkspaceApplicationServiceTest {

    private final InstitutionalProcessWorkspaceApplicationService service = new InstitutionalProcessWorkspaceApplicationService(
            new InstitutionalAccessProfileCatalogApplicationService(new InstitutionalOrganizationBlueprintCatalogApplicationService()),
            new InstitutionalPanelBlueprintApplicationService(),
            mock(ProcessoRepository.class)
    );

    @Test
    void deveMontarWorkspaceTitularDaPromotoriaComFaixasSeparadoresETrilhaRecursal() {
        InstitutionalProcessWorkspace workspace = service.detalharPerfil(
                "PROMOTORIA__PROMOTORIA_TITULAR",
                null,
                "PROCEDIMENTO_PENAL_COMUM",
                "RECURSAL",
                "RECURSO_INTERPOSTO",
                "PENAL"
        );

        assertTrue(workspace.tabs().contains("recursos"));
        assertTrue(workspace.tabs().contains("embargos"));
        assertTrue(workspace.authorityBands().stream().anyMatch(band -> band.code().equals("AUTORIDADE_MANIFESTACAO") && band.enabled()));
        assertTrue(workspace.separators().stream().anyMatch(separator -> separator.code().equals("SEP_RECURSAL") && separator.active()));
        assertTrue(workspace.visualLanes().stream().anyMatch(lane -> lane.code().equals("TRILHA_RECURSAL") && lane.active()));
    }

    @Test
    void deveGerarDiagnosticoSemFalhaDeManifestacaoDoTitular() {
        InstitutionalProcessDiagnosticReport report = service.diagnosticar(
                null,
                "EXECUCAO_FISCAL",
                "EXECUCAO",
                "CUMPRIMENTO_SENTENCA",
                "TRIBUTARIO"
        );

        assertFalse(report.findings().stream().anyMatch(finding -> finding.code().equals("WORKSPACE_SEM_MANIFESTACAO_TITULAR")));
        assertTrue(report.findings().stream().anyMatch(finding -> finding.code().equals("WORKSPACE_GOVERNANCA_SEM_ABA") || finding.code().equals("WORKSPACE_TRIAGEM_COM_SENSIVEL")) || report.compliant());
    }
}
