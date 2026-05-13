package com.tcc.pjb.backend.modules.atendimento.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_atendimento_message_attachment", indexes = {
        @Index(name = "idx_att_msg_att_msg", columnList = "message_id"),
        @Index(name = "idx_att_msg_att_att", columnList = "attachment_id")
})
public class AtendimentoMessageAttachment {

    @EmbeddedId
    private AtendimentoMessageAttachmentId id;

    @Column(name = "thread_id", nullable = false)
    private Long threadId;

    public AtendimentoMessageAttachmentId getId() {
        return id;
    }

    public void setId(AtendimentoMessageAttachmentId id) {
        this.id = id;
    }

    public Long getThreadId() {
        return threadId;
    }

    public void setThreadId(Long threadId) {
        this.threadId = threadId;
    }
}
