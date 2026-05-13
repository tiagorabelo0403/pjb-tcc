package com.tcc.pjb.backend.core.comunicacao.institucional.coerencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalAccessProfileCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application.InstitutionalOrganizationBlueprintCatalogApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceAggregate;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceDiagnosticReport;
import com.tcc.pjb.backend.core.comunicacao.institucional.panel.application.InstitutionalPanelBlueprintApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.processual.application.InstitutionalProcessWorkspaceApplicationService;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import org.junit.jupiter.api.Test;

class InstitutionalProceduralCoherenceApplicationServiceTest {

    private final InstitutionalProcessWorkspaceApplicationService workspaceService = new InstitutionalProcessWorkspaceApplicationService(
            new InstitutionalAccessProfileCatalogApplicationService(new InstitutionalOrganizationBlueprintCatalogApplicationService()),
            new InstitutionalPanelBlueprintApplicationService(),
            mock(ProcessoRepository.class)
    );

    private final InstitutionalProceduralCoherenceApplicationService service = new InstitutionalProceduralCoherenceApplicationService(
            workspaceService,
            new InstitutionalAccessProfileCatalogApplicationService(new InstitutionalOrganizationBlueprintCatalogApplicationService())
    );

    @Test
    void devePriorizarTrilhaRecursalParaPromotoriaTitular() {
        InstitutionalProceduralCoherenceAggregate aggregate = service.detalhar(
                "PROMOTORIA__PROMOTORIA_TITULAR",
                null,
                "PROCEDIMENTO_PENAL_COMUM",
                "RECURSAL",
                "RECURSO_INTERPOSTO",
                "PENAL"
        );

        assertTrue(aggregate.context().recursal());
        assertFalse(aggregate.nextBestActs().isEmpty());
        assertTrue(aggregate.nextBestActs().stream().anyMatch(item -> item.actionCode().contains("RECORRER") || item.actionCode().contains("RECURSO") || item.actionCode().contains("EMBARG")));
        assertTrue(aggregate.actEvaluations().stream().anyMatch(item -> item.actionCode().equals("RECORRER_MP") && item.allowed()));
    }

    @Test
    void deveBloquearAssinaturaQuandoProcessoEstiverArquivado() {
        InstitutionalProceduralActEvaluation evaluation = service.avaliarAto(
                "PROMOTORIA__PROMOTORIA_TITULAR",
                "ASSINAR_MANIFESTACAO",
                null,
                "COMUM_ORDINARIO",
                "CONHECIMENTO",
                "ARQUIVADO",
                "CIVIL"
        );

        assertTrue(evaluation.blocking());
        assertEquals("BLOQUEADO_POR_COERENCIA_PROCESSUAL", evaluation.decision());
        assertTrue(evaluation.findings().stream().anyMatch(finding -> finding.code().equals("STATUS_ENCERRADO_BLOQUEIA_MODIFICACAO")));
    }

    @Test
    void deveGerarDiagnosticoAgregadoSemQuebrarFluxoInstitucional() {
        InstitutionalProceduralCoherenceDiagnosticReport report = service.diagnosticar(
                null,
                "EXECUCAO_FISCAL",
                "EXECUCAO",
                "CUMPRIMENTO_SENTENCA",
                "TRIBUTARIO"
        );

        assertTrue(report.totalFindings() >= 0);
        assertTrue(report.fundamentos().stream().anyMatch(item -> item.contains("coerência processual") || item.contains("próximo melhor ato")));
    }
}
