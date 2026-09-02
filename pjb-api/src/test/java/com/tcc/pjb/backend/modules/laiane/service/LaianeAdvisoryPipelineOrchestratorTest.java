package com.tcc.pjb.backend.modules.laiane.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailReport;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceEngine;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotReport;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotService;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoAssistRequest;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LaianeAdvisoryPipelineOrchestratorTest {

    private final LegalCoherenceEngine coherenceEngine = mock(LegalCoherenceEngine.class);
    private final ProtocolDryRunService dryRunService = mock(ProtocolDryRunService.class);
    private final ProcessIntegrityRadarService integrityRadarService = mock(ProcessIntegrityRadarService.class);
    private final ProcessMaterialDossierService dossierService = mock(ProcessMaterialDossierService.class);
    private final ProcessMaterialStrategyService strategyService = mock(ProcessMaterialStrategyService.class);
    private final SettlementAdvisoryService settlementService = mock(SettlementAdvisoryService.class);
    private final StrategicCopilotService copilotService = mock(StrategicCopilotService.class);
    private final InstitutionalMemoryService memoryService = mock(InstitutionalMemoryService.class);
    private final ContextualPrecedentAdvisoryService precedentService = mock(ContextualPrecedentAdvisoryService.class);
    private final ExplainableDecisionTrailService trailService = mock(ExplainableDecisionTrailService.class);
    private final InstitutionalGovernanceContextService governanceContextService = mock(InstitutionalGovernanceContextService.class);
    private final LaianeAdvisoryPipelineOrchestrator orchestrator = new LaianeAdvisoryPipelineOrchestrator(
            coherenceEngine, dryRunService, integrityRadarService, dossierService, strategyService,
            settlementService, copilotService, memoryService, precedentService, trailService, governanceContextService);

    @Test
    void coherenceDelegaComOs9Argumentos() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var validator = mock(LaianePeticaoValidateResponse.class);
        var attachmentValidation = mock(LaianeLawyerAttachmentValidationResponse.class);
        var competencia = mock(DynamicCompetenceDistributionResponse.class);
        var teto = mock(TetoProcessualService.DiagnosticoTetoProcessual.class);
        var radar = mock(RadarPadroesService.AnaliseRadarResultado.class);
        var conflito = mock(ProntuarioNacionalService.AnaliseConflitoProcessual.class);
        var response = mock(LegalCoherenceReport.class);
        when(coherenceEngine.analyzeRequest(req, canonical, "COMUM", validator, attachmentValidation, competencia, teto, radar, conflito)).thenReturn(response);

        assertThat(orchestrator.coherence(req, canonical, "COMUM", validator, attachmentValidation, competencia, teto, radar, conflito)).isSameAs(response);
    }

    @Test
    void dryRunDelegaComReadinessScore() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var validator = mock(LaianePeticaoValidateResponse.class);
        var coherenceReport = mock(LegalCoherenceReport.class);
        var response = mock(ProtocolDryRunReport.class);
        when(dryRunService.simulateRequest(req, canonical, "COMUM", validator, null, null, null, coherenceReport, 0.85)).thenReturn(response);

        assertThat(orchestrator.dryRun(req, canonical, "COMUM", validator, null, null, null, coherenceReport, 0.85)).isSameAs(response);
    }

    @Test
    void settlementDelegaComValorCausaEMergedSignals() {
        Processo processo = Processo.builder().id(1L).build();
        var integrityRadar = mock(ProcessIntegrityRadarReport.class);
        var response = mock(SettlementAdvisoryReport.class);
        when(settlementService.analyze(processo, "COMUM", new BigDecimal("1000"), List.of("sig1"), integrityRadar)).thenReturn(response);

        assertThat(orchestrator.settlement(processo, "COMUM", new BigDecimal("1000"), List.of("sig1"), integrityRadar)).isSameAs(response);
    }

    @Test
    void institutionalMemoryEncadeaCorretamenteAposCopilot() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var competencia = mock(DynamicCompetenceDistributionResponse.class);
        var coherenceReport = mock(LegalCoherenceReport.class);
        var integrityRadar = mock(ProcessIntegrityRadarReport.class);
        var copilot = mock(StrategicCopilotReport.class);
        var response = mock(InstitutionalMemoryReport.class);
        when(memoryService.analyzeRequest(req, canonical, "COMUM", competencia, coherenceReport, integrityRadar, copilot)).thenReturn(response);

        assertThat(orchestrator.institutionalMemory(req, canonical, "COMUM", competencia, coherenceReport, integrityRadar, copilot)).isSameAs(response);
    }

    @Test
    void explainableDecisionTrailComposeComOs10Argumentos() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var competencia = mock(DynamicCompetenceDistributionResponse.class);
        var coherenceReport = mock(LegalCoherenceReport.class);
        var dryRun = mock(ProtocolDryRunReport.class);
        var integrityRadar = mock(ProcessIntegrityRadarReport.class);
        var copilot = mock(StrategicCopilotReport.class);
        var memory = mock(InstitutionalMemoryReport.class);
        var precedent = mock(ContextualPrecedentAdvisoryReport.class);
        var response = mock(ExplainableDecisionTrailReport.class);
        when(trailService.composeRequest(req, canonical, "COMUM", competencia, coherenceReport, dryRun, integrityRadar, copilot, memory, precedent)).thenReturn(response);

        assertThat(orchestrator.explainableDecisionTrail(req, canonical, "COMUM", competencia, coherenceReport, dryRun, integrityRadar, copilot, memory, precedent)).isSameAs(response);
    }

    @Test
    void governanceContextDelegaComOs6Argumentos() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var coherenceReport = mock(LegalCoherenceReport.class);
        var memory = mock(InstitutionalMemoryReport.class);
        var precedent = mock(ContextualPrecedentAdvisoryReport.class);
        var response = mock(InstitutionalGovernanceContextReport.class);
        when(governanceContextService.analyzeRequest(req, canonical, "COMUM", coherenceReport, memory, precedent)).thenReturn(response);

        assertThat(orchestrator.governanceContext(req, canonical, "COMUM", coherenceReport, memory, precedent)).isSameAs(response);
    }

    @Test
    void materialDossierEStrategyDelegam() {
        var req = mock(LaianePeticaoAssistRequest.class);
        var canonical = mock(CanonicalContext.class);
        var dossier = mock(ProcessMaterialDossierReport.class);
        var strategy = mock(ProcessMaterialStrategyReport.class);
        when(dossierService.analyzeRequest(req, canonical, "COMUM")).thenReturn(dossier);
        when(strategyService.analyzeRequest(req, canonical, "COMUM", dossier, 0.75, List.of("neg"))).thenReturn(strategy);

        assertThat(orchestrator.materialDossier(req, canonical, "COMUM")).isSameAs(dossier);
        assertThat(orchestrator.materialStrategy(req, canonical, "COMUM", dossier, 0.75, List.of("neg"))).isSameAs(strategy);
    }
}
