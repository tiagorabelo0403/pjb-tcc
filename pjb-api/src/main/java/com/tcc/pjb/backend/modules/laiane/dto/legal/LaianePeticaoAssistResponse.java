package com.tcc.pjb.backend.modules.laiane.dto.legal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.tcc.pjb.backend.core.procedural.ProceduralRoutingReport;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintReport;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionReport;
import com.tcc.pjb.backend.core.kernel.advisory.ContextualPrecedentAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.ExplainableDecisionTrailReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.LegalCoherenceReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessIntegrityRadarReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProtocolDryRunReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.StrategicCopilotReport;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicySnapshotReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsReport;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationReport;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.competencia.DynamicCompetenceDistributionResponse;
import com.tcc.pjb.backend.modules.laiane.dto.roles.lawyer.LaianeLawyerAttachmentValidationResponse;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.teto.TetoProcessualService;
import com.tcc.pjb.backend.service.territorial.TerritorialProcessualService;
import com.tcc.pjb.backend.service.triagem.TriagemNacionalIAEngine;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaianePeticaoAssistResponse {

    private String requestId;
    private Instant generatedAt;
    private String draftMarkdown;

    @Builder.Default
    private List<String> draftWarnings = new ArrayList<>();

    private LaianePeticaoValidateResponse validator;
    private LaianeLawyerAttachmentValidationResponse attachmentValidation;
    private TriagemNacionalIAEngine.ResultadoTriagem triagem;
    private DynamicCompetenceDistributionResponse competencia;
    private TetoProcessualService.DiagnosticoTetoProcessual tetoProcessual;
    private TerritorialProcessualService.DiagnosticoTerritorialProcessual territorialProcessual;
    private RadarPadroesService.AnaliseRadarResultado radar;
    private ProntuarioNacionalService.AnaliseConflitoProcessual conflitoProcessual;
    private LaianeRitosCoverageResponse ritosCoverage;
    private boolean prontaParaProtocolo;
    private double readinessScore;

    @Builder.Default
    private List<String> orientacoes = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> protocolPayloadPreview = new LinkedHashMap<>();

    private LegalCoherenceReport coherenceReport;
    private ProtocolDryRunReport protocolDryRun;
    private ProcessIntegrityRadarReport integrityRadar;
    private ProcessMaterialDossierReport materialDossier;
    private ProcessMaterialStrategyReport materialStrategy;
    private ProceduralRoutingReport proceduralRouting;
    private ProceduralSubmissionBlueprintReport submissionBlueprint;
    private ProceduralConnectorExecutionReport connectorExecution;
    private StrategicCopilotReport strategicCopilot;
    private SettlementAdvisoryReport settlementAdvisory;
    private InstitutionalMemoryReport institutionalMemory;
    private ContextualPrecedentAdvisoryReport precedentAdvisory;
    private ExplainableDecisionTrailReport explainableDecisionTrail;
    private InstitutionalGovernanceContextReport institutionalGovernanceContext;
    private KernelOperationalGovernanceReport kernelOperationalGovernance;
    private InstitutionalPolicySnapshotReport institutionalPolicySnapshot;
    private KernelDecisionMetricsReport kernelDecisionMetrics;
    private KernelRiskEscalationReport kernelRiskEscalation;
    private KernelAdvisoryTelemetry advisoryTelemetry;

    @Builder.Default
    private List<String> strategicInsights = new ArrayList<>();
}
