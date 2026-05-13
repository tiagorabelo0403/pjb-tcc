package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "adv_office_process_operation",
        indexes = {
                @Index(name = "ix_adv_office_process_operation_processo_status", columnList = "processo_id, status, created_at"),
                @Index(name = "ix_adv_office_process_operation_executor_status", columnList = "executor_user_id, status, created_at"),
                @Index(name = "ix_adv_office_process_operation_signer_status", columnList = "signer_user_id, status, created_at")
        })
@Getter
@Setter
public class AdvOfficeProcessOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipe_id", foreignKey = @ForeignKey(name = "fk_adv_office_process_operation_equipe"))
    private Equipe equipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_operation_processo"))
    private Processo processo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "executor_user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_operation_executor"))
    private Usuario executor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_user_id", foreignKey = @ForeignKey(name = "fk_adv_office_process_operation_signer"))
    private Usuario signer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "queue_item_id", foreignKey = @ForeignKey(name = "fk_adv_office_process_operation_queue"))
    private OfficeSignatureQueueItem queueItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 60)
    private OfficeActionType actionType;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "result_payload_json", columnDefinition = "TEXT")
    private String resultPayloadJson;

    @Column(name = "signature_payload_json", columnDefinition = "TEXT")
    private String signaturePayloadJson;

    @Column(name = "signature_hash", length = 64)
    private String signatureHash;

    @Column(name = "signer_name_snapshot", length = 255)
    private String signerNameSnapshot;

    @Column(name = "signer_registration_snapshot", length = 120)
    private String signerRegistrationSnapshot;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", length = 240)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
