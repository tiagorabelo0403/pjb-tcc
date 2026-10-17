package com.tcc.pjb.backend.service;

import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;
import com.tcc.pjb.backend.model.dto.intelligence.JudgeAgreementApprovalPromptResponse;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.PropostaAcordo;
import com.tcc.pjb.backend.model.repository.ChatMensagemRepository;
import com.tcc.pjb.backend.service.intelligence.AgreementChatLedgerService;
import com.tcc.pjb.backend.service.intelligence.JudgeAgreementApprovalService;
import com.tcc.pjb.backend.service.intelligence.ProcessOutcomePredictionService;
import jakarta.inject.Inject;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
class AcordoSubmissaoJudicialService {

    private final JudgeAgreementApprovalService judgeAgreementApprovalService;
    private final ProcessOutcomePredictionService processOutcomePredictionService;
    private final ChatMensagemRepository chatMensagemRepository;
    private final AgreementChatLedgerService agreementChatLedgerService;

    @Inject
    AcordoSubmissaoJudicialService(JudgeAgreementApprovalService judgeAgreementApprovalService,
                                   ProcessOutcomePredictionService processOutcomePredictionService,
                                   ChatMensagemRepository chatMensagemRepository,
                                   AgreementChatLedgerService agreementChatLedgerService) {
        this.judgeAgreementApprovalService = Objects.requireNonNull(judgeAgreementApprovalService);
        this.processOutcomePredictionService = Objects.requireNonNull(processOutcomePredictionService);
        this.chatMensagemRepository = Objects.requireNonNull(chatMensagemRepository);
        this.agreementChatLedgerService = Objects.requireNonNull(agreementChatLedgerService);
    }

    JudgeAgreementApprovalPromptResponse solicitarAprovacao(Processo processo,
                                                            PropostaAcordo proposta,
                                                            SettlementAdvisoryReport settlementAdvisory,
                                                            String resumoExecutivo) {
        return judgeAgreementApprovalService.requestApproval(
                processo,
                proposta,
                settlementAdvisory,
                processOutcomePredictionService.analyze(processo),
                resumoExecutivo
        );
    }

    String mensagemDaRodadaDeSubmissao(Processo processo) {
        AgreementChatLedgerService.RoundSnapshot snapshot = agreementChatLedgerService.nextRoundSnapshot(chatMensagemRepository.findByProcesso_IdOrderByDataEnvioAsc(processo.getId()), "homologação judicial do acordo");
        return agreementChatLedgerService.renderRoundSystemMessage(new AgreementChatLedgerService.RoundSnapshot(snapshot.round(), snapshot.version(), "SUBMISSAO_JUDICIAL"));
    }
}
