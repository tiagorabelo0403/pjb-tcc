package com.tcc.pjb.backend.service.intelligence;

import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalPromptResponse;
import com.tcc.pjb.backend.model.dto.intelligence.ProcessAiDossierResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.PropostaAcordoRepository;
import com.tcc.pjb.backend.service.exception.RecursoNaoEncontradoException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcessAiDossierService {

    private final ProcessoRepository processoRepository;
    private final PropostaAcordoRepository propostaAcordoRepository;
    private final ProcessOutcomePredictionService outcomePredictionService;
    private final QualifiedThemeProactiveService qualifiedThemeProactiveService;
    private final StructuredProcessSummaryService structuredProcessSummaryService;
    private final ProcessFraudRiskService processFraudRiskService;
    private final ExecutionRecoveryRiskService executionRecoveryRiskService;
    private final JudgeAgreementApprovalService judgeAgreementApprovalService;

    public ProcessAiDossierService(ProcessoRepository processoRepository,
                                   PropostaAcordoRepository propostaAcordoRepository,
                                   ProcessOutcomePredictionService outcomePredictionService,
                                   QualifiedThemeProactiveService qualifiedThemeProactiveService,
                                   StructuredProcessSummaryService structuredProcessSummaryService,
                                   ProcessFraudRiskService processFraudRiskService,
                                   ExecutionRecoveryRiskService executionRecoveryRiskService,
                                   JudgeAgreementApprovalService judgeAgreementApprovalService) {
        this.processoRepository = Objects.requireNonNull(processoRepository);
        this.propostaAcordoRepository = Objects.requireNonNull(propostaAcordoRepository);
        this.outcomePredictionService = Objects.requireNonNull(outcomePredictionService);
        this.qualifiedThemeProactiveService = Objects.requireNonNull(qualifiedThemeProactiveService);
        this.structuredProcessSummaryService = Objects.requireNonNull(structuredProcessSummaryService);
        this.processFraudRiskService = Objects.requireNonNull(processFraudRiskService);
        this.executionRecoveryRiskService = Objects.requireNonNull(executionRecoveryRiskService);
        this.judgeAgreementApprovalService = Objects.requireNonNull(judgeAgreementApprovalService);
    }

    @Transactional
    public ProcessAiDossierResponse analyze(Long processoId) {
        Processo processo = processoRepository.findProcessoCompletoById(processoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Processo", processoId));
        var outcome = outcomePredictionService.analyze(processo);
        var themes = qualifiedThemeProactiveService.analyze(processo);
        var summary = structuredProcessSummaryService.summarize(processo);
        var fraud = processFraudRiskService.analyze(processo);
        var execution = executionRecoveryRiskService.analyze(processo);
        JudgeAgreementApprovalPromptResponse approval = judgeAgreementApprovalService.preview(
                processo,
                propostaAcordoRepository.findTopByProcesso_IdOrderByDataAtualizacaoDesc(processoId).orElse(null),
                null,
                outcome
        );
        return new ProcessAiDossierResponse(processoId, outcome, themes, summary, fraud, execution, approval);
    }
}
