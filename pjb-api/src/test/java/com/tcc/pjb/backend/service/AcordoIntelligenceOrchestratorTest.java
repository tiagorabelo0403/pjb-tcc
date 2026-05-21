package com.tcc.pjb.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextService;
import com.tcc.pjb.backend.core.kernel.advisory.KernelAdvisoryTelemetry;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationWindowReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementIntelligenceService;
import com.tcc.pjb.backend.core.kernel.governance.InstitutionalPolicyResolver;
import com.tcc.pjb.backend.core.kernel.governance.KernelDecisionMetricsService;
import com.tcc.pjb.backend.core.kernel.governance.KernelRiskEscalationService;
import com.tcc.pjb.backend.core.kernel.governance.NegotiationReleaseGuard;
import com.tcc.pjb.backend.core.procedural.NationalProceduralRoutingService;
import com.tcc.pjb.backend.core.procedural.ProceduralConnectorExecutionService;
import com.tcc.pjb.backend.core.procedural.ProceduralSubmissionBlueprintService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import com.tcc.pjb.backend.service.intelligence.ProcessOutcomePredictionService;
import com.tcc.pjb.backend.service.intelligence.QualifiedThemeProactiveService;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AcordoIntelligenceOrchestratorTest {

    private final ProcessoRepository processoRepository = mock(ProcessoRepository.class);
    private final PropostaAcordoRepository propostaAcordoRepository = mock(PropostaAcordoRepository.class);
    private final ChatMensagemRepository chatMensagemRepository = mock(ChatMensagemRepository.class);
    private final ProcessoRitoSnapshotService processoRitoSnapshotService = mock(ProcessoRitoSnapshotService.class);
    private final SettlementIntelligenceService settlementIntelligenceService = mock(SettlementIntelligenceService.class);
    private final ProcessMaterialDossierService processMaterialDossierService = mock(ProcessMaterialDossierService.class);
    private final ProcessMaterialStrategyService processMaterialStrategyService = mock(ProcessMaterialStrategyService.class);
    private final SettlementAdvisoryService settlementAdvisoryService = mock(SettlementAdvisoryService.class);
    private final NationalProceduralRoutingService nationalProceduralRoutingService = mock(NationalProceduralRoutingService.class);
    private final ProceduralSubmissionBlueprintService proceduralSubmissionBlueprintService = mock(ProceduralSubmissionBlueprintService.class);
    private final ProceduralConnectorExecutionService proceduralConnectorExecutionService = mock(ProceduralConnectorExecutionService.class);
    private final InstitutionalGovernanceContextService institutionalGovernanceContextService = mock(InstitutionalGovernanceContextService.class);
    private final NegotiationMemoryService negotiationMemoryService = mock(NegotiationMemoryService.class);
    private final NegotiationExplainabilityService negotiationExplainabilityService = mock(NegotiationExplainabilityService.class);
    private final NegotiationChatDigestService negotiationChatDigestService = mock(NegotiationChatDigestService.class);
    private final NegotiationApprovalMatrixService negotiationApprovalMatrixService = mock(NegotiationApprovalMatrixService.class);
    private final NegotiationChannelGovernanceService negotiationChannelGovernanceService = mock(NegotiationChannelGovernanceService.class);
    private final KernelOperationalGovernanceService kernelOperationalGovernanceService = mock(KernelOperationalGovernanceService.class);
    private final InstitutionalPolicyResolver institutionalPolicyResolver = mock(InstitutionalPolicyResolver.class);
    private final KernelDecisionMetricsService kernelDecisionMetricsService = mock(KernelDecisionMetricsService.class);
    private final KernelRiskEscalationService kernelRiskEscalationService = mock(KernelRiskEscalationService.class);
    private final NegotiationReleaseGuard negotiationReleaseGuard = mock(NegotiationReleaseGuard.class);
    private final ProcessOutcomePredictionService outcomePredictionService = mock(ProcessOutcomePredictionService.class);
    private final QualifiedThemeProactiveService qualifiedThemeProactiveService = mock(QualifiedThemeProactiveService.class);
    private final JudgeAgreementApprovalService judgeAgreementApprovalService = mock(JudgeAgreementApprovalService.class);

    private AcordoIntelligenceOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new AcordoIntelligenceOrchestrator(
                processoRepository, propostaAcordoRepository, chatMensagemRepository,
                processoRitoSnapshotService, settlementIntelligenceService,
                processMaterialDossierService, processMaterialStrategyService,
                settlementAdvisoryService, nationalProceduralRoutingService,
                proceduralSubmissionBlueprintService, proceduralConnectorExecutionService,
                institutionalGovernanceContextService, negotiationMemoryService,
                negotiationExplainabilityService, negotiationChatDigestService,
                negotiationApprovalMatrixService, negotiationChannelGovernanceService,
                kernelOperationalGovernanceService, institutionalPolicyResolver,
                kernelDecisionMetricsService, kernelRiskEscalationService,
                negotiationReleaseGuard, outcomePredictionService,
                qualifiedThemeProactiveService, judgeAgreementApprovalService
        );
    }

    @Test
    void deveLancarRecursoNaoEncontradoQuandoProcessoInexistente() {
        when(processoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orchestrator.analisarJanelaNegocial(99L, BigDecimal.TEN))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void deveDelegarParaSettlementIntelligenceServiceQuandoProcessoEncontrado() {
        Processo processo = new Processo();
        processo.setId(1L);
        processo.setRamoDireito(RamoDireito.CIVIL);
        processo.setStatusProcesso(StatusProcesso.EM_ANDAMENTO);
        processo.setPedidoPrincipal("Cobrança contratual");
        when(processoRepository.findById(1L)).thenReturn(Optional.of(processo));
        when(processMaterialDossierService.analyzeProcess(any(), any())).thenReturn(null);
        when(processMaterialStrategyService.analyzeProcess(any(), any(), any())).thenReturn(null);
        NegotiationWindowReport report = mock(NegotiationWindowReport.class);
        when(settlementIntelligenceService.analyze(any(), any(), any())).thenReturn(report);

        NegotiationWindowReport resultado = orchestrator.analisarJanelaNegocial(1L, new BigDecimal("5000.00"));

        assertThat(resultado).isSameAs(report);
        verify(settlementIntelligenceService).analyze(eq(processo), eq(new BigDecimal("5000.00")), any());
    }
}
