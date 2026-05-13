package com.tcc.pjb.backend.modules.atendimento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AtendimentoMessageAttachmentId implements Serializable {

    @Column(name = "message_id", nullable = false)
    private Long messageId;

    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    public AtendimentoMessageAttachmentId() {
    }

    public AtendimentoMessageAttachmentId(Long messageId, Long attachmentId) {
        this.messageId = messageId;
        this.attachmentId = attachmentId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public Long getAttachmentId() {
        return attachmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtendimentoMessageAttachmentId that = (AtendimentoMessageAttachmentId) o;
        return Objects.equals(messageId, that.messageId) && Objects.equals(attachmentId, that.attachmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, attachmentId);
    }
}
