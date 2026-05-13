package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.i18n.PjbStaticMessageCatalog;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoAttachmentDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageDto;
import com.tcc.pjb.backend.modules.atendimento.dto.AtendimentoMessageReplyPreviewDto;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoAttachment;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessage;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageReceipt;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoMessageStatus;
import com.tcc.pjb.backend.modules.atendimento.util.AtendimentoParticipantLabelUtils;
import java.util.List;

final class AtendimentoChatMessageMapper {

    private AtendimentoChatMessageMapper() {
    }

    static boolean isHiddenForViewer(AtendimentoMessage message, Long viewerUserId) {
        if (message == null) {
            return true;
        }
        AtendimentoMessageStatus status = message.getStatus();
        if (status != AtendimentoMessageStatus.QUARANTINED && status != AtendimentoMessageStatus.BLOCKED) {
            return false;
        }
        if (viewerUserId == null) {
            return true;
        }
        return !viewerUserId.equals(message.getSenderUsuarioId());
    }

    static String visibleBodyForViewer(AtendimentoMessage message, Long viewerUserId) {
        return isHiddenForViewer(message, viewerUserId) ? null : message.getBody();
    }

    static String senderDisplayName(AtendimentoMessage message, Usuario sender) {
        if (sender != null) {
            return AtendimentoParticipantLabelUtils.displayName(sender);
        }
        return isSystemSender(message) ? PjbStaticMessageCatalog.text("pjb.atendimento.system.sender.name") : null;
    }

    static String senderLabel(AtendimentoMessage message, Usuario sender) {
        if (sender != null) {
            return AtendimentoParticipantLabelUtils.participantLabel(sender);
        }
        return isSystemSender(message) ? PjbStaticMessageCatalog.text("pjb.atendimento.system.sender.label") : null;
    }

    static String senderOab(AtendimentoMessage message, Usuario sender) {
        if (sender != null) {
            return AtendimentoParticipantLabelUtils.oabLabel(sender);
        }
        return null;
    }

    static AtendimentoMessageReplyPreviewDto toReplyPreview(AtendimentoMessage target, Long viewerUserId, String senderDisplayName, String senderLabel) {
        if (target == null) {
            return null;
        }
        String body = visibleBodyForViewer(target, viewerUserId);
        String preview = null;
        if (body != null) {
            String text = body.replace("\n", " ").replace("\r", " ").trim();
            if (text.length() > 160) {
                text = text.substring(0, 160);
            }
            preview = text.isBlank() ? null : text;
        }
        return new AtendimentoMessageReplyPreviewDto(
                target.getId(),
                target.getSenderUsuarioId(),
                target.getSenderTipo(),
                preview,
                target.getCreatedAt(),
                senderDisplayName,
                senderLabel
        );
    }

    static AtendimentoMessageDto toDto(AtendimentoMessage message,
                                       List<AtendimentoAttachment> attachments,
                                       Long viewerUserId,
                                       AtendimentoMessageReplyPreviewDto replyTo,
                                       Long otherLastDelivered,
                                       Long otherLastRead,
                                       AtendimentoMessageReceipt otherReceipt,
                                       String senderDisplayName,
                                       String senderLabel,
                                       String senderOab) {
        String status = message.getStatus() != null ? message.getStatus().name() : AtendimentoMessageStatus.DELIVERED.name();
        boolean hide = isHiddenForViewer(message, viewerUserId);
        String body = hide ? null : message.getBody();
        List<AtendimentoAttachmentDto> attachmentDtos = hide
                ? List.of()
                : attachments == null ? List.of() : attachments.stream().map(AtendimentoChatMessageMapper::toAttachmentDto).toList();
        boolean viewerIsSender = viewerUserId != null && viewerUserId.equals(message.getSenderUsuarioId());
        boolean deliveredToOther = viewerIsSender && otherLastDelivered != null && message.getId() != null && otherLastDelivered.longValue() >= message.getId().longValue();
        boolean readByOther = viewerIsSender && otherLastRead != null && message.getId() != null && otherLastRead.longValue() >= message.getId().longValue();
        return new AtendimentoMessageDto(
                message.getId(),
                message.getThreadId(),
                message.getSenderUsuarioId(),
                message.getSenderTipo(),
                status,
                body,
                message.getCreatedAt(),
                attachmentDtos,
                message.getReplyToMessageId(),
                replyTo,
                deliveredToOther,
                readByOther,
                otherReceipt != null ? otherReceipt.getDeliveredAt() : null,
                otherReceipt != null ? otherReceipt.getReadAt() : null,
                senderDisplayName,
                senderLabel,
                senderOab
        );
    }

    static AtendimentoAttachmentDto toAttachmentDto(AtendimentoAttachment attachment) {
        return new AtendimentoAttachmentDto(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getStatus() != null ? attachment.getStatus().name() : null
        );
    }

    private static boolean isSystemSender(AtendimentoMessage message) {
        return message != null && "PJB_SISTEMA".equals(message.getSenderTipo());
    }
}
