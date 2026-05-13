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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@PjbDataOwnership(module = PjbModuleId.AUDITORIA_OBSERVABILIDADE, mode = PjbOwnershipMode.PUBLISHED_VIEW, publishedReadModel = true)
@Entity
@Table(name = "tb_processual_read_model_trail",
        indexes = {
                @Index(name = "idx_prm_trail_domain_key_version", columnList = "projection_domain,projection_key,projection_version", unique = true),
                @Index(name = "idx_prm_trail_scope", columnList = "projection_domain,tribunal_code,ramo_code"),
                @Index(name = "idx_prm_trail_created", columnList = "created_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class ProcessualReadModelMaterializationTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "projection_domain", nullable = false, length = 120)
    private String projectionDomain;

    @Column(name = "projection_key", nullable = false, length = 240)
    private String projectionKey;

    @Column(name = "projection_version", nullable = false)
    private Long projectionVersion;

    @Column(name = "previous_version")
    private Long previousVersion;

    @Column(name = "event_type", length = 160)
    private String eventType;

    @Column(name = "aggregate_type", length = 120)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 160)
    private String aggregateId;

    @Column(name = "tribunal_code", length = 80)
    private String tribunalCode;

    @Column(name = "ramo_code", length = 80)
    private String ramoCode;

    @Column(name = "source", length = 80)
    private String source;

    @Column(name = "materialization_hash", length = 96)
    private String materializationHash;

    @Column(name = "materialization_status", nullable = false, length = 40)
    private String materializationStatus;

    @Column(name = "payload_snapshot_json", columnDefinition = "TEXT")
    private String payloadSnapshotJson;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (projectionVersion == null || projectionVersion <= 0L) {
            projectionVersion = 1L;
        }
        if (materializationStatus == null || materializationStatus.isBlank()) {
            materializationStatus = "MATERIALIZED";
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
