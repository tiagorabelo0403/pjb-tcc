package com.tcc.pjb.backend.modules.advocacia.office.entity;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeQueueStatus;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "adv_office_signature_queue",
        indexes = {
                @Index(name = "ix_office_queue_signer_status", columnList = "signer_user_id, status, created_at"),
                @Index(name = "ix_office_queue_equipe_status", columnList = "equipe_id, status, created_at"),
                @Index(name = "ix_office_queue_executor", columnList = "executor_user_id, created_at")
        }
)
@Getter
@Setter
public class OfficeSignatureQueueItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_queue_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executor_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_queue_executor"))
    private Usuario executor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signer_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_queue_signer"))
    private Usuario signer;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private OfficeActionType actionType;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 80)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OfficeQueueStatus status = OfficeQueueStatus.PENDING;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @Column(name = "summary", length = 240)
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by_user_id", foreignKey = @ForeignKey(name = "fk_office_queue_decided_by"))
    private Usuario decidedBy;

    @Column(name = "decision_reason", length = 240)
    private String decisionReason;
}
