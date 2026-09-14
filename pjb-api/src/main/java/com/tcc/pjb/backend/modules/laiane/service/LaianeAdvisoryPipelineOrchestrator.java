package com.tcc.pjb.backend.modules.laiane.service;

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
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de LaianePeticaoAssistService: pipeline de análises legais/advisory
 * encadeadas -- 11 colaboradores calculando coerência, ensaio de protocolo, radar de
 * integridade, dossiê e estratégia material, aconselhamento de acordo, copiloto
 * estratégico, memória institucional, precedentes contextuais, trilha de decisão
 * explicável e contexto de governança. A ordem entre eles é sequencial: cada relatório
 * subsequente usa os anteriores. Preserva byte-a-byte o encadeamento original do preflight().
 */
@Service
public class LaianeAdvisoryPipelineOrchestrator {

    private final LegalCoherenceEngine legalCoherenceEngine;
    private final ProtocolDryRunService protocolDryRunService;
    private final ProcessIntegrityRadarService processIntegrityRadarService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final StrategicCopilotService strategicCopilotService;
    private final InstitutionalMemoryService institutionalMemoryService;
    private final ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService;
    private final ExplainableDecisionTrailService explainableDecisionTrailService;
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService;

    public LaianeAdvisoryPipelineOrchestrator(LegalCoherenceEngine legalCoherenceEngine,
                                               ProtocolDryRunService protocolDryRunService,
                                               ProcessIntegrityRadarService processIntegrityRadarService,
                                               ProcessMaterialDossierService processMaterialDossierService,
                                               ProcessMaterialStrategyService processMaterialStrategyService,
                                               SettlementAdvisoryService settlementAdvisoryService,
                                               StrategicCopilotService strategicCopilotService,
                                               InstitutionalMemoryService institutionalMemoryService,
                                               ContextualPrecedentAdvisoryService contextualPrecedentAdvisoryService,
                                               ExplainableDecisionTrailService explainableDecisionTrailService,
                                               InstitutionalGovernanceContextService institutionalGovernanceContextService) {
        this.legalCoherenceEngine = Objects.requireNonNull(legalCoherenceEngine);
        this.protocolDryRunService = Objects.requireNonNull(protocolDryRunService);
        this.processIntegrityRadarService = Objects.requireNonNull(processIntegrityRadarService);
        this.processMaterialDossierService = Objects.requireNonNull(processMaterialDossierService);
        this.processMaterialStrategyService = Objects.requireNonNull(processMaterialStrategyService);
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService);
        this.strategicCopilotService = Objects.requireNonNull(strategicCopilotService);
        this.institutionalMemoryService = Objects.requireNonNull(institutionalMemoryService);
        this.contextualPrecedentAdvisoryService = Objects.requireNonNull(contextualPrecedentAdvisoryService);
        this.explainableDecisionTrailService = Objects.requireNonNull(explainableDecisionTrailService);
        this.institutionalGovernanceContextService = Objects.requireNonNull(institutionalGovernanceContextService);
    }

    public LegalCoherenceReport coherence(LaianePeticaoAssistRequest req,
                                            CanonicalContext canonical,
                                            String ritoResolvido,
                                            LaianePeticaoValidateResponse validator,
                                            LaianeLawyerAttachmentValidationResponse attachmentValidation,
                                            DynamicCompetenceDistributionResponse competencia,
                                            TetoProcessualService.DiagnosticoTetoProcessual teto,
                                            RadarPadroesService.AnaliseRadarResultado radar,
                                            ProntuarioNacionalService.AnaliseConflitoProcessual conflito) {
        return legalCoherenceEngine.analyzeRequest(req, canonical, ritoResolvido, validator, attachmentValidation, competencia, teto, radar, conflito);
    }

    public ProtocolDryRunReport dryRun(LaianePeticaoAssistRequest req,
                                        CanonicalContext canonical,
                                        String ritoResolvido,
                                        LaianePeticaoValidateResponse validator,
                                        LaianeLawyerAttachmentValidationResponse attachmentValidation,
                                        DynamicCompetenceDistributionResponse competencia,
                                        TetoProcessualService.DiagnosticoTetoProcessual teto,
                                        LegalCoherenceReport coherenceReport,
                                        double readinessScore) {
        return protocolDryRunService.simulateRequest(req, canonical, ritoResolvido, validator, attachmentValidation, competencia, teto, coherenceReport, readinessScore);
    }

    public ProcessIntegrityRadarReport integrityRadar(LaianePeticaoAssistRequest req,
                                                       CanonicalContext canonical,
                                                       String ritoResolvido,
                                                       LegalCoherenceReport coherenceReport,
                                                       ProtocolDryRunReport dryRun,
                                                       DynamicCompetenceDistributionResponse competencia,
                                                       TetoProcessualService.DiagnosticoTetoProcessual teto) {
        return processIntegrityRadarService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, dryRun, competencia, teto);
    }

    public ProcessMaterialDossierReport materialDossier(LaianePeticaoAssistRequest req,
                                                         CanonicalContext canonical,
                                                         String ritoResolvido) {
        return processMaterialDossierService.analyzeRequest(req, canonical, ritoResolvido);
    }

    public ProcessMaterialStrategyReport materialStrategy(LaianePeticaoAssistRequest req,
                                                           CanonicalContext canonical,
                                                           String ritoResolvido,
                                                           ProcessMaterialDossierReport materialDossier,
                                                           double readinessScore,
                                                           List<String> negotiationSignals) {
        return processMaterialStrategyService.analyzeRequest(req, canonical, ritoResolvido, materialDossier, readinessScore, negotiationSignals);
    }

    public SettlementAdvisoryReport settlement(Processo syntheticProcess,
                                                 String ritoResolvido,
                                                 BigDecimal valorCausa,
                                                 List<String> mergedSignals,
                                                 ProcessIntegrityRadarReport integrityRadar) {
        return settlementAdvisoryService.analyze(syntheticProcess, ritoResolvido, valorCausa, mergedSignals, integrityRadar);
    }

    public StrategicCopilotReport copilot(LaianePeticaoAssistRequest req,
                                            CanonicalContext canonical,
                                            String ritoResolvido,
                                            LegalCoherenceReport coherenceReport,
                                            ProtocolDryRunReport dryRun,
                                            ProcessIntegrityRadarReport integrityRadar,
                                            DynamicCompetenceDistributionResponse competencia) {
        return strategicCopilotService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, dryRun, integrityRadar, competencia);
    }

    public InstitutionalMemoryReport institutionalMemory(LaianePeticaoAssistRequest req,
                                                          CanonicalContext canonical,
                                                          String ritoResolvido,
                                                          DynamicCompetenceDistributionResponse competencia,
                                                          LegalCoherenceReport coherenceReport,
                                                          ProcessIntegrityRadarReport integrityRadar,
                                                          StrategicCopilotReport copilot) {
        return institutionalMemoryService.analyzeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, integrityRadar, copilot);
    }

    public ContextualPrecedentAdvisoryReport precedentAdvisory(LaianePeticaoAssistRequest req,
                                                                 CanonicalContext canonical,
                                                                 String ritoResolvido,
                                                                 DynamicCompetenceDistributionResponse competencia,
                                                                 LegalCoherenceReport coherenceReport,
                                                                 StrategicCopilotReport copilot) {
        return contextualPrecedentAdvisoryService.analyzeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, copilot);
    }

    public ExplainableDecisionTrailReport explainableDecisionTrail(LaianePeticaoAssistRequest req,
                                                                     CanonicalContext canonical,
                                                                     String ritoResolvido,
                                                                     DynamicCompetenceDistributionResponse competencia,
                                                                     LegalCoherenceReport coherenceReport,
                                                                     ProtocolDryRunReport dryRun,
                                                                     ProcessIntegrityRadarReport integrityRadar,
                                                                     StrategicCopilotReport copilot,
                                                                     InstitutionalMemoryReport memory,
                                                                     ContextualPrecedentAdvisoryReport precedentAdvisory) {
        return explainableDecisionTrailService.composeRequest(req, canonical, ritoResolvido, competencia, coherenceReport, dryRun, integrityRadar, copilot, memory, precedentAdvisory);
    }

    public InstitutionalGovernanceContextReport governanceContext(LaianePeticaoAssistRequest req,
                                                                    CanonicalContext canonical,
                                                                    String ritoResolvido,
                                                                    LegalCoherenceReport coherenceReport,
                                                                    InstitutionalMemoryReport memory,
                                                                    ContextualPrecedentAdvisoryReport precedentAdvisory) {
        return institutionalGovernanceContextService.analyzeRequest(req, canonical, ritoResolvido, coherenceReport, memory, precedentAdvisory);
    }
}
