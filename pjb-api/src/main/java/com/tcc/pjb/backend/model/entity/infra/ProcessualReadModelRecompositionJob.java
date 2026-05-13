package com.tcc.pjb.backend.model.entity.infra;

import com.tcc.pjb.backend.core.modularity.PjbModuleId;
import com.tcc.pjb.backend.core.ownership.PjbDataOwnership;
import com.tcc.pjb.backend.core.ownership.PjbOwnershipMode;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processual_read_model_recomp_job",
        indexes = {
                @Index(name = "idx_prm_recomp_status_not_before", columnList = "status,not_before_at"),
                @Index(name = "idx_prm_recomp_scope", columnList = "domain,tribunal_code,ramo_code,scope_key"),
                @Index(name = "idx_prm_recomp_updated", columnList = "updated_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ProcessualReadModelRecompositionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "domain", nullable = false, length = 120)
    private String domain;

    @Column(name = "tribunal_code", length = 80)
    private String tribunalCode;

    @Column(name = "ramo_code", length = 80)
    private String ramoCode;

    @Column(name = "scope_key", length = 240)
    private String scopeKey;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "not_before_at")
    private Instant notBeforeAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "last_claimed_at")
    private Instant lastClaimedAt;

    @Column(name = "last_completed_at")
    private Instant lastCompletedAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (status == null || status.isBlank()) {
            status = "PENDENTE";
        }
        if (attemptCount == null || attemptCount < 0) {
            attemptCount = 0;
        }
        if (notBeforeAt == null) {
            notBeforeAt = Instant.now();
        }
    }
}
