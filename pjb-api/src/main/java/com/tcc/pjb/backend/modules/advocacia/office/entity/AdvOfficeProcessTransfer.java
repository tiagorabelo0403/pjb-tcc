package com.tcc.pjb.backend.modules.advocacia.office.entity;

import com.tcc.pjb.backend.model.entity.Equipe;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeProcessTransferStatus;
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
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "adv_office_process_transfer", indexes = {
        @Index(name = "ix_adv_office_process_transfer_source", columnList = "source_equipe_id"),
        @Index(name = "ix_adv_office_process_transfer_target", columnList = "target_equipe_id"),
        @Index(name = "ix_adv_office_process_transfer_status", columnList = "status"),
        @Index(name = "ix_adv_office_process_transfer_target_user", columnList = "target_responsible_user_id")
})
@Getter
@Setter
public class AdvOfficeProcessTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_transfer_source"))
    private Equipe sourceEquipe;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_equipe_id", nullable = false, foreignKey = @ForeignKey(name = "fk_adv_office_process_transfer_target"))
    private Equipe targetEquipe;

    @Column(name = "initiated_by_user_id", nullable = false)
    private Long initiatedByUserId;

    @Column(name = "source_responsible_user_id")
    private Long sourceResponsibleUserId;

    @Column(name = "target_responsible_user_id", nullable = false)
    private Long targetResponsibleUserId;

    @Column(name = "process_count", nullable = false)
    private int processCount;

    @Column(name = "sensitive_process_count", nullable = false)
    private int sensitiveProcessCount;

    @Column(name = "motivo", length = 2000)
    private String motivo;

    @Column(name = "escopo", length = 500)
    private String escopo;

    @Column(name = "impact_hash", length = 128)
    private String impactHash;

    @Column(name = "preview_summary", length = 4000)
    private String previewSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private OfficeProcessTransferStatus status = OfficeProcessTransferStatus.PENDING_DESTINATION_ACCEPTANCE;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "response_by_user_id")
    private Long responseByUserId;

    @Column(name = "executed_at")
    private Instant executedAt;
}
