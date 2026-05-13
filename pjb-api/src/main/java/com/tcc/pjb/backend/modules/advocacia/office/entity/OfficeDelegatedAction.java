package com.tcc.pjb.backend.modules.advocacia.office.entity;

import java.time.LocalDateTime;
import java.util.UUID;
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
import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeDelegationMode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
        name = "adv_office_delegated_action",
        indexes = {
                @Index(name = "ix_office_action_equipe_created", columnList = "equipe_id, created_at"),
                @Index(name = "ix_office_action_signer_created", columnList = "signer_user_id, created_at"),
                @Index(name = "ix_office_action_executor_created", columnList = "executor_user_id, created_at")
        }
)
@Getter
@Setter
public class OfficeDelegatedAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_action_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executor_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_action_executor"))
    private Usuario executor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signer_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_office_action_signer"))
    private Usuario signer;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private OfficeDelegationMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private OfficeActionType actionType;

    @Column(name = "resource_type", nullable = false, length = 80)
    private String resourceType;

    @Column(name = "resource_id", nullable = false, length = 80)
    private String resourceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_item_id", foreignKey = @ForeignKey(name = "fk_office_action_queue_item"))
    private OfficeSignatureQueueItem queueItem;

    @Column(name = "job_id")
    private UUID jobId;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "payload_hash", length = 64)
    private String payloadHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
