package com.tcc.pjb.backend.core.kernel.twin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityService;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessTwinNegotiationOrchestratorTest {

    private final NegotiationMemoryService memoryService = mock(NegotiationMemoryService.class);
    private final NegotiationExplainabilityService explainabilityService = mock(NegotiationExplainabilityService.class);
    private final NegotiationChatDigestService chatDigestService = mock(NegotiationChatDigestService.class);
    private final NegotiationApprovalMatrixService approvalMatrixService = mock(NegotiationApprovalMatrixService.class);
    private final NegotiationChannelGovernanceService channelGovernanceService = mock(NegotiationChannelGovernanceService.class);
    private final ChatMensagemRepository chatMensagemRepository = mock(ChatMensagemRepository.class);
    private final PropostaAcordoRepository propostaAcordoRepository = mock(PropostaAcordoRepository.class);
    private final ProcessTwinNegotiationOrchestrator orchestrator = new ProcessTwinNegotiationOrchestrator(
            memoryService, explainabilityService, chatDigestService, approvalMatrixService, channelGovernanceService,
            chatMensagemRepository, propostaAcordoRepository);

    @Test
    void prepareContextCarregaPropostaEChatEExecutaMemoryEExplainabilityNessaOrdem() {
        Processo processo = Processo.builder().id(7L).build();
        var settlementAdvisory = mock(SettlementAdvisoryReport.class);
        var institutionalGovernanceContext = mock(InstitutionalGovernanceContextReport.class);
        var proposal = mock(PropostaAcordo.class);
        ChatMensagem msg1 = chatWith(LocalDateTime.parse("2026-09-01T09:00:00"));
        ChatMensagem msg2 = chatWith(LocalDateTime.parse("2026-09-01T08:00:00"));
        when(propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(7L)).thenReturn(Optional.of(proposal));
        when(chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(7L)).thenReturn(List.of(msg1, msg2));
        var memory = mock(NegotiationMemoryReport.class);
        var explainability = mock(NegotiationExplainabilityReport.class);
        // recentChat é reordenado por dataEnvio asc dentro do orquestrador
        when(memoryService.analyzeProcess(processo, proposal, List.of(msg2, msg1), settlementAdvisory, institutionalGovernanceContext)).thenReturn(memory);
        when(explainabilityService.compose(processo, proposal, List.of(msg2, msg1), settlementAdvisory, memory, institutionalGovernanceContext)).thenReturn(explainability);

        var pre = orchestrator.prepareContext(processo, settlementAdvisory, institutionalGovernanceContext);

        assertThat(pre.latestProposal()).isSameAs(proposal);
        assertThat(pre.recentChat()).containsExactly(msg2, msg1);
        assertThat(pre.memory()).isSameAs(memory);
        assertThat(pre.explainability()).isSameAs(explainability);
    }

    @Test
    void prepareContextRetornaPropostaNullQuandoNaoExiste() {
        Processo processo = Processo.builder().id(8L).build();
        var settlementAdvisory = mock(SettlementAdvisoryReport.class);
        var institutionalGovernanceContext = mock(InstitutionalGovernanceContextReport.class);
        when(propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(8L)).thenReturn(Optional.empty());
        when(chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(8L)).thenReturn(List.of());
        when(memoryService.analyzeProcess(processo, null, List.of(), settlementAdvisory, institutionalGovernanceContext)).thenReturn(mock(NegotiationMemoryReport.class));
        when(explainabilityService.compose(processo, null, List.of(), settlementAdvisory, null, institutionalGovernanceContext)).thenReturn(mock(NegotiationExplainabilityReport.class));

        var pre = orchestrator.prepareContext(processo, settlementAdvisory, institutionalGovernanceContext);

        assertThat(pre.latestProposal()).isNull();
        assertThat(pre.recentChat()).isEmpty();
    }

    @Test
    void finalizeAnalysisEncadeiaChatDigestApprovalMatrixEChannelGovernanceComKernelOperational() {
        Processo processo = Processo.builder().id(9L).build();
        var settlementAdvisory = mock(SettlementAdvisoryReport.class);
        var institutionalGovernanceContext = mock(InstitutionalGovernanceContextReport.class);
        var kernelOp = mock(KernelOperationalGovernanceReport.class);
        var proposal = mock(PropostaAcordo.class);
        List<ChatMensagem> chat = List.of();
        var memory = mock(NegotiationMemoryReport.class);
        var explainability = mock(NegotiationExplainabilityReport.class);
        var pre = new ProcessTwinNegotiationOrchestrator.PreBundle(proposal, chat, memory, explainability);
        var chatDigest = mock(NegotiationChatDigestReport.class);
        var approvalMatrix = mock(NegotiationApprovalMatrixReport.class);
        var channelGovernance = mock(NegotiationChannelGovernanceReport.class);
        when(chatDigestService.analyzeProcess(processo, proposal, chat, settlementAdvisory, memory, explainability, institutionalGovernanceContext, kernelOp)).thenReturn(chatDigest);
        when(approvalMatrixService.analyzeProcess(processo, proposal, chat, institutionalGovernanceContext, kernelOp, memory, explainability, chatDigest)).thenReturn(approvalMatrix);
        when(channelGovernanceService.analyzeProcess(processo, proposal, chat, institutionalGovernanceContext, kernelOp, memory, explainability, chatDigest, approvalMatrix)).thenReturn(channelGovernance);

        var bundle = orchestrator.finalizeAnalysis(processo, pre, settlementAdvisory, institutionalGovernanceContext, kernelOp);

        assertThat(bundle.latestProposal()).isSameAs(proposal);
        assertThat(bundle.memory()).isSameAs(memory);
        assertThat(bundle.explainability()).isSameAs(explainability);
        assertThat(bundle.chatDigest()).isSameAs(chatDigest);
        assertThat(bundle.approvalMatrix()).isSameAs(approvalMatrix);
        assertThat(bundle.channelGovernance()).isSameAs(channelGovernance);
        verify(chatDigestService).analyzeProcess(processo, proposal, chat, settlementAdvisory, memory, explainability, institutionalGovernanceContext, kernelOp);
        verify(approvalMatrixService).analyzeProcess(processo, proposal, chat, institutionalGovernanceContext, kernelOp, memory, explainability, chatDigest);
        verify(channelGovernanceService).analyzeProcess(processo, proposal, chat, institutionalGovernanceContext, kernelOp, memory, explainability, chatDigest, approvalMatrix);
    }

    private ChatMensagem chatWith(LocalDateTime instant) {
        ChatMensagem m = mock(ChatMensagem.class);
        when(m.getDataEnvio()).thenReturn(instant);
        return m;
    }
}
