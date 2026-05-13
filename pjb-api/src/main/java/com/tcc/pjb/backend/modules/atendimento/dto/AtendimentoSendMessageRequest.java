package com.tcc.pjb.backend.modules.atendimento.dto;

import java.util.List;
import java.util.UUID;

public record AtendimentoSendMessageRequest(
    String body,
    List<Long> attachmentIds,
    



    Long replyToMessageId,
    



    UUID clientMessageId
) {
}
