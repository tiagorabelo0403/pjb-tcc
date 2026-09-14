package com.tcc.pjb.backend.core.kernel.twin;

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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Extraída (F6) de ProcessDigitalTwinService: pipeline de análise de negociação de acordo.
 * Carrega a última proposta e o chat recente do processo, e encadeia 5 análises que se
 * alimentam mutuamente (memory -> explainability -> chatDigest -> approvalMatrix -> channelGovernance).
 * kernelOperationalGovernance vem de fora porque é usado também para telemetria no orquestrador
 * principal e não deve ser recalculado.
 */
@Service
public class ProcessTwinNegotiationOrchestrator {

    private final NegotiationMemoryService negotiationMemoryService;
    private final NegotiationExplainabilityService negotiationExplainabilityService;
    private final NegotiationChatDigestService negotiationChatDigestService;
    private final NegotiationApprovalMatrixService negotiationApprovalMatrixService;
    private final NegotiationChannelGovernanceService negotiationChannelGovernanceService;
    private final ChatMensagemRepository chatMensagemRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;

    public ProcessTwinNegotiationOrchestrator(NegotiationMemoryService negotiationMemoryService,
                                               NegotiationExplainabilityService negotiationExplainabilityService,
                                               NegotiationChatDigestService negotiationChatDigestService,
                                               NegotiationApprovalMatrixService negotiationApprovalMatrixService,
                                               NegotiationChannelGovernanceService negotiationChannelGovernanceService,
                                               ChatMensagemRepository chatMensagemRepository,
                                               PropostaAcordoRepository propostaAcordoRepository) {
        this.negotiationMemoryService = Objects.requireNonNull(negotiationMemoryService);
        this.negotiationExplainabilityService = Objects.requireNonNull(negotiationExplainabilityService);
        this.negotiationChatDigestService = Objects.requireNonNull(negotiationChatDigestService);
        this.negotiationApprovalMatrixService = Objects.requireNonNull(negotiationApprovalMatrixService);
        this.negotiationChannelGovernanceService = Objects.requireNonNull(negotiationChannelGovernanceService);
        this.chatMensagemRepository = Objects.requireNonNull(chatMensagemRepository);
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository);
    }

    /**
     * Passo 1 do pipeline de negociação: carrega proposta/chat e as 2 análises que precedem
     * a decisão de KernelOperationalGovernance (que consome memory + explainability). Chamado
     * antes de o twin() calcular kernelOperationalGovernance.
     */
    public PreBundle prepareContext(Processo processo,
                                     SettlementAdvisoryReport settlementAdvisory,
                                     InstitutionalGovernanceContextReport institutionalGovernanceContext) {
        Long processoId = processo.getId();
        Optional<PropostaAcordo> latestProposalOpt = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId);
        PropostaAcordo latestProposal = latestProposalOpt.orElse(null);
        List<ChatMensagem> recentChat = recentChat(processoId);
        NegotiationMemoryReport memory = negotiationMemoryService.analyzeProcess(processo, latestProposal, recentChat, settlementAdvisory, institutionalGovernanceContext);
        NegotiationExplainabilityReport explainability = negotiationExplainabilityService.compose(processo, latestProposal, recentChat, settlementAdvisory, memory, institutionalGovernanceContext);
        return new PreBundle(latestProposal, recentChat, memory, explainability);
    }

    /**
     * Passo 2 do pipeline de negociação: encadeia as 3 análises que dependem de
     * kernelOperationalGovernance (chatDigest -> approvalMatrix -> channelGovernance).
     */
    public Bundle finalizeAnalysis(Processo processo,
                                    PreBundle pre,
                                    SettlementAdvisoryReport settlementAdvisory,
                                    InstitutionalGovernanceContextReport institutionalGovernanceContext,
                                    KernelOperationalGovernanceReport kernelOperationalGovernance) {
        NegotiationChatDigestReport chatDigest = negotiationChatDigestService.analyzeProcess(processo, pre.latestProposal(), pre.recentChat(), settlementAdvisory, pre.memory(), pre.explainability(), institutionalGovernanceContext, kernelOperationalGovernance);
        NegotiationApprovalMatrixReport approvalMatrix = negotiationApprovalMatrixService.analyzeProcess(processo, pre.latestProposal(), pre.recentChat(), institutionalGovernanceContext, kernelOperationalGovernance, pre.memory(), pre.explainability(), chatDigest);
        NegotiationChannelGovernanceReport channelGovernance = negotiationChannelGovernanceService.analyzeProcess(processo, pre.latestProposal(), pre.recentChat(), institutionalGovernanceContext, kernelOperationalGovernance, pre.memory(), pre.explainability(), chatDigest, approvalMatrix);
        return new Bundle(pre.latestProposal(), pre.recentChat(), pre.memory(), pre.explainability(), chatDigest, approvalMatrix, channelGovernance);
    }

    private List<ChatMensagem> recentChat(Long processoId) {
        return chatMensagemRepository.findTop80ByProcesso_IdOrderByDataEnvioDesc(processoId).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatMensagem::getDataEnvio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public record PreBundle(
            PropostaAcordo latestProposal,
            List<ChatMensagem> recentChat,
            NegotiationMemoryReport memory,
            NegotiationExplainabilityReport explainability
    ) {
    }

    public record Bundle(
            PropostaAcordo latestProposal,
            List<ChatMensagem> recentChat,
            NegotiationMemoryReport memory,
            NegotiationExplainabilityReport explainability,
            NegotiationChatDigestReport chatDigest,
            NegotiationApprovalMatrixReport approvalMatrix,
            NegotiationChannelGovernanceReport channelGovernance
    ) {
    }
}
