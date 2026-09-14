package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.moderation.ContentBlockedException;
import com.tcc.pjb.backend.core.moderation.TextModerationService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoThread;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoMessageAttachmentRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

/**
 * Passos exclusivos de {@code sendMessage()} que não são o núcleo de persistência da mensagem
 * em si: moderação de conteúdo (com registro de tentativa bloqueada) e vínculo de anexos já
 * validados à mensagem. Extraído de {@link AtendimentoChatService} porque
 * {@code moderationService}, {@code moderationEventService} e {@code messageAttachmentRepository}
 * são usados exclusivamente por esse método.
 */
@Service
public class AtendimentoOutboundMessageGuardService {

    private final TextModerationService moderationService;
    private final AtendimentoModerationEventService moderationEventService;
    private final AtendimentoMessageAttachmentRepository messageAttachmentRepository;

    public AtendimentoOutboundMessageGuardService(TextModerationService moderationService,
                                                   AtendimentoModerationEventService moderationEventService,
                                                   AtendimentoMessageAttachmentRepository messageAttachmentRepository) {
        this.moderationService = Objects.requireNonNull(moderationService);
        this.moderationEventService = Objects.requireNonNull(moderationEventService);
        this.messageAttachmentRepository = Objects.requireNonNull(messageAttachmentRepository);
    }

    public String validateOrRecordBlocked(Usuario actor, AtendimentoThread thread, String body) {
        try {
            return moderationService.validateMessage(body);
        } catch (ContentBlockedException exception) {
            moderationEventService.recordBlockedAttempt(actor, thread, exception.reason(), body);
            throw exception;
        }
    }

    public void linkAttachments(List<AtendimentoMessageAttachment> links) {
        messageAttachmentRepository.saveAll(links);
    }
}
