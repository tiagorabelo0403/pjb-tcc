package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialDossierService;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyReport;
import com.tcc.pjb.backend.core.kernel.advisory.ProcessMaterialStrategyService;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryService;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.model.dto.intelligence.AgreementChatContextResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.entity.ChatMensagem;
import com.tcc.pjb.backend.model.entity.enums.StatusAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import com.tcc.pjb.backend.service.rito.ProcessoRitoSnapshotService;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgreementChatContextService {

    private final ProcessoRepository processoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final ChatMensagemRepository chatMensagemRepository;
    private final PjbAuthorizationService authorizationService;
    private final ProcessOutcomePredictionService outcomePredictionService;
    private final QualifiedThemeProactiveService qualifiedThemeProactiveService;
    private final StructuredProcessSummaryService structuredProcessSummaryService;
    private final ProcessFraudRiskService processFraudRiskService;
    private final ExecutionRecoveryRiskService executionRecoveryRiskService;
    private final JudgeAgreementApprovalService judgeAgreementApprovalService;
    private final ProcessoRitoSnapshotService processoRitoSnapshotService;
    private final ProcessMaterialDossierService processMaterialDossierService;
    private final ProcessMaterialStrategyService processMaterialStrategyService;
    private final SettlementAdvisoryService settlementAdvisoryService;
    private final AgreementChatGovernanceService agreementChatGovernanceService;
    private final AgreementChatLedgerService agreementChatLedgerService;

    public AgreementChatContextService(ProcessoRepository processoRepository,
                                       PropostaAcordoRepository propostaAcordoRepository,
                                       ChatMensagemRepository chatMensagemRepository,
                                       PjbAuthorizationService authorizationService,
                                       ProcessOutcomePredictionService outcomePredictionService,
                                       QualifiedThemeProactiveService qualifiedThemeProactiveService,
                                       StructuredProcessSummaryService structuredProcessSummaryService,
                                       ProcessFraudRiskService processFraudRiskService,
                                       ExecutionRecoveryRiskService executionRecoveryRiskService,
                                       JudgeAgreementApprovalService judgeAgreementApprovalService,
                                       ProcessoRitoSnapshotService processoRitoSnapshotService,
                                       ProcessMaterialDossierService processMaterialDossierService,
                                       ProcessMaterialStrategyService processMaterialStrategyService,
                                       SettlementAdvisoryService settlementAdvisoryService,
                                       AgreementChatGovernanceService agreementChatGovernanceService,
                                       AgreementChatLedgerService agreementChatLedgerService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository);
        this.chatMensagemRepository = Objects.requireNonNull(chatMensagemRepository);
        this.authorizationService = Objects.requireNonNull(authorizationService);
        this.outcomePredictionService = Objects.requireNonNull(outcomePredictionService);
        this.qualifiedThemeProactiveService = Objects.requireNonNull(qualifiedThemeProactiveService);
        this.structuredProcessSummaryService = Objects.requireNonNull(structuredProcessSummaryService);
        this.processFraudRiskService = Objects.requireNonNull(processFraudRiskService);
        this.executionRecoveryRiskService = Objects.requireNonNull(executionRecoveryRiskService);
        this.judgeAgreementApprovalService = Objects.requireNonNull(judgeAgreementApprovalService);
        this.processoRitoSnapshotService = Objects.requireNonNull(processoRitoSnapshotService);
        this.processMaterialDossierService = Objects.requireNonNull(processMaterialDossierService);
        this.processMaterialStrategyService = Objects.requireNonNull(processMaterialStrategyService);
        this.settlementAdvisoryService = Objects.requireNonNull(settlementAdvisoryService);
        this.agreementChatGovernanceService = Objects.requireNonNull(agreementChatGovernanceService);
        this.agreementChatLedgerService = Objects.requireNonNull(agreementChatLedgerService);
    }

    @Transactional(readOnly = true)
    public AgreementChatContextResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        authorizationService.requireReadProcesso(processo);
        PropostaAcordo proposta = propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId).orElse(null);
        var outcome = outcomePredictionService.analyze(processo);
        var themes = qualifiedThemeProactiveService.analyze(processo);
        var summary = structuredProcessSummaryService.summarize(processo);
        var fraud = processFraudRiskService.analyze(processo);
        var execution = executionRecoveryRiskService.analyze(processo);
        SettlementAdvisoryReport advisory = buildSettlementAdvisory(processo, proposta);
        var approval = judgeAgreementApprovalService.preview(processo, proposta, advisory, outcome);
        var channelPolicy = agreementChatGovernanceService.analyze(processo, proposta);
        List<ChatMensagem> history = chatMensagemRepository.findByProcesso_IdOrderByDataEnvioAsc(processoId);
        var rounds = agreementChatLedgerService.buildRoundTimeline(history);
        var attachments = agreementChatLedgerService.buildStructuredAttachments(history);
        LinkedHashSet<String> guardrails = new LinkedHashSet<>();
        guardrails.addAll(safeList(advisory.executionSafeguards()));
        guardrails.addAll(safeList(approval.safeguards()));
        if (fraud.exigeRevisaoHumana()) {
            guardrails.add("Submeter a negociação à revisão humana antes de consolidar a minuta final.");
        }
        LinkedHashSet<String> suggestedPrompts = new LinkedHashSet<>();
        suggestedPrompts.addAll(safeList(outcome.conciliacaoPrompts()));
        if (execution.recoveryProbability() < 0.45d) {
            suggestedPrompts.add("Sugerir acordo com garantias reais, cronograma escalonado e gatilho de vencimento antecipado.");
        }
        if (themes.autoStaySuggested() || themes.applicationSuggested()) {
            suggestedPrompts.add("Antes de avançar na redação final, verificar impacto de tema repetitivo ou repercussão geral aplicável.");
        }
        if (fraud.litiganciaMassificada() || fraud.enderecoSuspeito() || !fraud.cpfValido()) {
            suggestedPrompts.add("Confirmar identidade, representação e dados cadastrais antes da aceitação definitiva do acordo.");
        }
        if (suggestedPrompts.isEmpty()) {
            suggestedPrompts.add("Conduzir a negociação com foco em cláusulas executáveis, calendário objetivo e multa por inadimplemento.");
        }
        LinkedHashSet<String> nextActions = new LinkedHashSet<>();
        nextActions.addAll(safeList(advisory.nextMoves()));
        nextActions.addAll(safeList(summary.proximosAtos()));
        if (proposta != null && proposta.getStatus() == StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ) {
            nextActions.add("Aguardar decisão do gabinete sobre homologar, devolver para revisão ou rejeitar.");
        }
        if (nextActions.isEmpty()) {
            nextActions.add("Manter o canal negocial ativo até consolidar a minuta ou encerrar a rodada sem acordo.");
        }
        return new AgreementChatContextResponse(
                processoId,
                proposta != null ? proposta.getId() : null,
                proposta != null && proposta.getStatus() != null ? proposta.getStatus().name() : "SEM_PROPOSTA_ATIVA",
                proposta != null && proposta.getStatus() == StatusAcordo.AGUARDANDO_HOMOLOGACAO_JUIZ,
                true,
                outcome,
                themes,
                summary,
                fraud,
                execution,
                approval,
                List.copyOf(guardrails),
                List.copyOf(suggestedPrompts),
                List.copyOf(nextActions),
                rounds,
                attachments,
                channelPolicy.allowedSpeakerBands(),
                channelPolicy.stage(),
                channelPolicy.judgeDecisionOpen()
        );
    }

    private SettlementAdvisoryReport buildSettlementAdvisory(Processo processo, PropostaAcordo proposta) {
        List<String> baseSignals = buildNegotiationSignals(processo);
        ProcessMaterialDossierReport dossier = processMaterialDossierService.analyzeProcess(processo, baseSignals);
        ProcessMaterialStrategyReport strategy = processMaterialStrategyService.analyzeProcess(processo, dossier, baseSignals);
        ArrayList<String> mergedSignals = new ArrayList<>(baseSignals);
        mergedSignals.addAll(safeList(dossier.settlementLevers()));
        mergedSignals.addAll(safeList(strategy.negotiationGuardrails()));
        return settlementAdvisoryService.analyze(
                processo,
                processoRitoSnapshotService.resolve(processo, null).ritoCode(),
                proposta != null ? proposta.getValorAcordo() : null,
                List.copyOf(mergedSignals.stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).distinct().toList()),
                null
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<String> buildNegotiationSignals(Processo processo) {
        ArrayList<String> signals = new ArrayList<>();
        if (processo.getFaseAtual() != null) {
            signals.add("Fase atual: " + processo.getFaseAtual().name());
        }
        if (processo.getStatusProcesso() != null) {
            signals.add("Status do processo: " + processo.getStatusProcesso().name());
        }
        if (processo.getResultadoFinal() != null && !processo.getResultadoFinal().isBlank()) {
            signals.add("Resultado atual: " + processo.getResultadoFinal().trim());
        }
        if (processo.getAssunto() != null && !processo.getAssunto().isBlank()) {
            signals.add("Assunto: " + processo.getAssunto().trim());
        }
        return List.copyOf(signals);
    }
}
